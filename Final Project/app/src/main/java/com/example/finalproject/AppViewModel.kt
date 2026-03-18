package com.example.finalproject

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HealthStatus {GOOD, OKAY, BAD}

data class Stand(
    val name: String,
    val cord: LatLng,
    val sitCount: Int,
    val healthStatus: HealthStatus,
    val userName: String
    )

data class User(
        val userName: String,
        val password: String
        )

class AppViewModel : ViewModel() {

    // Each stand is a list, so this is a list of lists
    private val _stands = MutableStateFlow(listOf(
        Stand("Apple Tree", LatLng(43.26755, -86.10963), 5, HealthStatus.OKAY, "nathan"),
        Stand("Swamp", LatLng(43.25169, -86.19497), 3, HealthStatus.GOOD, "nathan"),
        Stand("Hemlock", LatLng(43.25040, -86.00015), 10, HealthStatus.BAD, "nathan"),
        Stand("Pines", LatLng(43.22354, -85.92077), 4, HealthStatus.GOOD, "nathan"),
        Stand("Pines1", LatLng(44.81569, -85.21633), 4, HealthStatus.GOOD, "barber"),
        Stand("Pines2", LatLng(44.80997, -85.20686), 4, HealthStatus.GOOD, "barber")
    ))
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

    var statusMessage by mutableStateOf("")

    // User Handling
    private val _currentUser = MutableStateFlow<String>("")
    val currentUser = _currentUser.asStateFlow()

    // Stand status handling
    private val _status = MutableStateFlow<HealthStatus>(HealthStatus.OKAY)
    val status: StateFlow<HealthStatus> = _status


    // Getting all Stands that associate with User
    val filteredStands: StateFlow<List<Stand>> = combine(stands, currentUser) { standList, user ->
        standList.filter { it.userName == user } }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
        )

    val totalStands: StateFlow<Int> = filteredStands
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalSits: StateFlow<Int> = filteredStands.map { list ->
        list.sumOf { it.sitCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val averageSits: StateFlow<Int> = filteredStands.map { list ->
        list.map { it.sitCount }.average().toInt() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

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

    fun deleteStand(stand: Stand){
        _stands.value = _stands.value.filter { it != stand }
    }

    fun isStandTaken(name: String): Boolean{
        return filteredStands.value.any { it.name.equals(name, ignoreCase = false) }
    }

    // Create Function that checks if name is taken by another stand, if not then create a map
    // of all stands then, if stand.name is the same as the stand to update then create a copy,
    // and change the name
    fun updateStandName(standToUpdate: Stand, newName: String) {
        if (!isStandTaken(newName)) {
            _stands.value = _stands.value.map { stand ->
                if (stand.name == standToUpdate.name) {
                    stand.copy(name = newName)
                } else {
                    stand
                }
            }
        }
    }

    fun addSit(standToUpdate: Stand){
        _stands.value = _stands.value.map { stand ->
            if (stand.name == standToUpdate.name) {
                stand.copy(sitCount = stand.sitCount + 1)
            } else {
                stand
            }
        }
    }
    fun addStand(cord: LatLng, name: String){
        val newStand = Stand(name, cord, 0, HealthStatus.GOOD, _currentUser.value)
        _stands.update { it + newStand }
        usernameInput = ""
    }
}