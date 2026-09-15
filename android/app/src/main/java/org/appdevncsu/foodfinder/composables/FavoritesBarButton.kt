package org.appdevncsu.foodfinder.composables

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.viewmodel.FavoritesBadgeViewModel

/**
 * Top-bar shortcut to the favorites page, badged with the number of favorite items being served
 * today. The badge is hidden when nothing is available.
 */
@Composable
fun FavoritesBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesBadgeViewModel = hiltViewModel(),
) {
    val count by viewModel.count.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge {
                        Text(text = count.toString())
                    }
                }
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.star_24px),
                contentDescription = stringResource(R.string.favorites_page_title),
            )
        }
    }
}
