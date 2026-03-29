package com.example.finalproject

import android.Manifest

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.finalproject.data.Stand
import com.example.finalproject.ui.theme.HunterOrange
import com.example.finalproject.ui.theme.LightestGray
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@Composable
fun StandScreen(modifier: Modifier = Modifier, viewModel: AppViewModel, onNewStand: () -> Unit) {
    val stands by viewModel.filteredStands.collectAsState()

    var selectedStand by remember { mutableStateOf<Stand?>(null) }



    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Track permission state to safely enable My Location layer
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Create a Default camera/location in case of no location permission
    val defaultLocation = LatLng(42.9634, -85.6681)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // Location Permission Requester
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Add map Style from JSON data and add it to a val as well as isMylocationEnabled
    // https://mapstyle.withgoogle.com/
    val mapProperties = remember(locationPermissionGranted) {
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, com.example.finalproject.R.raw.map_style),
            isMyLocationEnabled = locationPermissionGranted
        )
    }

    // Track if we have performed initial camera positioning
    var initialPositionSet by remember { mutableStateOf(false) }

    // Camera Positioning Logic: First Stand > Current Location > Default (initial value)
    LaunchedEffect(stands, locationPermissionGranted) {
        if (!initialPositionSet) {
            if (stands.isNotEmpty()) {
                // Priority 1: Focus on the first stand
                cameraPositionState.position = CameraPosition.fromLatLngZoom(stands[0].cord, 13f)
                initialPositionSet = true
            } else if (locationPermissionGranted) {
                // Priority 2: Focus on current location (if no stands yet)
                getCurrentLocation(fusedLocationClient) { location ->
                    if (!initialPositionSet) { // Double check in case stands loaded during async call
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 13f)
                        initialPositionSet = true
                    }
                }
            }
        }
    }

    // Launch permission request on start if not already granted
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
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