# Hunt Health Tutorial

**Hunt Health** is a modern Android application designed for hunters to manage their hunting stands and track "sit" frequency. By monitoring how often a stand is visited, the app helps hunters maintain "stand health" to avoid over-hunting specific locations.

## Overview
- **Platform Focus Areas**: Hunt Health focuses on the Android topics Maps SDK and Location and Context APIs.
- **On Device Storage**: Hunt Health uses Room SQLite for on device storage to ensure even when there is no internet connection, the app still functions.
  - Keeping storage on device better suites the apps real world application. Hunting often comes with lack of internet connection, keeping app functional.
  - Keeping the storge on device allows to keep the cost down to maintain the application
    

## 🚀 Features

- **Interactive Map**: View all your hunting stands on a custom-styled Google Map.
- **Stand Management**: Add, update, and delete stands with custom names and precise coordinates.
- **Automatic Sit Tracking (Geofencing)**: Automatically records a "sit" when you enter a 75-meter radius of your stand.
- **Stand Health Algorithm**: Calculates the health of each stand (GOOD, OKAY, BAD) based on the number of sits in the last 10 days.
- **Offline First**: Uses Room Database for persistent storage, ensuring your data is available even in remote hunting locations.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Database**: [Room SQLite](https://developer.android.com/training/data-storage/room)
- **Architecture**: MVVM (Model-View-ViewModel)
- **APIs**:
    - Google Maps API for Android
    - Google Play Services Location (Fused Location & Geofencing)
- **Navigation**: Type-safe Compose Navigation

## 📦 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hunt-health.git
   ```
2. Open the project in **Android Studio Jellyfish** or newer.
3. Add your Google Maps API Key to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```
4. Build and run the app on an emulator or physical device.

### 🗺️ Maps SDK
Hunt Health utilizes the **Google Maps SDK for Android** with the **Maps Compose** library to provide an interactive, state-driven mapping experience.

**Setup Details:**
- **Google Cloud**: You need to create a Google Cloud Consol account
  - You need to set up a billing account, in order to use Maps SDK
  - You need to enable the Google Maps API
  - You need to create a new project in Google Cloud Consol
- **API Key**: You are going to need an API key in order for your app to talk to the Google Maps API
  - It is important that you use this property in your android project, as you do not want people to use see your key. 
  - In your Android Manifest you will have your API Key like so and then you will add your key in local.properties set to the nae in the {}:

```properties
    android:value="${MAPS_API_KEY}"
```

- **Setting up Google Maps SDK**: Here is a link to Goolges documentation for Setup https://developers.google.com/maps/documentation/android-sdk/get-api-key?hl=en
  - Here it will have in depth details on how:
    - To set up account and Billing
    - Enable the SDK in your Google Cloud project
    - Configure your API Key
    - What dependencies to add, what play services versions to use, what libraries to add, and how to use your API KEY

**Key Implementation Details:**
- **Custom Styling**: The map uses a custom JSON style (loaded from `raw/map_style.json`) to provide a high-contrast, Earthy feel to go with the app theme.
  - Link Google Maps API map style: https://mapstyle.withgoogle.com/ 
  - This is set in variable and used as a parameter in the Google Map composable, properties
- **State Management**: Uses `CameraPositionState` to programmatically pan and zoom the map based on the user's location or the location of their hunting stands.
  - In Hunt Health the idea is to have the users stands as the current map view. This LaunchEffect, watches the stands data class, and our location permissions
    - This checks if we have the initial map position set, if not then we check our first priority, stands
    - If there are no stands then we want to give the user a map location that is familiar, their location.
    - If we do not have location permissions from the user then we use a default location so the map can work. 
  - When the app is destroyed this Launch effect is ran to set initial camera state of the map.
  - It is important that you have a defult/backup location in case 
  
```kotlin
LaunchedEffect(stands, viewModel.locationPermissionGranted) {
        if (!initialPositionSet) {
            if (stands.isNotEmpty()) {
                // Priority 1: Focus on the first stand
                cameraPositionState.position = CameraPosition.fromLatLngZoom(stands[0].cord, 11f)
                initialPositionSet = true
            } else if (viewModel.locationPermissionGranted) {
                // Priority 2: Focus on current location (if no stands yet)
                fetchLocation(fusedLocationClient) { location ->
                    if (!initialPositionSet) { // Double check in case stands loaded during async call
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 13f)
                        initialPositionSet = true
                    }
                }
            }
        }
    }
```

- **Dynamic Markers**: Stand locations are rendered as interactive markers that, when tapped, trigger a Compose `AnimatedVisibility` card showing stand-specific details.
  - 

```kotlin
// Map implementation in StandScreen.kt
GoogleMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState,
    properties = MapProperties(
        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style),
        isMyLocationEnabled = locationPermissionGranted
    ),
    uiSettings = MapUiSettings(rotationGesturesEnabled = false, tiltGesturesEnabled = false)
) {
    stands.forEach { stand ->
        Marker(
            state = rememberMarkerState(position = stand.cord),
            title = stand.name,
            onClick = { selectedStand = stand; false }
        )
    }
}

// https://mapstyle.withgoogle.com/
val mapProperties = remember(viewModel.locationPermissionGranted) {
    MapProperties(
        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, com.example.finalproject.R.raw.map_style),
        isMyLocationEnabled = viewModel.locationPermissionGranted
    )
}
```

### 📍 Location and Context APIs
The app leverages **Google Play Services Location** to handle high-accuracy tracking and background geofencing.

**Key Implementation Details:**
- **Fused Location Provider**: Used to fetch the user's precise location. The app employs a "Fast Fix" strategy: it checks for the `lastLocation` for speed, and if null (common on fresh installs), it forces a fresh fix using `getCurrentLocation` with `PRIORITY_HIGH_ACCURACY`.
- **Background Geofencing**: The app registers `Geofence` objects with the OS for every hunting stand. This allows the system to monitor location transitions even when the app is stopped or the device is restarted.
- **Geofence Broadcast Receiver**: When a user enters a 100-meter radius of a stand, the OS triggers a `BroadcastReceiver` that executes database logic to record a "sit" and recalculate stand health.

```kotlin
// Geofence registration in AppViewModel.kt
val geofence = Geofence.Builder()
    .setRequestId(stand.id.toString())
    .setCircularRegion(stand.cord.latitude, stand.cord.longitude, 75f)
    .setExpirationDuration(Geofence.NEVER_EXPIRE)
    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
    .build()

val request = GeofencingRequest.Builder()
    .setInitialTrigger(0)
    .addGeofences(listOf(geofence))
    .build()

geofencingClient.addGeofences(request, geofencePendingIntent)
```

## Coding Instructions


## 📸 Screenshots

*(Add screenshots of your app here after uploading them to a `screenshots` folder in your repo)*


