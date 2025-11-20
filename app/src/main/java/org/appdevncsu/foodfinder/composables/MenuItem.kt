package org.appdevncsu.foodfinder.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.DiningMenuItem
import org.appdevncsu.foodfinder.data.sampleMenuItems

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
                        painter = painterResource(badge.value),
                        contentDescription = badge.key,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                Toast.makeText(
                                    context, badge.key, 2
                                )
                            })
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

private val badgeMap = mapOf(
    "Wolf Approved" to R.drawable.wolf_approved,
    "Vegetarian" to R.drawable.vegetarian,
    "Vegan" to R.drawable.vegan,
    "Soy" to R.drawable.soy,
    "Halal (U)" to R.drawable.halal_u,
    "Eggs" to R.drawable.eggs,
    "Contains Sesame" to R.drawable.contains_sesame,
    "Contains Seafood" to R.drawable.contains_seafood,
    "Contains Pork" to R.drawable.contains_pork,
    "Contains Nuts" to R.drawable.contains_nuts,
    "Contains Gluten" to R.drawable.contains_gluten,
    "Contains Dairy" to R.drawable.contains_dairy,
)

private fun generateBadgeList(menuItem: DiningMenuItem): List<Map.Entry<String, Int>> {
    return menuItem.flags.mapNotNull { badgeMap.entries.find { entry -> entry.key == it } }
}
