package org.appdevncsu.foodfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.composables.LocationList
import org.appdevncsu.foodfinder.composables.MenuList
import org.appdevncsu.foodfinder.composables.MenuSectionList
import org.appdevncsu.foodfinder.composables.TopBar
import org.appdevncsu.foodfinder.data.sampleLocations
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

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

    Scaffold(modifier = modifier, topBar = {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val (title, subtitle) = when {
            currentBackStackEntry?.destination?.hasRoute<Route.Home>() == true -> {
                "FoodFinder" to "NCSU Dining"
            }

            currentBackStackEntry?.destination?.hasRoute<Route.MenuList>() == true -> {
                "Menus" to "" // TODO fill in dining location name
            }

            currentBackStackEntry?.destination?.hasRoute<Route.Menu>() == true -> {
                "Menu" to "" // TODO fill in dining menu date + name
            }

            else -> {
                "FoodFinder" to "NCSU Dining"
            }
        }

        TopBar(title, subtitle)
    }) { innerPadding ->
        NavHost(
            navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = Route.MenuList(1)
        ) {
            composable<Route.Home> {
                Column {
                    LocationList(onLocationClick = { id -> navController.navigate(Route.MenuList(id)) })
                }
            }

            composable<Route.MenuList> { backStackEntry ->
                val unitID = backStackEntry.toRoute<Route.MenuList>().unitId
                val loc = sampleLocations.first { it.unitId == unitID }
                MenuList(loc.unitId, navController)
            }

            composable<Route.Menu> { backStackEntry ->
                val menuId = backStackEntry.toRoute<Route.Menu>().menuId
                MenuSectionList(menuId)
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
    data class MenuList(val unitId: Int) : Route()

    @Serializable
    @SerialName("menu")
    data class Menu(val menuId: Int) : Route()
}
