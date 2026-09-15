package org.appdevncsu.foodfinder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY displayName")
    fun observeAll(): Flow<List<FavoriteItem>>

    @Query("SELECT normalizedName FROM favorites")
    fun observeNormalizedNames(): Flow<List<String>>

    @Query("SELECT displayName FROM favorites ORDER BY displayName")
    suspend fun displayNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE normalizedName = :normalizedName)")
    suspend fun exists(normalizedName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteItem)

    @Query("DELETE FROM favorites WHERE normalizedName = :normalizedName")
    suspend fun delete(normalizedName: String)
}
