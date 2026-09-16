package org.appdevncsu.foodfinder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.composables.FavoritesBarButton
import org.appdevncsu.foodfinder.composables.FavoritesList
import org.appdevncsu.foodfinder.composables.ItemHistoryCalendar
import org.appdevncsu.foodfinder.composables.LocationList
import org.appdevncsu.foodfinder.composables.MenuList
import org.appdevncsu.foodfinder.composables.MenuSectionList
import org.appdevncsu.foodfinder.composables.ScreenScaffold
import org.appdevncsu.foodfinder.composables.formatMenuDate
import org.appdevncsu.foodfinder.notifications.FavoritesNotifier
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pendingRoute = MutableStateFlow<Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent.toRoute()
        setContent {
            FoodFinderTheme {
                val route by pendingRoute.collectAsState()
                NavigationGraph(
                    modifier = Modifier.fillMaxSize(),
                    pendingRoute = route,
                    onConsumeRoute = { pendingRoute.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.toRoute()
    }
}

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    pendingRoute: Route? = null,
    onConsumeRoute: () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentOnConsumeRoute by rememberUpdatedState(onConsumeRoute)

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute)
            currentOnConsumeRoute()
        }
    }

    NavHost(
        navController,
        modifier = modifier,
        startDestination = Route.Home,
        enterTransition = { forwardEnterTransition() },
        exitTransition = { forwardExitTransition() },
        popEnterTransition = { backEnterTransition() },
        popExitTransition = { backExitTransition() },
        predictivePopEnterTransition = { backEnterTransition() },
        predictivePopExitTransition = { backExitTransition() },
    ) {
        foodFinderDestinations(navController)
    }
}

private fun NavGraphBuilder.foodFinderDestinations(navController: NavController) {
    composable<Route.Home> {
        ScreenScaffold(
            title = stringResource(R.string.app_name),
            actions = {
                FavoritesBarButton(onClick = { navController.navigate(Route.Favorites) })
            },
        ) {
            LocationList(
                onLocationClick = { location ->
                    navController.navigate(Route.MenuList(location.id, location.name))
                },
            )
        }
    }

    composable<Route.MenuList> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.MenuList>()
        ScreenScaffold(
            title = route.locationName,
            onBack = dropUnlessResumed { navController.navigateUp() },
        ) {
            MenuList(route.unitId, navController)
        }
    }

    composable<Route.Menu> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Menu>()
        ScreenScaffold(
            title = route.menuName,
            subtitle = formatMenuDate(route.date),
            onBack = dropUnlessResumed { navController.navigateUp() },
        ) {
            MenuSectionList(
                menuId = route.menuId,
                locationId = route.locationId,
                onItemClick = { item ->
                    navController.navigate(
                        Route.ItemHistory(route.locationId, item.name, route.date)
                    )
                },
            )
        }
    }

    composable<Route.ItemHistory> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.ItemHistory>()
        ScreenScaffold(
            title = route.itemName,
            onBack = dropUnlessResumed { navController.navigateUp() },
        ) {
            ItemHistoryCalendar(route.locationId, route.itemName, route.date)
        }
    }

    composable<Route.Favorites> {
        ScreenScaffold(
            title = stringResource(R.string.favorites_page_title),
            onBack = dropUnlessResumed { navController.navigateUp() },
        ) {
            FavoritesList(
                onMenuClick = { menu ->
                    navController.navigate(
                        Route.Menu(menu.menuId, menu.menuName, menu.date, menu.locationId)
                    )
                },
            )
        }
    }
}

private const val NavTransitionDurationMillis = 300

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnterTransition(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(NavTransitionDurationMillis),
    ) + fadeIn(animationSpec = tween(NavTransitionDurationMillis))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExitTransition(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(NavTransitionDurationMillis),
    ) + fadeOut(animationSpec = tween(NavTransitionDurationMillis))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnterTransition(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(NavTransitionDurationMillis),
    ) + fadeIn(animationSpec = tween(NavTransitionDurationMillis))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExitTransition(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(NavTransitionDurationMillis),
    ) + fadeOut(animationSpec = tween(NavTransitionDurationMillis))

private const val InvalidId = -1

/** The deep link carried by a favorite notification, if this intent has one. */
private fun Intent.toRoute(): Route? {
    val menuId = getIntExtra(FavoritesNotifier.EXTRA_MENU_ID, InvalidId)
    return when {
        getBooleanExtra(FavoritesNotifier.EXTRA_OPEN_FAVORITES, false) -> Route.Favorites
        menuId == InvalidId -> null
        else -> Route.Menu(
            menuId = menuId,
            menuName = getStringExtra(FavoritesNotifier.EXTRA_MENU_NAME).orEmpty(),
            date = getStringExtra(FavoritesNotifier.EXTRA_DATE).orEmpty(),
            locationId = getIntExtra(FavoritesNotifier.EXTRA_LOCATION_ID, InvalidId),
        )
    }
}

@Serializable
sealed class Route {
    @Serializable
    @SerialName("home")
    object Home : Route()

    @Serializable
    @SerialName("favorites")
    object Favorites : Route()

    @Serializable
    @SerialName("menuList")
    data class MenuList(val unitId: Int, val locationName: String) : Route()

    @Serializable
    @SerialName("menu")
    data class Menu(val menuId: Int, val menuName: String, val date: String, val locationId: Int) : Route()

    @Serializable
    @SerialName("itemHistory")
    data class ItemHistory(val locationId: Int, val itemName: String, val date: String) : Route()
}
