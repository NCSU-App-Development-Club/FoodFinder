package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.appdevncsu.foodfinder.viewmodel.MenuViewModel

@Composable
fun MenuSectionList(
    menuId: Int,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = viewModel()
) {
    LaunchedEffect(menuId) {
        viewModel.loadMenu(menuId)
    }
    val sections by viewModel.sections.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        sections.forEach { section ->
            item {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    section.items.forEach { menuItem ->
                        MenuItem(menuItem = menuItem)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MenuSectionListPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        MenuSectionList(menuId = 1)
    }
}
