package org.appdevncsu.foodfinder.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        val today = LocalDate.now(ncsuZone)
        val locationsDeferred = async { apiClient.listLocations() }
        val hoursDeferred = async { refreshHours(today) }
        val locations = locationsDeferred.await()
        store(PayloadKeys.LOCATIONS, locations)
        val todayHours = hoursDeferred.await()
        appScope.launch { prefetchMenusForOpenLocations(locations.locations, todayHours) }
        prune(today)
    }

    suspend fun refreshMenus(locationId: Int) {
        store(PayloadKeys.menus(locationId), apiClient.listMenus(locationId))
    }

    suspend fun refreshSections(locationId: Int, menuId: Int) {
        store(PayloadKeys.sections(locationId, menuId), apiClient.listSection(locationId, menuId))
    }

    /** Hours are best-effort: a failure just leaves that day uncached. */
    private suspend fun refreshHours(today: LocalDate): HoursList? = coroutineScope {
        val dates = (0 until PrefetchDays).map { today.plusDays(it.toLong()) }
        val fetched = dates
            .map { date -> async { date to fetch { apiClient.listHours(date.toString()) } } }
            .awaitAll()
        fetched.forEach { (date, hours) ->
            if (hours != null) store(PayloadKeys.hours(date), hours)
        }
        fetched.firstOrNull { (date, _) -> date == today }?.second
    }

    private suspend fun prefetchMenusForOpenLocations(
        locations: List<Location>,
        todayHours: HoursList?,
    ) {
        val hoursBySlug = todayHours?.locations?.associate { it.slug to it.hours } ?: return
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

    private suspend fun prune(today: LocalDate) {
        payloadDao.keysWithPrefix(PayloadKeys.HoursPrefix).forEach { key ->
            val date = runCatching {
                LocalDate.parse(key.removePrefix(PayloadKeys.HoursPrefix))
            }.getOrNull()
            if (date != null && date.isBefore(today)) {
                payloadDao.delete(key)
            }
        }
        val cutoff = System.currentTimeMillis() - MenuRetentionMillis
        payloadDao.deleteStaleMenus(cutoff, PayloadKeys.MenusPrefix)
        payloadDao.deleteStaleSections(cutoff, PayloadKeys.SectionsPrefix)
    }

    private fun observeHoursByDate(): Flow<Map<String, Map<String, List<HoursRange>>>> =
        payloadDao.observeWithPrefix(PayloadKeys.HoursPrefix).map { rows ->
            rows.associate { row ->
                val date = row.key.removePrefix(PayloadKeys.HoursPrefix)
                val bySlug = decode<HoursList>(row.body)
                    ?.locations
                    ?.associate { it.slug to it.hours }
                    .orEmpty()
                date to bySlug
            }
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
