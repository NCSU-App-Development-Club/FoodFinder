package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.viewmodel.MenuViewModel

private const val ExpandedChevronRotationDegrees = 90f

@Composable
fun MenuSectionList(
    menuId: Int,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel()
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
        sections?.sections?.forEach { section ->
            item {
                var expanded by rememberSaveable { mutableStateOf(true) }
                Column(
                    modifier = Modifier.padding(
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
