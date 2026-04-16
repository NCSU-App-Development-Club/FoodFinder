package org.appdevncsu.foodfinder.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun Badge(badge: BadgeInfo, modifier: Modifier = Modifier) {

    val context = LocalContext.current

    Image(
        painter = painterResource(badge.drawableRes),
        contentDescription = "${badge.description} Badge",
        modifier = modifier
            .size(15.dp)
            .clickable {
                Toast.makeText(
                    context,
                    badge.description,
                    Toast.LENGTH_SHORT
                )
            }
    )
}
