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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import org.appdevncsu.foodfinder.viewmodel.MenuListViewModel

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

    val menus by viewModel.menuList.collectAsState()

    MenuListContent(menus, navController, modifier)
}

@Composable
private fun MenuListContent(
    menus: MenuList?,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val dates = menus?.menus?.groupBy { it.date }
    LazyColumn(modifier = modifier.padding(horizontal = 8.dp)) {
        if (dates == null) {
            items(MenuListSkeletonGroupCount) {
                SkeletonMenuGroup()
            }
        } else if (dates.isEmpty()) {
            item {
                EmptyMenuState(modifier = Modifier.fillParentMaxSize())
            }
        } else {
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
                                .clickable { navController.navigate(Route.Menu(menu.id)) }
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
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "No menus available for this location",
            fontSize = 16.sp,
            color = Color.Gray,
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
    MenuListContent(menus = null, navController = rememberNavController())
}

@Composable
@Preview(showBackground = true)
private fun MenuListEmptyPreview() {
    MenuListContent(menus = MenuList(menus = emptyList()), navController = rememberNavController())
}

private val SampleMenuList = MenuList(
    menus = listOf(
        Menu(name = "Breakfast", id = 1, date = "Monday, Sep 7", locationId = 1),
        Menu(name = "Lunch", id = 2, date = "Monday, Sep 7", locationId = 1),
        Menu(name = "Dinner", id = 3, date = "Tuesday, Sep 8", locationId = 1),
    )
)

@Composable
@Preview(showBackground = true)
private fun MenuListPreview() {
    MenuListContent(menus = SampleMenuList, navController = rememberNavController())
}
