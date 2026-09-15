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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(payload: CachedPayload)

    @Query(
        "DELETE FROM cached_payload WHERE fetchedAt < :cutoff " +
            "AND key LIKE :prefix || '%'"
    )
    suspend fun deleteStale(cutoff: Long, prefix: String)

    @Query("DELETE FROM cached_payload WHERE key LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)
}
