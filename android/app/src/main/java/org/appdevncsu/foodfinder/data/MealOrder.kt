package org.appdevncsu.foodfinder.data

import java.util.Locale

private val mealOrderRanks = mapOf(
    "breakfast" to 0,
    "lunch" to 1,
    "dinner" to 2,
    "daily" to 3,
)

/** Sort key for a menu name (breakfast, lunch, dinner, daily); unknown meals sort last. */
fun mealOrder(name: String): Int =
    mealOrderRanks[name.trim().lowercase(Locale.ROOT)] ?: Int.MAX_VALUE
