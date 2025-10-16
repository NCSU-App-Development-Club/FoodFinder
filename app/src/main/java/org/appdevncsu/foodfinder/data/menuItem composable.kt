package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.DiningMenuItem


// -------------------------------
// Composable for a Menu Item
// -------------------------------
@Composable
fun MenuItem(menuItem: DiningMenuItem, modifier: Modifier = Modifier) {
    var badgeText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        // Menu item name
        Text(
            text = menuItem.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Image(
        painter = painterResource(R.drawable.wolf_approved),
        contentDescription = "Wolf Approved Badge",
        modifier = Modifier
            .size(30.dp)
        )

        Spacer(modifier = Modifier.height(4.dp)) // small space

        // Badges below text
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (menuItem.flags.contains("Wolf Approved")) {
                Image(
                    painter = painterResource(R.drawable.wolf_approved),
                    contentDescription = "Wolf Approved Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Wolf Approved" }
                )
            }
            if (menuItem.flags.contains("Vegetarian")) {
                Image(
                    painter = painterResource(R.drawable.vegetarian),
                    contentDescription = "Vegetarian Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Vegetarian" }
                )
            }
            if (menuItem.flags.contains("Vegan")) {
                Image(
                    painter = painterResource(R.drawable.vegan),
                    contentDescription = "Vegan Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Vegan" }
                )
            }
            if (menuItem.flags.contains("Soy")) {
                Image(
                    painter = painterResource(R.drawable.soy),
                    contentDescription = "Soy Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains soy" }
                )
            }
            if (menuItem.flags.contains("Halal (U)")) {
                Image(
                    painter = painterResource(R.drawable.halal_u),
                    contentDescription = "Halal (U) Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Halal (U)" }
                )
            }
            if (menuItem.flags.contains("Eggs")) {
                Image(
                    painter = painterResource(R.drawable.eggs),
                    contentDescription = "Eggs Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains eggs" }
                )
            }
            if (menuItem.flags.contains("Contains Sesame")) {
                Image(
                    painter = painterResource(R.drawable.contains_sesame),
                    contentDescription = "Contains Sesame Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains sesame" }
                )
            }
            if (menuItem.flags.contains("Contains Seafood")) {
                Image(
                    painter = painterResource(R.drawable.contains_seafood),
                    contentDescription = "Contains Seafood Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains seafood" }
                )
            }
            if (menuItem.flags.contains("Contains Pork")) {
                Image(
                    painter = painterResource(R.drawable.contains_pork),
                    contentDescription = "Contains Pork Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains pork" }
                )
            }
            if (menuItem.flags.contains("Contains Nuts")) {
                Image(
                    painter = painterResource(R.drawable.contains_nuts),
                    contentDescription = "Contains Nuts Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains nuts" }
                )
            }
            if (menuItem.flags.contains("Contains Gluten")) {
                Image(
                    painter = painterResource(R.drawable.contains_gluten),
                    contentDescription = "Contains Gluten Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains gluten" }
                )
            }
            if (menuItem.flags.contains("Contains Dairy")) {
                Image(
                    painter = painterResource(R.drawable.contains_dairy),
                    contentDescription = "Contains Dairy Badge",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { badgeText = "Contains dairy" }
                )
            }
        }
    }

    // Show clicked flag info below
    badgeText?.let { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}
