# Hunt Health Tutorial
### By: Nathan Barber

**Hunt Health** is a modern Android application designed for hunters to manage their hunting stands and track "sit" frequency. By monitoring how often a stand is visited, the app helps hunters maintain "stand health" to avoid over-hunting specific locations.

## Overview
- **Platform Focus Areas**: Hunt Health focuses on Location and Context APIs while also implementing Google Maps SDK.
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
   git https://github.com/barberna/Semester-Project.git
   ```
2. Open the project in **Android Studio Jellyfish** or newer.
3. Add your Google Maps API Key to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```
4. Add a location to emulator by going into extended controls
5. Build and run the app on an emulator or physical device.
6. Ensure to change app location settings to "Allow all the time" for app

## DemoVideo
https://1drv.ms/v/c/076f35e41e36c567/IQBC1QPJ3JEuRLLQxQlHFWYVAdz23rwg7VcEgfCOzyoykpk?e=7NOzM2

## Presentation Slides
https://youtu.be/2bKjatSm8b8

## Tutorial
### 📍 Location and Context APIs
The app leverages **Google Play Services Location** to handle high-accuracy tracking and background geofencing.

**Key Implementation Details:**
- **Requesting Appropriate Permissions**: We need appropriate user location permission as well as certain location settings turn on for the full functionality of the app. These include:
  - ACCESS_FINE_LOCATION
  - ACCESS_COARSE_LOCATION
  - ACCESS_BACKGROUND_LOCATION
  - Google Location Accuracy
- **Fused Location Provider**: Used to fetch the user's precise location. The app employs a "Fast Fix" strategy: it checks for the `lastLocation` for speed, and if null (common on fresh installs), it forces a fresh fix using `getCurrentLocation` with `PRIORITY_HIGH_ACCURACY`.
- **Background Geofencing**: The app registers `Geofence` objects with the OS for every hunting stand. This allows the system to monitor location transitions even when the app is stopped or the device is restarted.
- **Geofence Broadcast Receiver**: When a user enters a 75-meter radius of a stand, the OS triggers a `BroadcastReceiver` that executes database logic to record a "sit" and recalculate stand health.


**Requesting Appropriate Permissions**: These are required to be accepted in order to use the users location in the app. 
- *Basic Setup*:
  - First we create a value in our view model that is able to keep track of our current permission status:
```kotlin
    var locationPermissionGranted by mutableStateOf(false)
```
  - In our UI we need to set a value to our current android context, this will allow us to keep track of what our current android system is.
````kotlin
    val context = LocalContext.current
````
 - In order to use Fused `Location Provider`, `Background Geofencing`, and the `Geofence Broadcast Receiver` you need these stated in your manifest,
 
```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
- Add this to your app level Gradle:
```kts
 // Location Dep
