package org.appdevncsu.foodfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.composables.LocationList
import org.appdevncsu.foodfinder.composables.MenuList
import org.appdevncsu.foodfinder.composables.MenuSectionList
import org.appdevncsu.foodfinder.composables.ScreenScaffold
import org.appdevncsu.foodfinder.composables.formatMenuDate
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodFinderTheme {
                NavigationGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun NavigationGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController,
        modifier = modifier,
        startDestination = Route.Home,
    ) {
        composable<Route.Home> {
            ScreenScaffold(title = "FoodFinder") {
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
                onBack = { navController.navigateUp() },
            ) {
                MenuList(route.unitId, navController)
            }
        }

        composable<Route.Menu> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Menu>()
            ScreenScaffold(
                title = route.menuName,
                subtitle = formatMenuDate(route.date),
                onBack = { navController.navigateUp() },
            ) {
                MenuSectionList(route.menuId, route.locationId)
            }
        }
    }
}

@Serializable
sealed class Route {
    @Serializable
    @SerialName("home")
    object Home : Route()

    @Serializable
    @SerialName("menuList")
    data class MenuList(val unitId: Int, val locationName: String) : Route()

    @Serializable
    @SerialName("menu")
    data class Menu(val menuId: Int, val menuName: String, val date: String, val locationId: Int) : Route()
}
