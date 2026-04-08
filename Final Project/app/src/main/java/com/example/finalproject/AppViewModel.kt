package com.example.finalproject

import android.app.Application

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.HuntHealthDAO
import com.example.finalproject.data.Sit
import com.example.finalproject.data.Stand
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.sql.Date
import java.time.LocalDate

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
      viewModelScope.launch(Dispatchers.IO) {
          appDAO.getStands().collect { _stands.value = it }
      }
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
    private val _addStandErrorMessage = MutableStateFlow<String?>(null)
    private val _deleteStandErrorMessage = MutableStateFlow<String?>(null)
    private val _updateNameErrorMessage = MutableStateFlow<String?>(null)
    private val _addSitErrorMessage = MutableStateFlow<String?>(null)


    val addStandErrorMessage: StateFlow<String?> = _addStandErrorMessage
    val deleteStandErrorMessage: StateFlow<String?> = _deleteStandErrorMessage
    val updateNameErrorMessage: StateFlow<String?> = _updateNameErrorMessage
    val addSitErrorMessage: StateFlow<String?> = _addSitErrorMessage


    fun clearAddStandError() { _addStandErrorMessage.value = null }
    fun clearDeleteStandError() { _deleteStandErrorMessage.value = null }
    fun clearUpdateNameError() { _updateNameErrorMessage.value = null }
    fun clearAddSitError() { _addSitErrorMessage.value = null }


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

    // Getting total stand count.
    val totalStands: StateFlow<Int> = filteredStands
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Getting total sits for all stands
    val totalSits: StateFlow<Int> = filteredStands.map { list ->
        list.sumOf { it.sitCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Average sits for all stands
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
        // store original list in case of error
        val originalList = _stands.value

        _stands.value = _stands.value.filter { it.id != stand.id }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDAO.removeStand(stand)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalList
                    _deleteStandErrorMessage.value = "DB error, Could not delete stand. Try again Later."
                }
            }
        }
    }

    // Used to chack if stand is taken
    fun isStandTaken(name: String, currentStandId: Int): Boolean{
        return filteredStands.value.any { it.name.equals(name, ignoreCase = false) && it.id  != currentStandId}
    }

    // Create Function that checks if name is taken by another stand, if not then create a map
    // of all stands then, if stand.name is the same as the stand to update then create a copy,
    // and change the name
    fun updateStandName(standToUpdate: Stand, newName: String) {
        if (standToUpdate.name == newName) return

        val originalList = _stands.value

        if (!isStandTaken(newName, standToUpdate.id)) {
            _stands.value = _stands.value.map { stand ->
                if (stand.id == standToUpdate.id) {
                    stand.copy(name = newName)
                } else {
                    stand
                }
            }


            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val updatedStand = standToUpdate.copy(name = newName)
                    // Update Stands Table
                    appDAO.changeStandName(updatedStand)
                    // Update Sits Table
                    appDAO.updateSitRecordName(standToUpdate.id, newName)
                } catch (e: Exception) {
                    // Roll back on the Main Thread if DB fails
                    withContext(Dispatchers.Main) {
                        _stands.value = originalList
                        _updateNameErrorMessage.value = "DB Error, Could not change name, try again later."
                    }
                }
            }
        } else {
            _updateNameErrorMessage.value = "The name $newName is already taken."
        }
    }

    // Add a sit to stand manually
    // !! Need to Add date input for stand data collection to calculate stand health.
    fun addSit(standToUpdate: Stand, date: LocalDate){
        val originalStands = _stands.value

        _stands.value = _stands.value.map { stand ->
            if (stand.id == standToUpdate.id) {
                stand.copy(sitCount = stand.sitCount + 1)
            } else {
                stand
            }
        }

        // Update Sit count for stand in Database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedStand = standToUpdate.copy(sitCount = standToUpdate.sitCount + 1)
                appDAO.addSit(updatedStand)
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalStands
                    _addSitErrorMessage.value = "DB Error, Could not update stand Sit Count."
                }
            }
        }

        // Add Sit record to database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sit = Sit(
                    standId = standToUpdate.id,
                    standName = standToUpdate.name,
                    date = date
                )
                appDAO.addSitRecord(sit)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _stands.value = originalStands
                    _addSitErrorMessage.value = "DB Error, Could not add sit record."
                }
            }
        }

    }
    fun addStand(cord: LatLng, name: String){
        val originalStands = _stands.value

        val newStand = Stand(name = name, cord = cord, sitCount =  0, healthStatus = HealthStatus.GOOD, userName = _currentUser.value)
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
                    _addStandErrorMessage.value = "DB Error, Could not add stand, try again later."
                    _addStandSuccess.value = false
                }
            }
        }
    }
}

