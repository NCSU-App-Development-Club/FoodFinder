package org.appdevncsu.foodfinder.data

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R


// -------------------------------
// Data Models
// -------------------------------

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

fun generateBadgeList(menuItem: DiningMenuItem): List<BadgeInfo> {
    return menuItem.flags.mapNotNull { badgeMap[it] }
}

// -------------------------------
// Composable UI
// -------------------------------
@Composable
fun MenuItem(menuItem: DiningMenuItem, modifier: Modifier = Modifier) {
    val badges = generateBadgeList(menuItem)
    val context = LocalContext.current

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
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
                            .clickable {
                                Toast.makeText(
                                    context,
                                    badge.description,
                                    3
                                )
                            }
                    )
                }
            }
        }

        Icon(
            painter = painterResource(R.drawable.favorite_24px),
            contentDescription = "Favorite",
            modifier = Modifier.size(24.dp),
            tint = Color.Red
        )
    }
}

@Preview
@Composable
private fun MenuItemPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        MenuItem(sampleMenuItems.get(2))
    }
}
