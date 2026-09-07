package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.LocationListItem
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.viewmodel.LocationListViewModel

private const val LocationImageAspectRatio = 16f / 9f
private const val LocationSkeletonCount = 4
private val SkeletonGap = 4.dp
private val SkeletonPillShape = RoundedCornerShape(percent = 50)
private const val SkeletonPrimaryWidthFraction = 0.75f
private val NameTextLineHeight = 24.sp
private val StatusTextLineHeight = 18.sp
private val SkeletonNameBarWidth = 180.dp
private val SkeletonNameBarHeight = 24.dp
private val SkeletonStatusBarHeight = 18.dp
private val BadgeSkeletonWidth = 64.dp
private val BadgeSkeletonHeight = 30.dp

@Composable
fun LocationList(
    onLocationClick: (Location) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LocationListContent(state, onLocationClick, modifier)
}

@Composable
private fun LocationListContent(
    state: LocationListViewModel.UiState,
    onLocationClick: (Location) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (state.loading) {
            items(LocationSkeletonCount) {
                SkeletonLocationItem(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
        items(state.items, key = { it.location.id }) { item ->
            LocationItem(
                item.location,
                item.status,
                onLocationClick,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun LocationItem(
    location: Location,
    status: LocationStatus?,
    onClick: (Location) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .clickable { onClick(location) },
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

        LocationInfoRow(location.name, status)
    }
}

@Composable
private fun LocationInfoRow(name: String, status: LocationStatus?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 20.sp,
                lineHeight = NameTextLineHeight,
                color = Color.Black
            )
            LocationStatusText(status)
        }

        if (status == null) {
            SkeletonBar(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(BadgeSkeletonWidth, BadgeSkeletonHeight),
                shape = SkeletonPillShape,
            )
        } else {
            LocationStatusBadge(status, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun LocationStatusText(status: LocationStatus?, modifier: Modifier = Modifier) {
    if (status == null) {
        SkeletonBar(
            modifier = modifier
                .height(statusLineHeight())
                .fillMaxWidth(SkeletonPrimaryWidthFraction)
        )
    } else {
        Text(
            text = status.rawText,
            fontSize = 14.sp,
            lineHeight = StatusTextLineHeight,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
            .clip(SkeletonPillShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            lineHeight = StatusTextLineHeight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SkeletonLocationItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.White
        )
    ) {
        SkeletonBar(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(LocationImageAspectRatio, matchHeightConstraintsFirst = true),
            shape = RectangleShape,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBar(modifier = Modifier.size(SkeletonNameBarWidth, SkeletonNameBarHeight))
                Spacer(modifier = Modifier.height(SkeletonGap))
                SkeletonBar(
                    modifier = Modifier
                        .height(SkeletonStatusBarHeight)
                        .fillMaxWidth(SkeletonPrimaryWidthFraction)
                )
            }
            SkeletonBar(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(BadgeSkeletonWidth, BadgeSkeletonHeight),
                shape = SkeletonPillShape,
            )
        }
    }
}

@Composable
private fun statusLineHeight(): Dp {
    return with(LocalDensity.current) { StatusTextLineHeight.toDp() }
}

private val SampleLocation = Location(
    name = "Fountain Dining Hall",
    id = 1,
    slug = "fountain",
    type = "dining-halls",
    imageUrl = "/api/locations/fountain/image",
)

private val PreviewImageColor = Color(0xFF66BB6A)

// Coil consults this handler only in the preview environment, so real loads are unaffected.
@OptIn(ExperimentalCoilApi::class)
@Composable
private fun WithPreviewImages(content: @Composable () -> Unit) {
    val previewHandler = AsyncImagePreviewHandler { ColorImage(PreviewImageColor.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        content()
    }
}

@Composable
@Preview(showBackground = true)
private fun LocationListLoadingPreview() {
    LocationListContent(LocationListViewModel.UiState(loading = true), onLocationClick = {})
}

@Composable
@Preview(showBackground = true)
private fun LocationListHoursLoadingPreview() {
    WithPreviewImages {
        LocationListContent(
            LocationListViewModel.UiState(
                items = listOf(
                    LocationListItem(SampleLocation, status = null),
                    LocationListItem(
                        SampleLocation.copy(
                            id = 2,
                            name = "Clark Dining Hall",
                            slug = "clark",
                            imageUrl = "/api/locations/clark/image",
                        ),
                        status = null,
                    ),
                )
            ),
            onLocationClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LocationItemHoursLoadingPreview() {
    WithPreviewImages {
        LocationItem(
            SampleLocation,
            status = null,
            onClick = {},
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LocationItemHoursLoadedPreview() {
    WithPreviewImages {
        LocationItem(
            SampleLocation,
            status = LocationStatus.Open("7:00am - 9:00pm"),
            onClick = {},
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
