package org.appdevncsu.foodfinder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayloadDao {

    @Query("SELECT body FROM cached_payload WHERE key = :key")
    fun observe(key: String): Flow<String?>

    @Query("SELECT * FROM cached_payload WHERE key LIKE :prefix || '%'")
    fun observeWithPrefix(prefix: String): Flow<List<CachedPayload>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(payload: CachedPayload)

    @Query("DELETE FROM cached_payload WHERE key = :key")
    suspend fun delete(key: String)

    @Query("SELECT key FROM cached_payload WHERE key LIKE :prefix || '%'")
    suspend fun keysWithPrefix(prefix: String): List<String>

    @Query(
        "DELETE FROM cached_payload WHERE fetchedAt < :cutoff " +
            "AND key LIKE :menusPrefix || '%'"
    )
    suspend fun deleteStaleMenus(cutoff: Long, menusPrefix: String)

    @Query(
        "DELETE FROM cached_payload WHERE fetchedAt < :cutoff " +
            "AND key LIKE :sectionsPrefix || '%'"
    )
    suspend fun deleteStaleSections(cutoff: Long, sectionsPrefix: String)
}