implementation(libs.play.services.location) 
```

***Implementation/Creating Permission Requests***
- **Creating LocationPermissionLauncher**: These are created in the UI
  - We need to be able to hold the launcher in a value in order to remember permission registration
  - In modern android we have to collect permission for background location in a separate request. Here is foreground and background launchers:
```kotlin
// StandScreen.kt
 // launcher specifically for Background Permission
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { Log.d("Permissions", "Background Location (All the time) granted") }
    }

    // Location Permission Requester
    // Foreground Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        // CHAIN: If foreground is granted, immediately ask for background (API 29+)
        if (viewModel.locationPermissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
```
- ***Creating LaunchEffects***: These are created in the UI, as they need to be able to handle the results of permission as soon as the activity is created
  - We only need one LaunchEffect for these permissions which is executed each time the app dies, or there is a change to its parameters. In this case Unit will not trigger the Launch.
  - We need to have to launch instances of background permission
    - One, when app is opened for first time and all permissions are requested, this will fire in LocationPermissionLauncher.
    - Two, when a user does not get prompted, or they declined "Allow all the time", they will get the popup again when they open up the app.
```kotlin
// StandScreen.kt
// Launch permission request on start if not already granted
    LaunchedEffect(Unit) {
        // Get current status on permissions as a bool and set it to viewModel.locationPermissionGranted
        viewModel.performOnStartChecks(context)

        if (!viewModel.locationPermissionGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        // Check to see what Android they are running, older versions do not need background in two requests
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // If background permission is not granted, which is requested
            if ( ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }
```
***Location Accuracy***: We need location Accuracy turned on in setting for more precise GPS locating, this will use a combo of Wi-Fi, Cell Towers, and Bluetooth to "pin" your location much more precisely
  - This is needed to ensure geofence encounters are accurate.
  - We use this function in viewmodel to check current permission context as well as check on each launch if this setting it turn on:
```kotlin
// AppViewModel.kt
// Location functions to clean up UI Logic
    fun performOnStartChecks(context: Context) {
        // Check current Location status
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Trigger Google Location Accuracy popup if needed
        checkLocationSettings(context)
    }
```
 - If this is not turned on, then it will be give a popup to allow. Here is the code for this with comment breakdown:
 ```kotlin
// AppViewModel.kt
// Checks if the user has 'Location Accuracy' on in location settings on device
    fun checkLocationSettings(context: Context) {
        // Define the type of location tracking we need (High Accuracy, 1-second updates)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        // Build a request to check if the device's CURRENT settings can handle that request
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // Get the Settings Client which acts as the "checker" for Google Play Services
        val client: SettingsClient = LocationServices.getSettingsClient(context)

        // Start the task to check settings
        val task = client.checkLocationSettings(builder.build())

        // If settings are NOT correct (e.g., GPS is off), this listener triggers
        task.addOnFailureListener { exception ->
            // Check if the error is "Resolvable" (meaning we can show a popup to fix it)
            if (exception is com.google.android.gms.common.api.ResolvableApiException) {
                try {
                    // Try to cast the context to an Activity so we can host the popup dialog
                    val activity = context as? Activity

                    // FIX: Call startResolutionForResult on the cast exception.
                    // This triggers the "Google Location Accuracy" system dialog.
                    // 12345 is just a request code to identify this result later.
                    exception.startResolutionForResult(activity!!, 12345)

                } catch (sendEx: IntentSender.SendIntentException) {
                    // If the intent failed to send, we ignore it
                    Log.e("Settings", "Error opening location settings dialog", sendEx)
                }
            }
        }
    }
```

***Focused Location Provider***
 - This is the primary entry point for the Google Play Services loaction APIs. 
 - This acts as a bridge between our app and Google Play Services.
 - Here is how I make an instance of this:
```kotlin
// StandScreen.kt
val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
```
 - We then use this instant of the LocationServices to call a fetchLocation function.
 - This function takes in the fusedLocationClient and expects to return a LatLong object through a parameter
 - We first check if there is a last location, if not then we request a freash location.
 - Here is the code:
```kotlin
// Located in StandScreen.kt
fun fetchLocation(
  fusedLocationClient: FusedLocationProviderClient,
  onLocationRetrieved: (LatLng) -> Unit
) {
  // 1. Attempt to get the Last Known Location
  fusedLocationClient.lastLocation.addOnSuccessListener { location ->
    if (location != null) {
      // Success: Use the cached location
      onLocationRetrieved(LatLng(location.latitude, location.longitude))
    } else {
      // If lastLocation is null, request a "Fresh" location
      // This is common on emulators or if GPS was recently off.
      Log.d("Location", "Last location null, requesting fresh location...")

      fusedLocationClient.getCurrentLocation(
        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
        null // Use null for no cancellation token
      ).addOnSuccessListener { freshLocation ->
        freshLocation?.let {
          onLocationRetrieved(LatLng(it.latitude, it.longitude))
        }
      }
    }
  }
}
```
- **UI Implementation**
  - We then call this function in a LaunchEffect in AddSTandScreen to focus the map on the users current location when adding a stand.
  - It is also used in StandScreen.kt(home screen), if there are no stands added, then it focused the home screen map to your current location. 
  - Here is the LaunchEffect:
```kotlin
// StandsScreen.kt
// Camera Positioning Logic: First Stand > Current Location > Default (initial value)
    // Also has listener for added stands and location permission settings, if changed so will map location
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

### Background Geofencing

***Setting up Geofences***: We create a function in our viewModel that will hold the logic for setting up the geofences.
   - We need to take in app context and the list of stands to so we can use their coordinates.
   - In the function we create an instance of the GooglePlay location services Geofencing Client
 
```kotlin
// AppViewModel.kt
 val geofencingClient =  LocationServices.getGeofencingClient(context)
```

**Geofence List Setup**
   - We create an instance of the geofence list by creating a new map of our stands list, and then calling `Geofence.Builder()`.
     - We set the request ID to the stands unique identifier
     - we use each stand coordinates and a set radius for each fence
     - We tell it to never let fence expire
     - In my instance I want mine to only trigger when entered
```kotlin
// AppViewModel.kt
val geofenceList = stands.map { stand ->
            Geofence.Builder()
                .setRequestId(stand.id.toString())
                .setCircularRegion(stand.cord.latitude, stand.cord.longitude, 75f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // This tells when to trigger when it comes to the geofence
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
```

**Creating Intent/Request**
  - We then create an intent and a request:
    - We create a request to tell Google Play Services, where to watch(stands) and when to care(Only on enter of 75m of stand fence)
      - We set initial trigger to 0, so that it does not trigger a 'Sit' when you are already in the stand, or there is GPS gitter
```kotlin
// AppViewModel.kt
val request = GeofencingRequest.Builder()
            // setInitialTrigger(0) ensures it won't trigger a sit
            // just because you opened the app while already sitting at the stand.
            .setInitialTrigger(0)
            .addGeofences(geofenceList)
            .build()
```
  - We create an intent so that Google Play Services knows who to alert when request conditions are met
    - Here we need to add our GeofenceBroadcastReceiver, this is where the Location services will alert
```kotlin
// AppViewModel.kt
// getBroadcast: Tells the system that when a geofence is triggered, it should send a Broadcast.
        // GeofenceBroadcastReceiver::class.java: This is the specific class that will "wake up" to handle the event.
        // FLAG_UPDATE_CURRENT: If geofences are already registered, this tells the system to just update the existing intent rather than creating a brand new one.
        // FLAG_MUTABLE: Crucial for Android 12+. Since Google Play Services needs to add location data (the transition details) into the intent before sending it to you, the intent must be "mutable" (changeable).
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
```

**Calling Client**
    - This is then sent through our geofencingClient instance and called in a try catch request to ensure any success or failure is logged for debugging.

```kotlin
// AppViewModel.kt
try {
     // Call the method directly on the client
     geofencingClient.addGeofences(request, intent).run {
         addOnSuccessListener {
             Log.d("Geofence", "Successfully registered ${stands.size} geofences")
         }
         addOnFailureListener { e ->
             Log.e("Geofence", "Failed to register geofences: ${e.message}")
         }
     }
 } catch (e: SecurityException) {
     Log.e("Geofence", "Permission missing for geofencing", e)
 }
```

**Syncing Geofences in UI**: On fresh app start, new stand addition, or a change in permissions we need these geofences to sync in the UI
  - Here we create a function that takes current UI context, and current app stands, and calls our fun `checkLocationSettings`
  - We then put this in a LaunchEffect in our StandScreen.kt where the content it is effecting is located. Ensuring they have the most accurate geofencing
    - Though this is not effecting a UI change directly, in the StandScreen composable is where we need to get the current context(LocalContext.current)
  
```kotlin
// viewModel
fun syncGeofencing(context: Context) {
    val currentStands = _stands.value
    if (locationPermissionGranted && currentStands.isNotEmpty()) {
        setupGeofencing(context, currentStands)
    }
}
```
```kotlin
// StandScreen.kt
// Register geofencing by watching stands list and permissions
// Also check if user has Location Accuracy on in their settings
LaunchedEffect(stands, viewModel.locationPermissionGranted) {
    viewModel.syncGeofencing(context)
}
```


### Geofence Broadcast Receiver
***BroadcastReceiver***: This is made in its own file called: `GeofenceBroadcastReceiver.kt`
  - This allows us to make changes to our database by using functions from our viewModel
  - In HuntHealth we only want to add a Sit record if We are crossing into our Geofence. We then want to wrap our work in an 'if statement'.
```kotlin
// This ONLY triggers when crossing the boundary from OUTSIDE to INSIDE
if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {}
```

**Database**
- A database instance is required to successfully use the database
```kotlin
val app = context.applicationContext as HuntHealth
val appDAO = app.appDB.getInstance()
```


**onReceive Calls**
- The geofencing event is then called on any stand that has a geofence event(possibly multiple at once if there is a stand overlap)
  - In this call we then want to run all of our database calls in a CoroutineScope on a separate thread. Why?
    - To have all database operations off the main thread
    - All suspend function need to be called in suspend functions or CoroutineScope. 
    - The onReceive only allows for short operation time, this ensures enough time to complete DB operations
```kotlin
CoroutineScope(Dispatchers.IO).launch {
    try{
        // Get last sit from stand, call from DB
        val lastSit = appDAO.getLatestSitForStand(standId)
        val now = LocalDateTime.now()

        // TIME-GATE: Only add a sit if:
        // - No sit exists yet OR The last sit was more than 3 hours ago, set to 2 min for testing
        // This allows Morning/Evening hunts but ignores GPS jitter (the "bounce")
        val isNewVisit = lastSit == null || lastSit.date.isBefore(now.minusMinutes(2))

        if (isNewVisit) {
            val stand = appDAO.getStandId(standId) ?: return@launch

            // Create the sit record
            val sit = Sit(
                standId = standId,
                standName = stand.name,
                date = now
            )

            // Add sit record to DB
            appDAO.addSitRecord(sit)

            val tenDaysAgo = LocalDateTime.now().minusDays(10)
            val count = appDAO.getStandSitCount(standId, tenDaysAgo)
            val newHealthStatus = AppViewModel.determineHealthStatus(count)
            val updatedStand = stand.copy(
                sitCount = stand.sitCount + 1,
                healthStatus = newHealthStatus
            )

            appDAO.updateStand(updatedStand)
            Log.d("Geofence", "Recorded visit for ${stand.name}")
        } else {
            Log.d("Geofence", "GPS Jitter or user still at stand $standId. Skipping.")
        }
        // Get stand from DB

    } catch (e: Exception) {
        Log.e("Geofence", "Unable to add sit record to Sits table", e)
    }
}
    
```

***Manifest Implementation***: Telling Android that we have a Broadcast Receiver
  - The tag `<reciver>` is designed to listen for specific events, in this case geofencing
    - android:name=".GeofenceBroadcastReceiver": This points to your actual Kotlin class. The . means it is in the root package (com.example.finalproject). If the name here doesn't match your file name exactly, the app will crash or ignore the geofence. 
    - android:enabled="true": This tells the system that it is allowed to create an instance of this class. If set to false, the receiver is disabled and will never fire. 
    - android:exported="true": This is critical. It allows processes outside the app (specifically Google Play Services) to send intents to this receiver. If this were false, only the app could trigger the receiver, which would break geofencing.
    - The tag `<intent-filter>` r acts as a "security guard" or a "sorting machine." It tells Android: "I don't want to hear about every single broadcast in the system; only wake me up if the message matches this specific type
```xml
<!-- THE GEOFENCE RECEIVER (Must have the correct name) -->
        <receiver
            android:name=".GeofenceBroadcastReceiver"
            android:enabled="true"
            android:exported="true"
            tools:ignore="ExportedReceiver">
            <intent-filter>
                <action android:name="com.google.android.gms.location.GeofenceStore.ACTION_GEOFENCE_EVENT" />
            </intent-filter>
        </receiver>
```


### 🗺️ Maps SDK
Hunt Health utilizes the **Google Maps SDK for Android** with the **Maps Compose** library to provide an interactive, state-driven mapping experience.

***Setup Details:***
**Google Cloud**: You need to create a Google Cloud Consol account
  - You need to set up a billing account, in order to use Maps SDK
  - You need to enable the Google Maps API
  - You need to create a new project in Google Cloud Consol
**API Key**: You are going to need an API key in order for your app to talk to the Google Maps API
  - It is important that you use this property in your android project, as you do not want people to use see your key. 
  - In your Android Manifest you will have your API Key and then you will add your key in local.properties set to the name in the {}:

```properties
    android:value="${MAPS_API_KEY}"
```

**Setting up Google Maps SDK**: Here is a link to Goolges documentation for Setup https://developers.google.com/maps/documentation/android-sdk/get-api-key?hl=en
  - Here it will have in depth details on how:
    - To set up account and Billing
    - Enable the SDK in your Google Cloud project
    - Configure your API Key
    - What dependencies to add, what play services versions to use, what libraries to add, and how to use your API KEY

***Key Implementation Details:***

**State Management**: Uses `CameraPositionState` to programmatically pan and zoom the map based on the user's location or the location of their hunting stands.
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

**Custom Styling**: The map uses a custom JSON style (loaded from `raw/map_style.json`) to provide a high-contrast, Earthy feel to go with the app theme.
- Link Google Maps API map style: https://mapstyle.withgoogle.com/
- This is set in variable and used as a parameter in the Google Map composable, properties

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

### 📝 Further Discussion & Conclusions
***Summary of Implementation***
The Hunt Health project demonstrates a sophisticated integration of the Google Play Services Location and Maps APIs within a modern Jetpack Compose architecture. By utilizing the Fused Location Provider, the app achieves high-accuracy positioning with optimized battery consumption. The implementation of Geofencing allows the application to move beyond a passive map viewer into a contextually aware assistant, automatically recording data based on the user's physical presence in the field.

- A key technical takeaway from this project is the strict separation of concerns:
    - The UI (Compose) handles the visual state and camera animations.
    - The ViewModel manages the business logic and geofence synchronization. 
    - The BroadcastReceiver & Room handle persistent data integrity even when the app process is terminated.

***Alternative Approaches***
- While the native Google Play Services approach used here is the industry standard for Android, developers might consider several alternatives depending on their requirements:
1. **Mapbox SDK**: An alternative to Google Maps that offers highly customizable vector maps and offline capabilities. While Google Maps is easier to integrate with Play Services, Mapbox is often preferred for specialized terrain rendering common in outdoor/hunting apps.
2. **WorkManager**: For more complex background tasks (e.g., syncing hunt data to a cloud server after a geofence trigger), developers might use WorkManager in instead of a simple CoroutineScope inside the BroadcastReceiver. WorkManager is more resilient to system-initiated process kills.
3. **Third-Party Location Trackers**: Libraries like Radar or Geofencing by Radius provide simplified wrappers around the native APIs. These can be useful for cross-platform development but often introduce unnecessary dependencies for native-focused projects.

***Related Features***
- To further enhance a project like Hunt Health, the following platform features are recommended for study:
  - **Activity Recognition API**: This Google API can detect if a user is walking, running, or stationary. Combining this with Geofencing would allow the app to be even smarter—for example, only recording a "Sit" if the user is "Stationary" within the geofence for more than 15 minutes.
  - **Health Connect**: As this app focuses on "Hunt Health," integrating with the Android Health Connect API would allow the app to pull heart rate or step data during a hunt to correlate physical exertion with hunting success.
  - **Weather API**: Adding a weather API that adds to the health formula calculation for each stand.
  - **Snap-to-Roads/Terrain API**: For users navigating deep woods, using the Google Maps Roads API or Elevation API could provide contextual data regarding the altitude or slope of a specific stand location.

### See Also

**Google Maps SDK**
- Google Maps Styling
    - https://mapstyle.withgoogle.com/
- Google Maps SDK Documantation
  - https://developers.google.com/maps/documentation/android-sdk/get-api-key?hl=en
  - https://www.youtube.com/watch?v=pOKPQ8rYe6g

**Geofence**
- Android Documentation
  - https://developer.android.com/develop/sensors-and-location/location/geofencing
- YouTube Tutorial
    - https://www.youtube.com/watch?v=nmAtMqljH9M

