package org.appdevncsu.foodfinder.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R

@Composable
fun Badge(badge: BadgeInfo, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val description = stringResource(badge.descriptionRes)

    Image(
        painter = painterResource(badge.drawableRes),
        contentDescription = stringResource(R.string.badge_content_description, description),
        modifier = modifier
            .size(18.dp)
            .clickable {
                Toast.makeText(
                    context,
                    description,
                    Toast.LENGTH_SHORT
                ).show()
            }
    )
}
