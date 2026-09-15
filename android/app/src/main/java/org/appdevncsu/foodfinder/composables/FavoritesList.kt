package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Item
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import org.appdevncsu.foodfinder.viewmodel.FavoritesViewModel

private const val FavoritesChevronRotationDegrees = 90f

@Composable
fun FavoritesList(
    modifier: Modifier = Modifier,
    onMenuClick: (FavoritesViewModel.MenuGroup) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    FavoritesListContent(
        groups = state.groups,
        onToggleFavorite = viewModel::toggleFavorite,
        onMenuClick = onMenuClick,
        modifier = modifier,
    )
}

@Composable
internal fun FavoritesListContent(
    groups: List<FavoritesViewModel.LocationGroup>,
    modifier: Modifier = Modifier,
    onToggleFavorite: (String) -> Unit = {},
    onMenuClick: (FavoritesViewModel.MenuGroup) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .consumeBottomNavBarInsets(),
        contentPadding = bottomNavBarContentPadding(),
    ) {
        if (groups.isEmpty()) {
            item(key = "favorites-empty") {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
            }
            return@LazyColumn
        }
        groups.forEach { location ->
            item(key = "favorites-location-${location.locationId}") {
                ExpandableLocationGroup(
                    group = location,
                    onToggleFavorite = onToggleFavorite,
                    onMenuClick = onMenuClick,
                )
            }
        }
    }
}

@Composable
private fun ExpandableLocationGroup(
    group: FavoritesViewModel.LocationGroup,
    onToggleFavorite: (String) -> Unit,
    onMenuClick: (FavoritesViewModel.MenuGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier.padding(
            top = 16.dp,
            bottom = if (expanded) 16.dp else 4.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = group.locationName, style = MaterialTheme.typography.titleLarge)
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                modifier = Modifier.rotate(
                    if (expanded) FavoritesChevronRotationDegrees else 0f
                ),
            )
        }

        if (expanded) {
            group.menus.forEach { menu ->
                Text(
                    text = menu.menuName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                menu.items.forEach { itemName ->
                    MenuItem(
                        menuItem = Item(name = itemName, id = 0, sectionId = 0, flags = emptyList()),
                        isFavorite = true,
                        onToggleFavorite = { onToggleFavorite(itemName) },
                    )
                }
                TextButton(onClick = { onMenuClick(menu) }) {
                    Text(text = stringResource(R.string.favorites_view_menu))
                }
            }
        }
    }
}

private val SampleFavoritesGroups = listOf(
    FavoritesViewModel.LocationGroup(
        locationId = 1,
        locationName = "Fountain Dining Hall",
        menus = listOf(
            FavoritesViewModel.MenuGroup(
                locationId = 1,
                menuId = 1,
                menuName = "Breakfast",
                date = "2026-09-15",
                items = listOf("Vanilla Granola", "Fresh Cantaloupe"),
            ),
            FavoritesViewModel.MenuGroup(
                locationId = 1,
                menuId = 2,
                menuName = "Lunch",
                date = "2026-09-15",
                items = listOf("Cheese Pizza"),
            ),
        ),
    ),
)

@Composable
@Preview(showBackground = true)
private fun FavoritesListContentPreview() {
    FoodFinderTheme {
        FavoritesListContent(groups = SampleFavoritesGroups)
    }
}
