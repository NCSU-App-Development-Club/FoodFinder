package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Item
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme

private const val FavoriteBackgroundAlpha = 0.4f

@Composable
fun MenuItem(
    menuItem: Item,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (isFavorite) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = FavoriteBackgroundAlpha))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = menuItem.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BadgeList(menuItem)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.star_filled_24px else R.drawable.star_24px
                ),
                contentDescription = stringResource(
                    if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                ),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
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
        MenuItem(menuItem = SampleMenuItem, isFavorite = false, onToggleFavorite = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuItemFavoritePreview() {
    FoodFinderTheme {
        MenuItem(menuItem = SampleMenuItem, isFavorite = true, onToggleFavorite = {})
    }
}
