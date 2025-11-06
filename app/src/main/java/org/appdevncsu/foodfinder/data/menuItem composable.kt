package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R


// -------------------------------
// Data Models
// -------------------------------
data class DiningMenuItem(
    val name: String,
    val flags: List<String>,
    val itemId: Int
)

data class BadgeInfo(
    val flagName: String,
    val drawableRes: Int,
    val description: String
)

// -------------------------------
// Map of all flag: badge info
// -------------------------------
private val badgeMap = mapOf(
    "Wolf Approved" to BadgeInfo("Wolf Approved", R.drawable.wolf_approved, "Wolf Approved"),
    "Vegetarian" to BadgeInfo("Vegetarian", R.drawable.vegetarian, "Vegetarian"),
    "Vegan" to BadgeInfo("Vegan", R.drawable.vegan, "Vegan"),
    "Soy" to BadgeInfo("Soy", R.drawable.soy, "Contains soy"),
    "Halal (U)" to BadgeInfo("Halal (U)", R.drawable.halal_u, "Halal (U)"),
    "Eggs" to BadgeInfo("Eggs", R.drawable.eggs, "Contains eggs"),
    "Contains Sesame" to BadgeInfo("Contains Sesame", R.drawable.contains_sesame, "Contains sesame"),
    "Contains Seafood" to BadgeInfo("Contains Seafood", R.drawable.contains_seafood, "Contains seafood"),
    "Contains Pork" to BadgeInfo("Contains Pork", R.drawable.contains_pork, "Contains pork"),
    "Contains Nuts" to BadgeInfo("Contains Nuts", R.drawable.contains_nuts, "Contains nuts"),
    "Contains Gluten" to BadgeInfo("Contains Gluten", R.drawable.contains_gluten, "Contains gluten"),
    "Contains Dairy" to BadgeInfo("Contains Dairy", R.drawable.contains_dairy, "Contains dairy")
)

fun generateBadgeList(menuItem: DiningMenuItem): List<BadgeInfo> {
    return menuItem.flags.mapNotNull { badgeMap[it] }
}

// -------------------------------
// Composable UI
// -------------------------------
@Composable
fun MenuItem(menuItem: DiningMenuItem, modifier: Modifier = Modifier) {
    var badgeText by remember { mutableStateOf<String?>(null) }
    val badges = generateBadgeList(menuItem)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(text = menuItem.name, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            badges.forEach { badge ->
                Image(
                    painter = painterResource(badge.drawableRes),
                    contentDescription = "${badge.description} Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = badge.description }
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.star_outline),
            contentDescription = "Favorite",
            modifier = Modifier
                .size(24.dp)
                .padding(start = 8.dp)
        )
    }

    badgeText?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}
