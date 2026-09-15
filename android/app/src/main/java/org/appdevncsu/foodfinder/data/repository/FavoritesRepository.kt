package org.appdevncsu.foodfinder.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val payloadDao: PayloadDao,
    private val apiClient: APIClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeFavorites(): Flow<List<FavoriteItem>> = favoriteDao.observeAll()

    fun observeNormalizedNames(): Flow<Set<String>> =
        favoriteDao.observeNormalizedNames().map { it.toSet() }

    /** Today's menus that contain favorites. */
    fun observeTodayMatches(): Flow<FavoritesResponse?> =
        payloadDao.observe(PayloadKeys.favoriteMatches(today()))
            .map { decode<FavoritesResponse>(it) }

    /** Adds [displayName] to favorites, or removes it if already favorited. */
    suspend fun toggle(displayName: String) {
        val normalizedName = normalizeFavoriteName(displayName)
        if (favoriteDao.exists(normalizedName)) {
            favoriteDao.delete(normalizedName)
        } else {
            favoriteDao.upsert(
                FavoriteItem(
                    normalizedName = normalizedName,
                    displayName = displayName,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
        // The cached matches no longer reflect the favorites list, so invalidate them.
        payloadDao.deleteByPrefix(PayloadKeys.FavoriteMatchesPrefix)
    }

    /**
     * Fetches the menus that contain the user's favorites and stores them for today.
     * Returns null when the user has no favorites.
     */
    suspend fun refreshMatches(): FavoritesResponse? {
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
    }
}
