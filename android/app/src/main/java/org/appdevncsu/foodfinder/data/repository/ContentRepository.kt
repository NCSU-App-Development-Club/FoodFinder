package org.appdevncsu.foodfinder.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.HoursList
import org.appdevncsu.foodfinder.data.HoursRange
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.LocationList
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.data.SectionList
import org.appdevncsu.foodfinder.data.currentStatus
import org.appdevncsu.foodfinder.data.local.CachedPayload
import org.appdevncsu.foodfinder.data.local.PayloadDao
import org.appdevncsu.foodfinder.data.local.PayloadKeys
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.ncsuZone
import org.appdevncsu.foodfinder.di.ApplicationScope
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the home screen needs, decoded from the cache. */
data class HomeData(
    // Null until the locations payload has been cached at least once.
    val locations: List<Location>?,
    // Cache date (yyyy-MM-dd) -> slug -> that day's hours ranges.
    val hoursByDate: Map<String, Map<String, List<HoursRange>>>,
)

private const val DiningHallType = "dining-halls"
private const val PrefetchDays = 3
private const val MenuRetentionMillis = 7L * 24 * 60 * 60 * 1000
private const val TAG = "ContentRepository"

/**
 * Single source of truth for backend content. UI observes the Room cache; network
 * refreshes write into it. Prefetching runs in [appScope] so it survives the
 * ViewModel that triggered it.
 */
@Suppress("TooManyFunctions")
@Singleton
class ContentRepository @Inject constructor(
    private val apiClient: APIClient,
    private val payloadDao: PayloadDao,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeHome(): Flow<HomeData> =
        combine(observeLocations(), observeHoursByDate()) { locations, hoursByDate ->
            HomeData(locations, hoursByDate)
        }

    fun observeLocations(): Flow<List<Location>?> =
        payloadDao.observe(PayloadKeys.LOCATIONS).map { decode<LocationList>(it)?.locations }

    fun observeMenus(locationId: Int): Flow<MenuList?> =
        payloadDao.observe(PayloadKeys.menus(locationId)).map { decode<MenuList>(it) }

    fun observeSections(locationId: Int, menuId: Int): Flow<SectionList?> =
        payloadDao.observe(PayloadKeys.sections(locationId, menuId)).map { decode<SectionList>(it) }

    /** Fetches locations and the next [PrefetchDays] days of hours, then prefetches menus. */
    suspend fun refreshHome() = coroutineScope {
        val locationsDeferred = async { apiClient.listLocations() }
        val hoursDeferred = async { fetch { apiClient.listHours(PrefetchDays) } }
        val locations = locationsDeferred.await()
        store(PayloadKeys.LOCATIONS, locations)
        val hours = hoursDeferred.await()
        if (hours != null) store(PayloadKeys.HOURS, hours)
        appScope.launch { prefetchMenusForOpenLocations(locations.locations, hours) }
        prune()
    }

    suspend fun refreshMenus(locationId: Int) {
        store(PayloadKeys.menus(locationId), apiClient.listMenus(locationId))
    }

    suspend fun refreshSections(locationId: Int, menuId: Int) {
        store(PayloadKeys.sections(locationId, menuId), apiClient.listSection(locationId, menuId))
    }

    private suspend fun prefetchMenusForOpenLocations(
        locations: List<Location>,
        hours: HoursList?,
    ) {
        if (hours == null) return
        val today = LocalDate.now(ncsuZone).toString()
        val hoursBySlug = hours.locations.associate { location ->
            location.slug to location.days.firstOrNull { it.date == today }?.hours.orEmpty()
        }
        locations
            .filter { it.type == DiningHallType }
            .filter { isOpen(currentStatus(hoursBySlug[it.slug], "")) }
            .forEach { prefetchMenusAndTodaySections(it.id) }
    }

    private suspend fun prefetchMenusAndTodaySections(locationId: Int) {
        val menus = fetch { apiClient.listMenus(locationId) } ?: return
        store(PayloadKeys.menus(locationId), menus)
        val today = LocalDate.now(ncsuZone).toString()
        menus.menus
            .filter { it.date == today }
            .forEach { menu ->
                fetch { apiClient.listSection(locationId, menu.id) }?.let { sections ->
                    store(PayloadKeys.sections(locationId, menu.id), sections)
                }
            }
    }

    private suspend fun prune() {
        val cutoff = System.currentTimeMillis() - MenuRetentionMillis
        payloadDao.deleteStale(cutoff, PayloadKeys.MenusPrefix)
        payloadDao.deleteStale(cutoff, PayloadKeys.SectionsPrefix)
    }

    private fun observeHoursByDate(): Flow<Map<String, Map<String, List<HoursRange>>>> =
        payloadDao.observe(PayloadKeys.HOURS).map { body ->
            decode<HoursList>(body)
                ?.locations
                .orEmpty()
                .flatMap { location ->
                    location.days.map { day -> Triple(day.date, location.slug, day.hours) }
                }
                .groupBy({ it.first }, { it.second to it.third })
                .mapValues { (_, bySlug) -> bySlug.toMap() }
        }

    private fun isOpen(status: LocationStatus): Boolean =
        status is LocationStatus.Open || status is LocationStatus.ClosingSoon

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> fetch(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logApiError(TAG, e)
            null
        }

    private suspend inline fun <reified T> store(key: String, value: T) {
        payloadDao.put(
            CachedPayload(
                key = key,
                body = json.encodeToString(value),
                fetchedAt = System.currentTimeMillis(),
            )
        )
    }

    private inline fun <reified T> decode(body: String?): T? =
        body?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }
}
