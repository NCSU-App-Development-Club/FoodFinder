package org.appdevncsu.foodfinder.composables


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.viewmodel.LocationListViewModel

@Composable
fun LocationList(
    onLocationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationListViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsState()
    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(locations) { location ->
                LocationItem(
                    location,
                    onLocationClick,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
fun LocationItem(
    location: DiningLocation,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .clickable { onClick(location.unitId) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF),
            contentColor = Color(0xFFFFFFFF)
        )
    ) {
        LocationImage(location.name)

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
fun LocationImage(name: String) {
    val image = when (name) {
        "Case Dining Hall" -> R.drawable.cased
        "Clark Dining Hall" -> R.drawable.clark
        "Fountain Dining Hall" -> R.drawable.fount
        else -> return
    }

    Image(
        painter = painterResource(id = image),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
        contentScale = ContentScale.Crop,
    )
}

@Composable
@Preview
private fun LocationListPreview() {
    LocationList(onLocationClick = {})
}
