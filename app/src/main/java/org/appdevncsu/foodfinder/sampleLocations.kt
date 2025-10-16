package org.appdevncsu.foodfinder


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
}


@Composable
fun Top() {
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(.15f).background(Color.Red))
    Column {
        Text(
            text = "Food finder",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
            fontSize = 30.sp
            )
        Text(
            text = "NCSU Dining",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Light

        )
    }
}
@Composable
fun LocationItem(location: DiningLocation) {
   Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Card(modifier = Modifier.padding(vertical = 10.dp).size(width = 350.dp, height = 225.dp).offset(y = 150.dp).clickable{},
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF), contentColor = Color(0xFFFFFFFF)))
        {


            Image(
                painter = painterResource(id = location.imageRes),
                contentDescription = null,
                modifier = Modifier.size(width = 350.dp, height = 170.dp),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.fillMaxSize().padding(vertical = 15.dp, horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {




                Row(Modifier.fillMaxSize()) {
                    Text(text = location.name,
                        modifier = Modifier.width(160.dp),
                        fontSize = 20.sp,
                        color = Color.Black)

                    Box(modifier = Modifier.padding(start = 70.dp).width(60.dp).height(50.dp).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF006400)), contentAlignment = Alignment.Center) {
                        Text(
                            text = "status >"
                        )
                    }
                }


            }



        }
   }

}

@Composable
@Preview
fun LocationLisPreview() {
    Top()
    LocationList(sampleLocations)
}