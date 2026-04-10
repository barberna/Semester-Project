package com.example.finalproject

import android.R
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalproject.data.Stand
import com.example.finalproject.ui.theme.HunterOrange

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.LocalDate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.contentColorFor
import com.example.finalproject.ui.theme.LightestGray
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AllStandsScreen(viewModel: AppViewModel) {
    val stands by viewModel.filteredStands.collectAsState()

    val totalSits by viewModel.totalSits.collectAsState()
    val totalStands by viewModel.totalStands.collectAsState()
    val avgSits by viewModel.averageSits.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current


    // Error Handling (Toast + Clear)
    // Listens for Viewmodel flow to change if so then displays the message, this message is set to whatever is triggered in model
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            // Clear Message
            viewModel.clearErrorMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                standOverviewCard("Total Stands", Icons.Default.Place, totalStands, modifier = Modifier.weight(1f))
                standOverviewCard("Total Sits", Icons.Default.Chair, totalSits, modifier = Modifier.weight(1f))
                standOverviewCard("Average Sits", Icons.Default.DataUsage, avgSits, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 12.dp)
            ) {
                items(stands) { stand ->
                    StandsContentCard(viewModel = viewModel, stand,
                        onDelete = { stand -> viewModel.deleteStand(stand) },
                        onChangeName = {stand, name -> viewModel.updateStandName(stand, name)}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandsContentCard(
    viewModel: AppViewModel,
    stand: Stand,
    onDelete: (Stand) -> Unit,
    onChangeName: (Stand, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    var changeStandInput by remember { mutableStateOf(stand.name) }

    val updateStandNameTakenError by viewModel.updateStandNameTakenErrorMessage.collectAsState()

    // Date Picker Logic
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
            .padding(10.dp)

    ) {
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Row{
                Text(
                    text = stand.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
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
            // Location
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Latitude: ${"%.4f".format(stand.cord.latitude)}")
                Text(text = "Longitude: ${"%.4f".format(stand.cord.longitude)}")
            }
            // Health Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Stand Health:")

                Box(modifier = Modifier.weight(1f)) {
                    HealthBar(stand.healthStatus)
                }

                // Allows for dropdown menu to expand
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = HunterOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                    Divider(color = Color.Black)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enter New Name
                        OutlinedTextField(
                            value = changeStandInput,
                            // Handle Name TakenError Here Also
                            onValueChange = { changeStandInput = it
                                if (updateStandNameTakenError != null) viewModel.clearUpdateStandNameTakenError() },
                            isError = updateStandNameTakenError != null,
                            // Error Message for Name Taken
                            supportingText = {
                                updateStandNameTakenError?.let { Text(text = it, color = Color.Red) } },
                            singleLine = true,
                            label = { Text("Enter New Stand Name") },
                            modifier = Modifier
                                .weight(1f),
                            shape = RoundedCornerShape(0),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                cursorColor = HunterOrange,
                                // Add these specifically:
                                errorBorderColor = Color.Red,
                                errorLabelColor = Color.Red,
                                errorCursorColor = Color.Red,
                                selectionColors = TextSelectionColors(
                                    handleColor = HunterOrange, // This changes the "arrows" / bubbles
                                    backgroundColor = HunterOrange.copy(alpha = 0.4f) // The highlight color when text is selected
                                )
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Button to update name
                        Button(
                            onClick = { onChangeName(stand, changeStandInput) },
                            enabled = changeStandInput.isNotBlank() && changeStandInput != stand.name,
                            modifier = Modifier.weight(0.75f).padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(Color.Black, Color.White)
                        ) { Text("Update Name") }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(0.7f),
                            colors = ButtonDefaults.buttonColors(containerColor = HunterOrange, contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                                )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Add Sit")
                        }
                        // Button to delete stand
                        Button(
                            onClick = { onDelete(stand) },
                            modifier = Modifier.weight(0.7f),
                            colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                        ) {
                            Text("Delete", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
    //Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                        viewModel.addSit(stand, selectedDate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor  = HunterOrange, contentColor = Color.White)
                ){ Text("Add Sit") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    )
                {Text("Cancel")}
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun standOverviewCard(text: String, icon: ImageVector, data: Int, modifier: Modifier = Modifier){
    Card(
        modifier = modifier
            .height(125.dp)
            .padding(vertical = 8.dp), // Adds space around it to help it "float"
        shape = RoundedCornerShape(20.dp), // Rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        // Total Stands
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HunterOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = text, style = MaterialTheme.typography.titleSmall, color = Color.Black)
            }
            // Orange Underline
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HunterOrange)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "$data",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
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
fun PreviewStandOverview(){

}