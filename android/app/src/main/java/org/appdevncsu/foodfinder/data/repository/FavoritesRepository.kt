package org.appdevncsu.foodfinder.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.FavoriteRequest
import org.appdevncsu.foodfinder.data.FavoritesResponse
import org.appdevncsu.foodfinder.data.local.CachedPayload
import org.appdevncsu.foodfinder.data.local.FavoriteDao
import org.appdevncsu.foodfinder.data.local.FavoriteItem
import org.appdevncsu.foodfinder.data.local.PayloadDao
import org.appdevncsu.foodfinder.data.local.PayloadKeys
import org.appdevncsu.foodfinder.data.ncsuZone
import org.appdevncsu.foodfinder.data.normalizeFavoriteName
import org.appdevncsu.foodfinder.di.ApplicationScope
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val payloadDao: PayloadDao,
    private val apiClient: APIClient,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val refreshMutex = Mutex()
    private var inFlightRefresh: Deferred<FavoritesResponse?>? = null

    fun observeFavorites(): Flow<List<FavoriteItem>> = favoriteDao.observeAll()

    fun observeNormalizedNames(): Flow<Set<String>> =
        favoriteDao.observeNormalizedNames().map { it.toSet() }

    /** Today's menus that contain favorites. */
    fun observeTodayMatches(): Flow<FavoritesResponse?> =
        payloadDao.observe(PayloadKeys.favoriteMatches(today()))
            .map { decode<FavoritesResponse>(it) }

    /**
     * Adds [displayName] to favorites, or removes it if already favorited.
     * Returns true if the favorite was added.
     */
    suspend fun toggle(displayName: String): Boolean {
        val normalizedName = normalizeFavoriteName(displayName)
        val added = !favoriteDao.exists(normalizedName)
        if (added) {
            favoriteDao.upsert(
                FavoriteItem(
                    normalizedName = normalizedName,
                    displayName = displayName,
                    createdAt = System.currentTimeMillis(),
                )
            )
            payloadDao.deleteByPrefix(PayloadKeys.FavoriteMatchesPrefix)
        } else {
            favoriteDao.delete(normalizedName)
            removeItemFromCachedMatches(normalizedName)
        }
        return added
    }

    /** Removes [normalizedName] from today's cached matches, keeping the rest of the entry intact. */
    private suspend fun removeItemFromCachedMatches(normalizedName: String) {
        val key = PayloadKeys.favoriteMatches(today())
        val cached = payloadDao.get(key) ?: return
        val response = decode<FavoritesResponse>(cached.body) ?: return
        val updated = response.copy(
            matches = response.matches.mapNotNull { match ->
                val items = match.items.filterNot { normalizeFavoriteName(it) == normalizedName }
                if (items.isEmpty()) null else match.copy(items = items)
            }
        )
        payloadDao.put(cached.copy(body = json.encodeToString(updated)))
    }

    /**
     * Returns the menus that contain the user's favorites for today, fetching and caching them.
     * Returns null when the user has no favorites.
     *
     * Concurrent callers share a single in-flight fetch, so multiple screens asking at once
     * result in one network request. Unless [force] is set, a result cached within
     * [MATCH_CACHE_TTL_MILLIS] is reused too.
     */
    suspend fun refreshMatches(force: Boolean = false): FavoritesResponse? {
        val hasFavorites = favoriteDao.displayNames().isNotEmpty()
        val cached = if (hasFavorites && !force) freshMatches() else null
        val refresh = when {
            !hasFavorites -> CompletableDeferred<FavoritesResponse?>(null)
            cached != null -> CompletableDeferred<FavoritesResponse?>(cached)
            else -> refreshMutex.withLock {
                // Re-check under the lock so a fetch that just finished wins the race.
                val fresh = if (force) null else freshMatches()
                if (fresh != null) {
                    CompletableDeferred<FavoritesResponse?>(fresh)
                } else {
                    inFlightRefresh?.takeIf { it.isActive }
                        ?: appScope.async { fetchAndStoreMatches() }.also { inFlightRefresh = it }
                }
            }
        }
        return refresh.await()
    }

    /** Today's cached matches if they were fetched within the TTL. */
    private suspend fun freshMatches(): FavoritesResponse? =
        payloadDao.get(PayloadKeys.favoriteMatches(today()))
            ?.takeIf { System.currentTimeMillis() - it.fetchedAt < MATCH_CACHE_TTL_MILLIS }
            ?.let { decode<FavoritesResponse>(it.body) }

    private suspend fun fetchAndStoreMatches(): FavoritesResponse? {
        val names = favoriteDao.displayNames()
        if (names.isEmpty()) return null
        val response = apiClient.favoriteMenus(FavoriteRequest(items = names, days = MATCH_DAYS))
        payloadDao.put(
            CachedPayload(
                key = PayloadKeys.favoriteMatches(today()),
                body = json.encodeToString(response),
                fetchedAt = System.currentTimeMillis(),
            )
        )
        return response
    }

    private fun today(): String = LocalDate.now(ncsuZone).toString()

    private inline fun <reified T> decode(body: String?): T? =
        body?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    private companion object {
        const val MATCH_DAYS = 1
        const val MATCH_CACHE_TTL_MILLIS = 60L * 60 * 1000
    }
}
