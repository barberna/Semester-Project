package com.example.finalproject

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.HuntHealthDAO
import com.example.finalproject.data.Sit
import com.example.finalproject.data.Stand
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime

enum class HealthStatus {GOOD, OKAY, BAD}

data class User(
        val userName: String,
        val password: String
        )

// To Implement
    // Add DB Insert Call plus error handling

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // Get the database instance from your Application class
    // Access the DAO specifically (assuming you named the function 'huntHealthDao()')
    private val appDAO: HuntHealthDAO = (application as HuntHealth).appDB.getInstance()

    init {
        // Get all stands from database for UI use on startup
        viewModelScope.launch(Dispatchers.IO) {
              appDAO.getStands().collect { _stands.value = it }
        }

        //  Update Stands Health on Startup due to date changing daily
        updateStandHealth()
    }

    // Each stand is a list, so this is a list of lists
    private val _stands = MutableStateFlow<List<Stand>>(emptyList())
    val stands = _stands.asStateFlow()

    // Store List all of all users
    private val _registeredUsers = MutableStateFlow(listOf(
        User("nathan", "test")
    ))

    // User Input Variables
    var usernameInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var passwordInputConfirm by mutableStateOf("")

    // Status handling variables
    var isLoading by mutableStateOf(false)
    var loginError by mutableStateOf(false)
    var isLoginSuccessful by mutableStateOf(false)

    // DB Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    fun clearErrorMessage() { _errorMessage.value = null }


    // Handles DB Insert Success before navigation back to stand screen
    private val _addStandSuccess = MutableStateFlow(false)
    val addStandSuccess = _addStandSuccess.asStateFlow()
    fun clearAddStandSuccess() {
        _addStandSuccess.value = false
    }

    // Error Handling ViewModel
    private val _updateStandNameTakenErrorMessage = MutableStateFlow<String?>(null)
    val updateStandNameTakenErrorMessage: StateFlow<String?> = _updateStandNameTakenErrorMessage
    fun clearUpdateStandNameTakenError() {
        _updateStandNameTakenErrorMessage.value = null
    }

    var statusMessage by mutableStateOf("")

    // User Handling
    private val _currentUser = MutableStateFlow<String>("")
    val currentUser = _currentUser.asStateFlow()

    // Stand status handling
    private val _healthStatus = MutableStateFlow<HealthStatus>(HealthStatus.GOOD)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus


    // Getting all Stands that associate with User
    // This is no longer needed as we are now using on device only
    val filteredStands: StateFlow<List<Stand>> = combine(stands, currentUser) { standList, user ->
        standList.filter { TODO()  } }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
        )

    // Getting total stand count.
    val totalStands: StateFlow<Int> = _stands
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Getting total sits for all stands
    val totalSits: StateFlow<Int> = _stands.map { list ->
        list.sumOf { it.sitCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Average sits for all stands
val averageSits: StateFlow<Int> = _stands.map { list ->
        list.map { it.sitCount }.average().toInt() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // User Login/Signup
    // No longer need this
    // May be future use for Google Auth
    fun Login() {
        viewModelScope.launch {
            isLoading = true
            loginError = false
            delay(3000)
        }

        val matchedUser = _registeredUsers.value.find { user ->
            user.userName == usernameInput && user.password == passwordInput}

        if (matchedUser != null) {
            isLoginSuccessful = true
            _currentUser.value = usernameInput

        } else {
            loginError = true
        }
        isLoading = false
        usernameInput = ""
        passwordInput = ""
        statusMessage = ""

    }

    fun Logout() {
        _currentUser.value = ""
        isLoginSuccessful = false
    }

    fun createAccount(name: String, pass: String) {
        val newUser = User(name, pass)
        _registeredUsers.update { current -> current + newUser }
    }

    fun checkTaken(name: String): Boolean {
        return _registeredUsers.value.any {it.userName == name}
    }

    fun SignUp(name: String, pass: String, confirmPass: String) {
        if (checkTaken(name)) {
            statusMessage = "Username Taken"
        } else if (pass != confirmPass) {
            statusMessage = "Passwords Do Not Match"
        } else {
            createAccount(name, pass)
            statusMessage = "Account Successfully Created"
        }
        usernameInput = ""
        passwordInput = ""
        passwordInputConfirm = ""
    }
    // User Login/Signup

    fun deleteStand(stand: Stand){
        // store original list in case of error
        val originalList = _stands.value

        // UI Update call
        _stands.value = _stands.value.filter { it.id != stand.id }

        // Database Update call
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDAO.removeStand(stand)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalList
                    _errorMessage.value = "Unable to delete stand"
                    Log.e("Database", "Unable to delete stand", e)
                }
            }
        }
    }

    // Used to chack if stand is taken
    fun isStandTaken(name: String, currentStandId: Int): Boolean{
        return _stands.value.any { it.name.equals(name, ignoreCase = false) && it.id  != currentStandId}
    }

    // Create Function that checks if name is taken by another stand, if not then create a map
    // of all stands then, if stand.name is the same as the stand to update then create a copy,
    // and change the name
    fun updateStandName(standToUpdate: Stand, newName: String) {
        if (standToUpdate.name == newName) return

        val originalList = _stands.value

        if (!isStandTaken(newName, standToUpdate.id)) {
            // UI Update
            _stands.value = _stands.value.map { stand ->
                if (stand.id == standToUpdate.id) {
                    stand.copy(name = newName)
                } else {
                    stand
                }
            }

            // Database Call
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val updatedStand = standToUpdate.copy(name = newName)
                    // Update Stands Table
                    appDAO.updateStand(updatedStand)
                    // Update Sits Table
                    appDAO.updateSitRecordName(standToUpdate.id, newName)
                } catch (e: Exception) {
                    // Roll back on the Main Thread if DB fails
                    withContext(Dispatchers.Main) {
                        _stands.value = originalList
                        _errorMessage.value = "Unable to update name"
                        Log.e("Database", "Unable to change stand name", e)
                    }
                }
            }
        } else {
            _errorMessage.value = "$newName is already used."
            _updateStandNameTakenErrorMessage.value = "Name in use!"
        }
    }

    // Add a sit to stand manually
    // !! Need to Add date input for stand data collection to calculate stand health.
    fun addSit(standToUpdate: Stand, date: LocalDateTime){
        val originalStands = _stands.value

        // Optimistic UI, update UI First
        _stands.value = _stands.value.map { stand ->
            if (stand.id == standToUpdate.id) {
                val newSitCount = stand.sitCount + 1
                // When added Sit, make a new copy
                stand.copy(sitCount = newSitCount)
            } else {
                stand
            }
        }

        // Add Sit record to database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add new sit record to DB table Sit
                val sit = Sit(
                    standId = standToUpdate.id,
                    standName = standToUpdate.name,
                    date = date
                )
                appDAO.addSitRecord(sit)

                // Update both stand sit count/Health when new sit is added
                val tenDaysAgo = LocalDateTime.now().minusDays(10)
                val sitsInLastTenDays = appDAO.getStandSitCount(standToUpdate.id, tenDaysAgo)
                val newHealthStatus = determineHealthStatus(sitsInLastTenDays)

                val updatedStand = standToUpdate.copy(
                    sitCount = standToUpdate.sitCount + 1,
                    healthStatus = newHealthStatus
                )

                appDAO.updateStand(updatedStand)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalStands
                    _errorMessage.value = "Unable to add sit."
                    Log.e("Database", "Unable to add sit record to Sits table", e)
                }
            }
        }

    }
    fun addStand(cord: LatLng, name: String){
        val originalStands = _stands.value

        val newStand = Stand(name = name, cord = cord, sitCount =  0, healthStatus = HealthStatus.GOOD)
        _stands.update { it + newStand }
        usernameInput = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDAO.addStand(newStand)

                withContext(Dispatchers.Main) {
                    _addStandSuccess.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalStands
                    _errorMessage.value = "Unable to add stand."
                    _addStandSuccess.value = false
                    Log.e("Database", "Unable to add stand to Stands", e)
                }
            }
        }
    }

    // Updates Stand health by querying each stands sit count based on stand.id and Current Date - 10 Days
    fun updateStandHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tenDaysAgo = LocalDateTime.now().minusDays(10)
                val currentStands = appDAO.getStands().first()

                currentStands.forEach { stand ->
                    val count = appDAO.getStandSitCount(stand.id, tenDaysAgo)
                    val newHealthStatus = determineHealthStatus(count)

                    if (stand.healthStatus != newHealthStatus) {
                        val updatedStand = stand.copy(healthStatus = newHealthStatus)
                        appDAO.updateStand(updatedStand)
                    }
                }
            } catch (e: Exception) {
                Log.e("Viewmodel", "Unable to update HealthStatus", e)
            }
        }
    }

    companion object{
        fun determineHealthStatus(sitCount: Int): HealthStatus {
            return when (sitCount) {
                in 0..3 -> HealthStatus.GOOD
                in 4..6 -> HealthStatus.OKAY
                else -> HealthStatus.BAD
            }
        }
    }


    // Location Logic
    var locationPermissionGranted by mutableStateOf(false)
    fun setupGeofencing(context: Context, stands:List<Stand>){
        if (stands.isEmpty()) return

        val geofencingClient =  LocationServices.getGeofencingClient(context)
        val geofenceList = stands.map { stand ->
            Geofence.Builder()
                .setRequestId(stand.id.toString())
                .setCircularRegion(stand.cord.latitude, stand.cord.longitude, 75f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // This tells when to trigger when it comes to the geofence
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }



        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val request = GeofencingRequest.Builder()
            // setInitialTrigger(0) ensures it won't trigger a sit
            // just because you opened the app while already sitting at the stand.
            .setInitialTrigger(0)
            .addGeofences(geofenceList)
            .build()

         try {
             // 2. Call the method directly on the client
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
    }

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

    // Location functions to clean up UI Logic
    fun performOnStartChecks(context: Context) {
        // Check current Location status
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Trigger Google Location Accuracy popup if needed
        checkLocationSettings(context)
    }

    fun syncGeofencing(context: Context) {
        val currentStands = _stands.value
        if (locationPermissionGranted && currentStands.isNotEmpty()) {
            setupGeofencing(context, currentStands)
        }
    }
}

