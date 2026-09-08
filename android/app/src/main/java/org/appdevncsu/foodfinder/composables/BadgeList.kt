package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Item

@Composable
fun BadgeList(menuItem: Item, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        menuItem.flags.forEach { flag ->
            val badge = badgeMap[flag] ?: return
            Badge(badge)
        }
    }
}

data class BadgeInfo(
    val flagName: String,
    val drawableRes: Int,
    val descriptionRes: Int
)

private val badgeMap = mapOf(
    "Wolf Approved" to BadgeInfo("Wolf Approved", R.drawable.wolf_approved, R.string.badge_wolf_approved),
    "Vegetarian" to BadgeInfo("Vegetarian", R.drawable.vegetarian, R.string.badge_vegetarian),
    "Vegan" to BadgeInfo("Vegan", R.drawable.vegan, R.string.badge_vegan),
    "Soy" to BadgeInfo("Soy", R.drawable.soy, R.string.badge_soy),
    "Halal (U)" to BadgeInfo("Halal (U)", R.drawable.halal_u, R.string.badge_halal),
    "Eggs" to BadgeInfo("Eggs", R.drawable.eggs, R.string.badge_eggs),
    "Contains Sesame" to BadgeInfo(
        "Contains Sesame",
        R.drawable.contains_sesame,
        R.string.badge_sesame
    ),
    "Contains Seafood" to BadgeInfo(
        "Contains Seafood",
        R.drawable.contains_seafood,
        R.string.badge_seafood
    ),
    "Contains Pork" to BadgeInfo("Contains Pork", R.drawable.contains_pork, R.string.badge_pork),
    "Contains Nuts" to BadgeInfo("Contains Nuts", R.drawable.contains_nuts, R.string.badge_nuts),
    "Contains Gluten" to BadgeInfo(
        "Contains Gluten",
        R.drawable.contains_gluten,
        R.string.badge_gluten
    ),
    "Contains Dairy" to BadgeInfo("Contains Dairy", R.drawable.contains_dairy, R.string.badge_dairy)
)
