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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    val description: String
)

private val badgeMap = mapOf(
    "Wolf Approved" to BadgeInfo("Wolf Approved", R.drawable.wolf_approved, "Wolf Approved"),
    "Vegetarian" to BadgeInfo("Vegetarian", R.drawable.vegetarian, "Vegetarian"),
    "Vegan" to BadgeInfo("Vegan", R.drawable.vegan, "Vegan"),
    "Soy" to BadgeInfo("Soy", R.drawable.soy, "Contains soy"),
    "Halal (U)" to BadgeInfo("Halal (U)", R.drawable.halal_u, "Halal (U)"),
    "Eggs" to BadgeInfo("Eggs", R.drawable.eggs, "Contains eggs"),
    "Contains Sesame" to BadgeInfo(
        "Contains Sesame",
        R.drawable.contains_sesame,
        "Contains sesame"
    ),
    "Contains Seafood" to BadgeInfo(
        "Contains Seafood",
        R.drawable.contains_seafood,
        "Contains seafood"
    ),
    "Contains Pork" to BadgeInfo("Contains Pork", R.drawable.contains_pork, "Contains pork"),
    "Contains Nuts" to BadgeInfo("Contains Nuts", R.drawable.contains_nuts, "Contains nuts"),
    "Contains Gluten" to BadgeInfo(
        "Contains Gluten",
        R.drawable.contains_gluten,
        "Contains gluten"
    ),
    "Contains Dairy" to BadgeInfo("Contains Dairy", R.drawable.contains_dairy, "Contains dairy")
)
