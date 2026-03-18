package com.example.finalproject

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.location.Location
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animation
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.finalproject.ui.theme.FinalProjectTheme
import com.example.finalproject.ui.theme.HunterOrange
import com.example.finalproject.ui.theme.LightestGray
import com.example.finalproject.ui.theme.grayGreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@Composable
fun StandScreen(modifier: Modifier = Modifier, viewModel: AppViewModel, onNewStand: () -> Unit) {
    val stands by viewModel.filteredStands.collectAsState()

    var selectedStand by remember { mutableStateOf<Stand?>(null) }

    val defaultLocation = LatLng(42.9634, -85.6681)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    var hasInitialSnapPerformed by remember { mutableStateOf(false) }

    LaunchedEffect(stands) {
        val firstStand = stands.firstOrNull()
        if (firstStand != null && !hasInitialSnapPerformed) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(firstStand.cord, 10f)
            hasInitialSnapPerformed = true
        }
    }

    // Allows for Custom Map Theme
    val context = LocalContext.current
    val mapProperties = remember {
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)

        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp), // Adds space around it to help it "float"
                shape = RoundedCornerShape(26.dp), // Rounded corners
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false
                        ),
                        properties = mapProperties,
                        onMapClick = { selectedStand = null }
                    ) {
                        stands.forEach { stand ->
                            Marker(
                                state = rememberMarkerState(position = stand.cord),
                                title = stand.name,
                                onClick = { selectedStand = stand
                                    false }
                            )
                        }
                    }
                    // Clicking on a Pin open shis Surface and when clicked off disappears
                    if (selectedStand == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = Color.Black.copy(0.5f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = HunterOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tap Pin For Details",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
            // If Selected Stands get details from stand and Call StandDetailCard() and add animation
            AnimatedVisibility(
                visible =  selectedStand != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) { selectedStand?.let { stand ->
                    StandDetailCard(stand)
                }
            }
        }
    }
}

@Composable
fun StandDetailCard(stand: Stand) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.8f)),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HunterOrange,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp))
             {
                Text(
                    text = stand.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Latitude: ${"%.4f".format(stand.cord.latitude)}  Longitude: ${
                        "%.4f".format(
                            stand.cord.longitude
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Total Sits:",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    // Visit Counter
                    Box(
                        modifier = Modifier
                            .background(Color.LightGray, shape = RoundedCornerShape(10.dp))
                            .padding(start = 5.dp, end = 5.dp)
                    ){
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${stand.sitCount}", fontSize = 12.sp)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Stand Health:")

                    Box(modifier = Modifier.weight(1f)) {
                        HealthBar(stand.healthStatus)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthBar(status: HealthStatus) {

    val (width, color) = when (status) {
        HealthStatus.GOOD -> 1.0f to Color(0xFF4CAF50) // Green
        HealthStatus.OKAY -> 0.75f to Color(0xFFFFEB3B) // Yellow
        HealthStatus.BAD -> 0.25f to Color(0xFFF44336) // Red
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(LightestGray, shape = RoundedCornerShape(10.dp))
    ){
        Box(
            modifier = Modifier
                .fillMaxWidth(width)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(10.dp))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StandScreenPreview() {

}