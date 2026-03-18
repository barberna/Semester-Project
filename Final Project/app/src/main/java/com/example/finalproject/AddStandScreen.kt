package com.example.finalproject

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Paint
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.finalproject.ui.theme.HunterOrange
import com.example.finalproject.ui.theme.LightestGray
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.ktx.model.cameraPosition

@Composable
fun AddStandScreen(
    viewModel: AppViewModel,
    onAddStand: () -> Unit
) {

    // Getting Current Context from Android to use for location permission
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Create a Default camera/location in case of no location permission
    val defaultLocation = LatLng(42.9634, -85.6681)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // Location Permission Requester
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation(fusedLocationClient) { location ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 5f)
            }
        }
    }

    val mapProperties = remember {
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, com.example.finalproject.R.raw.map_style),
            isMyLocationEnabled = true
        )
    }

    // Launch permission request on start
    LaunchedEffect(Unit) {
        // Check if we ALREADY have permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            // We already have it! Just get the location.
            getCurrentLocation(fusedLocationClient) { location ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 13f)
            }
        } else {
            // We don't have it, so ask for it.
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Card(
                    modifier = Modifier
                        .height(450.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)

                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                        )

                        {
                            Marker(
                                state = MarkerState(position = cameraPositionState.position.target),
                                title = "New Stand",
                                draggable = true,
                            )
                        }
                        // Attention to move marker
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = Color.Black.copy(0.5f),
                            shape = CircleShape
                        ) {
                            Row(modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = HunterOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Move the pin to the location of your new stand",
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Pin Coordinates: ${"%.5f".format(cameraPositionState.position.target.latitude)} ${
                        "%.5f".format(
                            cameraPositionState.position.target.longitude
                        )
                    }",
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 5.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.usernameInput,
                        onValueChange = { viewModel.usernameInput = it },
                        label = { Text("Enter Stand Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Black,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                            focusedContainerColor = Color.White,
                            focusedBorderColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            if (viewModel.isStandTaken(viewModel.usernameInput)) {
                                Toast.makeText(context, "Stand Name Taken!", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                viewModel.addStand(
                                    cameraPositionState.position.target,
                                    viewModel.usernameInput
                                )
                                onAddStand()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            HunterOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(0.75f)
                    ) { Text("Add Stand") }
                }
            }
        }
}

@SuppressLint("MissingPermission") // We handle this in the UI with the Launcher
private fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationRetrieved: (LatLng) -> Unit
) {
    // This 'location' refers to the Android Location object
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            val userLatLng = LatLng(location.latitude, location.longitude)
            onLocationRetrieved(userLatLng)
        }
    }
        .addOnFailureListener { e ->
            // If it fails, we just don't move the camera (no crash!)
            Log.e("MapsError", "Location fetch failed: ${e.message}")
        }
}

