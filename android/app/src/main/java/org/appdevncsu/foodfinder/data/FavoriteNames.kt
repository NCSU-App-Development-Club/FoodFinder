package org.appdevncsu.foodfinder.data

import java.util.Locale

/**
 * Favorites are keyed by menu item name (item IDs change on every scrape), so
 * names are normalized to match them case- and whitespace-insensitively.
 */
fun normalizeFavoriteName(name: String): String = name.trim().lowercase(Locale.ROOT)
