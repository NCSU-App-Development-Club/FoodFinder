package org.appdevncsu.foodfinder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.sampleLocations

@Composable
fun LocationList(locations: List<DiningLocation>) {

    LazyColumn (modifier = Modifier.fillMaxSize()){
        items(locations) { location -> LocationItem(location)

        }
    }
/*
    Row {
        Text(text = locations.joinToString(separator = ", "))

    } */
}

@Composable
fun LocationItem(location: DiningLocation) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Card(modifier = Modifier.padding(vertical = 8.dp).size(width = 200.dp, height = 100.dp).clickable{},
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF0000), contentColor = Color(0xFFFFFFFF)))
        {

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = location.name, fontSize = 20.sp)
                Text(text = "Unit ID: ${location.unitId}", fontSize = 20.sp)

            }



        }
    }

}

@Composable
@Preview
fun LocationLisPreview() {
    LocationList(sampleLocations)
}