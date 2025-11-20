package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.Route
import org.appdevncsu.foodfinder.data.sampleLocations
import org.appdevncsu.foodfinder.viewmodel.MenuListViewModel

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

    val menus by viewModel.menuList.collectAsState()

    if (menus.isEmpty()) {
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("There are no menus available for this location.")
        }
        return
    }

    val dates = menus.groupBy { it.date }
    LazyColumn(modifier = modifier.padding(horizontal = 8.dp)) {
        items(dates.keys.toList()) { date ->
            val menus = dates[date]!!

            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(fontSize = 24.sp, text = date)
                for (menu in menus) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(4.dp))
                            .clickable { navController.navigate(Route.Menu(menu.menuId)) }
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            fontSize = 18.sp,
                            text = menu.name,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        Icon(
                            painter = painterResource(R.drawable.keyboard_arrow_right_24px),
                            tint = Color.Black,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun MenuListPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        MenuList(sampleLocations.first().unitId, rememberNavController())
    }
}
