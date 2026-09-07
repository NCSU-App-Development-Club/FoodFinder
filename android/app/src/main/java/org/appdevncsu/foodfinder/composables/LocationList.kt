package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.viewmodel.LocationListViewModel

private const val LocationImageAspectRatio = 16f / 9f

@Composable
fun LocationList(
    onLocationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationListViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsState()
    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(locations) { item ->
                LocationItem(
                    item.location,
                    item.status,
                    onLocationClick,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
fun LocationItem(
    location: Location,
    status: LocationStatus,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .clickable { onClick(location.id) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.White
        )
    ) {
        location.absoluteImageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = location.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(LocationImageAspectRatio, matchHeightConstraintsFirst = true),
                contentScale = ContentScale.Crop,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Text(
                    text = status.rawText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LocationStatusBadge(status, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun LocationStatusBadge(status: LocationStatus, modifier: Modifier = Modifier) {
    val pill = when (status) {
        is LocationStatus.Open -> Triple(Color.Blue, Color.White, "Open")
        is LocationStatus.ClosingSoon -> Triple(Color.Yellow, Color.Black, "Closing")
        is LocationStatus.Closed -> Triple(Color.Red, Color.White, "Closed")
        is LocationStatus.Unavailable -> null
    }
    if (pill == null) return
    val (background, contentColor, text) = pill
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
private fun LocationListPreview() {
    LocationList(onLocationClick = {})
}
