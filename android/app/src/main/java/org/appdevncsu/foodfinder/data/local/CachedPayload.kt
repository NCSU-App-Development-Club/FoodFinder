package org.appdevncsu.foodfinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A raw JSON response from the backend, keyed by endpoint.
 *
 * The body is stored verbatim so that it always matches what the server sent;
 * SQLite's JSON functions can reach into it later if needed.
 */
@Entity(tableName = "cached_payload")
data class CachedPayload(
    @PrimaryKey val key: String,
    val body: String,
    val fetchedAt: Long,
    val etag: String? = null,
)
