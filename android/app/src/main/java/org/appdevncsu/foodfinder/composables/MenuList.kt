package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.Route
import org.appdevncsu.foodfinder.data.Menu
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import org.appdevncsu.foodfinder.viewmodel.MenuListViewModel
import java.time.LocalDate

private const val MenuListSkeletonGroupCount = 2
private const val MenuListSkeletonRowsPerGroup = 3
private const val SkeletonDateBarWidthFraction = 0.4f
private val SkeletonDateBarHeight = 32.dp
private val SkeletonMenuRowHeight = 48.dp

@Composable
fun MenuList(
    locationId: Int,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MenuListViewModel = hiltViewModel()
) {
    LaunchedEffect(locationId) {
        viewModel.loadMenusForLocation(locationId)
    }

    val state by viewModel.uiState.collectAsState()

    MenuListContent(
        state = state,
        navController = navController,
        onRetry = { viewModel.retry(locationId) },
        modifier = modifier,
    )
}

@Composable
internal fun MenuListContent(
    state: MenuListViewModel.UiState,
    navController: NavController,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val menus = state.menuList
    val dates = menus?.menus?.groupBy { it.date }
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .consumeBottomNavBarInsets(),
        contentPadding = bottomNavBarContentPadding(),
    ) {
        if (state.error != null && menus == null) {
            item {
                ErrorState(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        } else if (dates == null) {
            items(MenuListSkeletonGroupCount) {
                SkeletonMenuGroup()
            }
        } else if (dates.isEmpty()) {
            item {
                EmptyMenuState(modifier = Modifier.fillParentMaxSize())
            }
        } else {
            items(dates.keys.sorted()) { date ->
                val menus = dates[date]!!

                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(fontSize = 24.sp, text = formatMenuDate(date))
                    for (menu in menus) {
                        MenuRow(
                            menu = menu,
                            onClick = {
                                navController.navigate(
                                    Route.Menu(menu.id, menu.name, menu.date, menu.locationId)
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(menu: Menu, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Text(
            fontSize = 18.sp,
            text = menu.name,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Icon(
            painter = painterResource(R.drawable.keyboard_arrow_right_24px),
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null
        )
    }
}

@Composable
private fun EmptyMenuState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(R.drawable.restaurant_menu_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(R.string.empty_menus),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SkeletonMenuGroup(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkeletonBar(
            modifier = Modifier
                .fillMaxWidth(SkeletonDateBarWidthFraction)
                .height(SkeletonDateBarHeight)
        )
        repeat(MenuListSkeletonRowsPerGroup) {
            SkeletonBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SkeletonMenuRowHeight)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun MenuListLoadingPreview() {
    FoodFinderTheme {
        MenuListContent(
            state = MenuListViewModel.UiState(loading = true),
            navController = rememberNavController(),
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun MenuListEmptyPreview() {
    FoodFinderTheme {
        MenuListContent(
            state = MenuListViewModel.UiState(menuList = MenuList(menus = emptyList())),
            navController = rememberNavController(),
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun MenuListErrorPreview() {
    FoodFinderTheme {
        MenuListContent(
            state = MenuListViewModel.UiState(error = stringResource(R.string.error_no_connection)),
            navController = rememberNavController(),
        )
    }
}

private val SampleMenuList = MenuList(
    menus = listOf(
        Menu(name = "Breakfast", id = 1, date = LocalDate.now().toString(), locationId = 1),
        Menu(name = "Lunch", id = 2, date = LocalDate.now().toString(), locationId = 1),
        Menu(name = "Dinner", id = 3, date = LocalDate.now().plusDays(1).toString(), locationId = 1),
        Menu(name = "Brunch", id = 4, date = LocalDate.now().plusDays(2).toString(), locationId = 1),
    )
)

@Composable
@Preview(showBackground = true)
private fun MenuListPreview() {
    FoodFinderTheme {
        MenuListContent(
            state = MenuListViewModel.UiState(menuList = SampleMenuList),
            navController = rememberNavController(),
        )
    }
}

@Composable
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MenuListDarkPreview() {
    FoodFinderTheme(darkTheme = true) {
        MenuListContent(
            state = MenuListViewModel.UiState(menuList = SampleMenuList),
            navController = rememberNavController(),
        )
    }
}
