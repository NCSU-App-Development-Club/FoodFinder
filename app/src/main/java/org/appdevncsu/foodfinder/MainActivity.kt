package org.appdevncsu.foodfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodFinderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NavigationGraph(modifier: Modifier) {
    val navController = rememberNavController()

    NavHost(navController, modifier = modifier, startDestination = HomePageDestination) {
        composable<HomePageDestination> {
            LocationList(navController)
        }

        composable<MenuListPageDestination> { backStackEntry ->
            val locationId = backStackEntry.toRoute<MenuListPageDestination>().locationId
            MenuList(locationId)
        }

        composable<MenuPageDestination> { backStackEntry ->
            val menuId = backStackEntry.toRoute<MenuPageDestination>().menuId
            MenuSectionList(menuId)
        }
    }
}

@Serializable
object HomePageDestination

@Serializable
data class MenuListPageDestination(val locationId: Int)

@Serializable
data class MenuPageDestination(val menuId: Int)

/** Placeholder */
@Composable
fun LocationList(navController: NavController) {
//    navController.navigate(MenuListPageDestination(12))
}

/** Placeholder */
@Composable
fun MenuList(locationId: Int) {

}

/** Placeholder */
@Composable
fun MenuSectionList(menuId: Int) {

}
