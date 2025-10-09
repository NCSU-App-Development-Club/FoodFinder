package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp




data class DiningMenuItem(
    val name: String,
    val flags: List<String>,
    val itemId: Int
)

@Composable
fun menuItem(menuItem: DiningMenuItem, modifier: Modifier = Modifier) {
    var badgeText by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = menuItem.name,
            modifier = modifier.weight(1f)
        )

        // Show flag icons next to menu item name
        if (menuItem.flags.contains("Wolf Approved")) {
            Image(
                painter = painterResource(R.drawable.wolf_approved),
                contentDescription = "Wolf Approved Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Wolf Approved"
                    }
            )
        }
        if (menuItem.flags.contains("Vegetarian")) {
            Image(
                painter = painterResource(R.drawable.vegetarian),
                contentDescription = "Vegetarian Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Vegetarian"
                    }
            )
        }
        if (menuItem.flags.contains("Vegan")) {
            Image(
                painter = painterResource(R.drawable.vegan),
                contentDescription = "Vegan Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Vegan"
                    }
            )
        }
        if (menuItem.flags.contains("Soy")) {
            Image(
                painter = painterResource(R.drawable.soy),
                contentDescription = "Soy Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains soy"
                    }
            )
        }
        if (menuItem.flags.contains("Halal (U)")) {
            Image(
                painter = painterResource(R.drawable.halal_u),
                contentDescription = "Halal (U) Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Halal (U)"
                    }
            )
        }
        if (menuItem.flags.contains("Eggs")) {
            Image(
                painter = painterResource(R.drawable.eggs),
                contentDescription = "Eggs Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains eggs"
                    }
            )
        }
        if (menuItem.flags.contains("Contains Sesame")) {
            Image(
                painter = painterResource(R.drawable.contains_sesame),
                contentDescription = "Contains Sesame Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains sesasme"

                    }
            )
        }
        if (menuItem.flags.contains("Contains Seafood")) {
            Image(
                painter = painterResource(R.drawable.contains_seafood),
                contentDescription = "Contains Seafood Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains seafood"
                    }
            )
        }
        if (menuItem.flags.contains("Contains Pork")) {
            Image(
                painter = painterResource(R.drawable.contains_pork),
                contentDescription = "Contains Pork Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains pork"
                    }
            )
        }
        if (menuItem.flags.contains("Contains Nuts")) {
            Image(
                painter = painterResource(R.drawable.contains_nuts),
                contentDescription = "Contains Nuts Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains nuts"
                    }
            )
        }
        if (menuItem.flags.contains("Contains Gluten")) {
            Image(
                painter = painterResource(R.drawable.contains_gluten),
                contentDescription = "Contains Gluten Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains gluten"
                    }
            )
        }
        if (menuItem.flags.contains("Contains Dairy")) {
            Image(
                painter = painterResource(R.drawable.contains_dairy),
                contentDescription = "Contains Dairy Badge",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        badgeText = "Contains dairy"
                    }
            )
        }
    }
}