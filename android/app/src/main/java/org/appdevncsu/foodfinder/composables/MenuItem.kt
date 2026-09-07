package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.data.Item
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

@Composable
fun MenuItem(menuItem: Item, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = menuItem.name, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        BadgeList(menuItem)
    }
}

private val SampleMenuItem = Item(
    name = "Grilled Chicken Sandwich",
    id = 1,
    sectionId = 1,
    flags = listOf("Wolf Approved", "Contains Dairy")
)

@Preview(showBackground = true)
@Composable
private fun MenuItemPreview() {
    FoodFinderTheme {
        MenuItem(menuItem = SampleMenuItem)
    }
}
