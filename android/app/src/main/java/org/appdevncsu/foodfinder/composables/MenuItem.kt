package org.appdevncsu.foodfinder.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Item




// -------------------------------
// Composable UI
// -------------------------------
@Composable
fun MenuItem(menuItem: Item, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Row {
            Text(text = menuItem.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(4.dp))
            BadgeList(menuItem)
        }

        // Icon(
          //  painter = painterResource(R.drawable.favorite_24px),
          //  contentDescription = "Favorite",
           // modifier = Modifier.size(24.dp),
          //  tint = Color.Red
        // )
        //
    }
}

//@Preview
//@Composable
//private fun MenuItemPreview() {
//    Box(modifier = Modifier.background(Color.White)) {
//        MenuItem(sampleMenuItems.get(2))
//    }
//}
