package org.appdevncsu.foodfinder.composables


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.sampleLocations

@Composable
fun LocationList(locations: List<DiningLocation>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(locations) { location ->
                LocationItem(location, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun LocationItem(location: DiningLocation, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .clickable {},
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF),
            contentColor = Color(0xFFFFFFFF)
        )
    ) {
        Image(
            painter = painterResource(id = location.imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
            contentScale = ContentScale.Crop,
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 15.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = location.name,
                modifier = Modifier.fillMaxWidth(0.8f),
                fontSize = 20.sp,
                color = Color.Black
            )

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF006400)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "status >"
                )
            }
        }
    }
}

@Composable
@Preview
private fun LocationListPreview() {
    LocationList(sampleLocations)
}
