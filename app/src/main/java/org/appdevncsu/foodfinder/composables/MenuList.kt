package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.data.sampleMenuListItems

@Composable
fun MenuList(menus: List<DiningMenuListItem>, modifier: Modifier = Modifier) {
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
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Text(fontSize = 18.sp, text = menu.name, modifier = Modifier.padding(vertical = 10.dp))
                        Image(
                            painter = painterResource(R.drawable.keyboard_arrow_right_24px),
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
        MenuList(sampleMenuListItems)
    }
}
