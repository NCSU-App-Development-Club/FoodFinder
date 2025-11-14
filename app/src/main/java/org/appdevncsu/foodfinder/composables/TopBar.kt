package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String, subtitle: String, modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color(0xCCFFFFFF),
                    fontSize = 16.sp
                )
            }
        },
        colors = TopAppBarColors(
            containerColor = Color.Red,
            scrolledContainerColor = Color.Red,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

@Preview
@Composable
fun TopBarPreview() {
    TopBar("FoodFinder", "NCSU Dining")
}