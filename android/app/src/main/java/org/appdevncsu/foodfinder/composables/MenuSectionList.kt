package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Item
import org.appdevncsu.foodfinder.data.Section
import org.appdevncsu.foodfinder.data.SectionList
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import org.appdevncsu.foodfinder.viewmodel.MenuViewModel

private const val ExpandedChevronRotationDegrees = 90f

private const val MenuSectionListSkeletonCount = 3
private const val MenuSectionListSkeletonItemsPerSection = 3
private const val SkeletonSectionTitleBarWidthFraction = 0.5f
private const val SkeletonItemNameBarWidthFraction = 0.65f
private val SkeletonSectionTitleBarHeight = 28.dp
private val SkeletonItemNameBarHeight = 24.dp
private val SkeletonItemBadgeBarWidth = 72.dp
private val SkeletonItemBadgeBarHeight = 18.dp

@Composable
fun MenuSectionList(
    menuId: Int,
    locationId: Int,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel()
) {
    LaunchedEffect(menuId, locationId) {
        viewModel.loadMenu(menuId, locationId)
    }
    val state by viewModel.uiState.collectAsState()

    MenuSectionListContent(
        state = state,
        onRetry = { viewModel.retry(menuId, locationId) },
        modifier = modifier,
    )
}

@Composable
internal fun MenuSectionListContent(
    state: MenuViewModel.UiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val sections = state.sections
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .consumeBottomNavBarInsets(),
        contentPadding = bottomNavBarContentPadding(),
    ) {
        if (state.error != null && sections == null) {
            item {
                ErrorState(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
            return@LazyColumn
        }
        if (sections == null) {
            items(MenuSectionListSkeletonCount) {
                SkeletonMenuSection()
            }
            return@LazyColumn
        }

        sections.sections.forEach { section ->
            item {
                ExpandableMenuSection(section = section)
            }
        }
    }
}

@Composable
private fun ExpandableMenuSection(section: Section, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier.padding(
            top = 16.dp,
            bottom = if (expanded) 16.dp else 4.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.name,
                style = MaterialTheme.typography.titleLarge
            )
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                modifier = Modifier.rotate(
                    if (expanded) ExpandedChevronRotationDegrees else 0f
                )
            )
        }

        if (expanded) {
            section.items.forEach { menuItem ->
                MenuItem(menuItem = menuItem)
            }
        }
    }
}

@Composable
private fun SkeletonMenuSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBar(
            modifier = Modifier
                .fillMaxWidth(SkeletonSectionTitleBarWidthFraction)
                .height(SkeletonSectionTitleBarHeight)
        )
        repeat(MenuSectionListSkeletonItemsPerSection) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonBar(
                    modifier = Modifier
                        .fillMaxWidth(SkeletonItemNameBarWidthFraction)
                        .height(SkeletonItemNameBarHeight)
                )
                SkeletonBar(
                    modifier = Modifier.size(
                        SkeletonItemBadgeBarWidth,
                        SkeletonItemBadgeBarHeight
                    )
                )
            }
        }
    }
}

private val SampleSections = SectionList(
    sections = listOf(
        Section(
            name = "Entrees",
            id = 1,
            items = listOf(
                Item(
                    name = "Grilled Chicken Sandwich",
                    id = 1,
                    sectionId = 1,
                    flags = listOf("Wolf Approved", "Contains Gluten", "Contains Dairy")
                ),
                Item(
                    name = "Black Bean Burger",
                    id = 2,
                    sectionId = 1,
                    flags = listOf("Wolf Approved", "Vegetarian", "Vegan")
                ),
            )
        ),
        Section(
            name = "Sides",
            id = 2,
            items = listOf(
                Item(
                    name = "Seasoned Fries",
                    id = 3,
                    sectionId = 2,
                    flags = listOf("Vegetarian")
                ),
            )
        ),
    )
)

@Composable
@Preview(showBackground = true)
private fun MenuSectionListPreview() {
    FoodFinderTheme {
        MenuSectionListContent(state = MenuViewModel.UiState(sections = SampleSections))
    }
}

@Composable
@Preview(showBackground = true)
private fun MenuSectionListLoadingPreview() {
    FoodFinderTheme {
        MenuSectionListContent(state = MenuViewModel.UiState(loading = true))
    }
}

@Composable
@Preview(showBackground = true)
private fun MenuSectionListErrorPreview() {
    FoodFinderTheme {
        MenuSectionListContent(
            state = MenuViewModel.UiState(error = stringResource(R.string.error_server)),
        )
    }
}
