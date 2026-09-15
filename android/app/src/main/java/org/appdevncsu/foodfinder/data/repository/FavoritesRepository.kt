package org.appdevncsu.foodfinder.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.appdevncsu.foodfinder.data.local.FavoriteDao
import org.appdevncsu.foodfinder.data.local.FavoriteItem
import org.appdevncsu.foodfinder.data.normalizeFavoriteName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
) {
    fun observeFavorites(): Flow<List<FavoriteItem>> = favoriteDao.observeAll()

    fun observeNormalizedNames(): Flow<Set<String>> =
        favoriteDao.observeNormalizedNames().map { it.toSet() }

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
    }
}
