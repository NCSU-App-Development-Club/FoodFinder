package org.appdevncsu.foodfinder

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import org.appdevncsu.foodfinder.composables.LocationListContent
import org.appdevncsu.foodfinder.composables.MenuListContent
import org.appdevncsu.foodfinder.composables.MenuSectionListContent
import org.appdevncsu.foodfinder.composables.ScreenScaffold
import org.appdevncsu.foodfinder.data.Item
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.LocationListItem
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.data.Menu
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.data.Section
import org.appdevncsu.foodfinder.data.SectionList
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import org.appdevncsu.foodfinder.viewmodel.LocationListViewModel
import org.appdevncsu.foodfinder.viewmodel.MenuListViewModel
import org.appdevncsu.foodfinder.viewmodel.MenuViewModel
import org.junit.Rule
import org.junit.Test

/**
 * This class renders the screenshots shown in the README.
 *
 * Run `./scripts/update-readme-screenshots.sh` to generate these
 * screenshots, wrap them in a device frame, and move them to the
 * right place.
 */
class ReadmeScreenshotsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun homeLight() {
        paparazzi.snapshot {
            ReadmeFrame(title = "FoodFinder", darkTheme = false) {
                LocationListContent(
                    state = LocationListViewModel.UiState(
                        loading = false,
                        hoursLoading = false,
                        items = sampleLocations,
                    ),
                    onLocationClick = {},
                )
            }
        }
    }

    @Test
    fun homeDark() {
        paparazzi.snapshot {
            ReadmeFrame(title = "FoodFinder", darkTheme = true) {
                LocationListContent(
                    state = LocationListViewModel.UiState(
                        loading = false,
                        hoursLoading = false,
                        items = sampleLocations,
                    ),
                    onLocationClick = {},
                )
            }
        }
    }

    @Test
    fun menuListLight() {
        paparazzi.snapshot {
            ReadmeFrame(title = "Fountain Dining Hall", onBack = {}, darkTheme = false) {
                MenuListContent(
                    state = MenuListViewModel.UiState(
                        loading = false,
                        menuList = sampleMenus,
                    ),
                    navController = rememberNavController(),
                )
            }
        }
    }

    @Test
    fun menuListDark() {
        paparazzi.snapshot {
            ReadmeFrame(title = "Fountain Dining Hall", onBack = {}, darkTheme = true) {
                MenuListContent(
                    state = MenuListViewModel.UiState(
                        loading = false,
                        menuList = sampleMenus,
                    ),
                    navController = rememberNavController(),
                )
            }
        }
    }

    @Test
    fun menuLight() {
        paparazzi.snapshot {
            ReadmeFrame(title = "Lunch", subtitle = "Monday, August 25", onBack = {}, darkTheme = false) {
                MenuSectionListContent(
                    state = MenuViewModel.UiState(
                        loading = false,
                        sections = sampleSections,
                    ),
                )
            }
        }
    }

    @Test
    fun menuDark() {
        paparazzi.snapshot {
            ReadmeFrame(title = "Lunch", subtitle = "Monday, August 25", onBack = {}, darkTheme = true) {
                MenuSectionListContent(
                    state = MenuViewModel.UiState(
                        loading = false,
                        sections = sampleSections,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun ReadmeFrame(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    FoodFinderTheme(darkTheme = darkTheme, dynamicColor = false) {
        val fallback = MaterialTheme.colorScheme.primaryContainer
        val previewHandler = AsyncImagePreviewHandler { request ->
            val url = request.data.toString()
            val drawableRes = when {
                "fountain" in url -> R.drawable.fount
                "clark" in url -> R.drawable.clark
                "case" in url -> R.drawable.cased
                else -> null
            }
            val bitmap = drawableRes?.let {
                BitmapFactory.decodeResource(request.context.resources, it)
            }
            bitmap?.asImage() ?: ColorImage(fallback.toArgb())
        }
        CompositionLocalProvider(
            LocalInspectionMode provides true,
            LocalAsyncImagePreviewHandler provides previewHandler,
        ) {
            Column {
                ReadmeStatusBar()
                ScreenScaffold(title = title, subtitle = subtitle, onBack = onBack) {
                    content()
                }
            }
        }
    }
}

/** Fake status bar so that the screenshots fit the device frame better */
@Composable
private fun ReadmeStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 36.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "9:30",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        StatusBarIcons(color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatusBarIcons(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(modifier = Modifier.size(16.dp, 12.dp)) {
            val barWidth = size.width / 7f
            listOf(0.35f, 0.55f, 0.8f, 1f).forEachIndexed { index, fraction ->
                val barHeight = size.height * fraction
                drawRect(
                    color = color,
                    topLeft = Offset(x = index * barWidth * 1.75f, y = size.height - barHeight),
                    size = Size(width = barWidth, height = barHeight),
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.signal_wifi_4_bar_24px),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Icon(
            painter = painterResource(R.drawable.battery_full_24px),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
    }
}

// Fixed sample data on fixed dates — never LocalDate.now() — so re-recording
// only changes pixels when UI code actually changes.
private val sampleLocations = listOf(
    LocationListItem(
        Location(
            name = "Fountain Dining Hall",
            id = 1,
            slug = "fountain",
            type = "dining-halls",
            imageUrl = "/api/locations/fountain/image",
        ),
        status = LocationStatus.Open("7:00am - 9:00pm"),
    ),
    LocationListItem(
        Location(
            name = "Clark Dining Hall",
            id = 2,
            slug = "clark",
            type = "dining-halls",
            imageUrl = "/api/locations/clark/image",
        ),
        status = LocationStatus.ClosingSoon("7:00am - 8:00pm"),
    ),
    LocationListItem(
        Location(
            name = "Case Dining Hall",
            id = 3,
            slug = "case",
            type = "dining-halls",
            imageUrl = "/api/locations/case/image",
        ),
        status = LocationStatus.Closed("9:00am - 7:00pm"),
    ),
)

private val sampleMenus = MenuList(
    menus = listOf(
        Menu(name = "Breakfast", id = 1, date = "2025-08-25", locationId = 1),
        Menu(name = "Lunch", id = 2, date = "2025-08-25", locationId = 1),
        Menu(name = "Dinner", id = 3, date = "2025-08-26", locationId = 1),
    ),
)

private val sampleSections = SectionList(
    sections = listOf(
        Section(
            name = "Entrees",
            id = 1,
            items = listOf(
                Item(
                    name = "Grilled Chicken Sandwich",
                    id = 1,
                    sectionId = 1,
                    flags = listOf("Wolf Approved", "Contains Gluten", "Contains Dairy"),
                ),
                Item(
                    name = "Black Bean Burger",
                    id = 2,
                    sectionId = 1,
                    flags = listOf("Wolf Approved", "Vegetarian", "Vegan"),
                ),
            ),
        ),
        Section(
            name = "Sides",
            id = 2,
            items = listOf(
                Item(
                    name = "Seasoned Fries",
                    id = 3,
                    sectionId = 2,
                    flags = listOf("Vegetarian"),
                ),
            ),
        ),
    ),
)
