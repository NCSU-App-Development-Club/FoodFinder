package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_24px),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        // Top and horizontal insets are applied here. The bottom
        // inset is left for scrolling content so lists can draw behind the
        // transparent gesture bar.
        contentWindowInsets = scaffoldContentWindowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenScaffoldPreview() {
    FoodFinderTheme {
        ScreenScaffold(
            title = "Fountain Dining Hall",
            subtitle = stringResource(R.string.today),
            onBack = {},
        ) {
            Text("Content")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenScaffoldNoSubtitlePreview() {
    FoodFinderTheme {
        ScreenScaffold(title = stringResource(R.string.app_name)) {
            Text("Content")
        }
    }
}
