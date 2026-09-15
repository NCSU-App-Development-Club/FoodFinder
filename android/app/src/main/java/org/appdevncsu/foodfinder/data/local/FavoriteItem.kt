package org.appdevncsu.foodfinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A menu item the user has favorited.
 *
 * Menu items are scraped once per menu occurrence and get a fresh NetNutrition ID
 * every time, so favorites are keyed by a normalized version of the item name.
 */
@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val normalizedName: String,
    val displayName: String,
    val createdAt: Long,
)
