package com.example.finalproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalproject.ui.theme.HunterOrange
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AllStandsScreen(viewModel: AppViewModel) {
    val stands by viewModel.filteredStands.collectAsState()

    val totalSits by viewModel.totalSits.collectAsState()
    val totalStands by viewModel.totalStands.collectAsState()
    val avgSits by viewModel.totalStands.collectAsState()

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

@Composable
fun StandsContentCard(
    viewModel: AppViewModel,
    stand: Stand,
    onDelete: (Stand) -> Unit,
    onChangeName: (Stand, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    var changeStandInput by remember { mutableStateOf(stand.name) }

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
                        OutlinedTextField(
                            value = changeStandInput,
                            onValueChange = { changeStandInput = it },
                            singleLine = true,
                            label = { Text("Enter New Stand Name") },
                            modifier = Modifier
                                .weight(1f),
                            shape = RoundedCornerShape(0),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                cursorColor = HunterOrange
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onChangeName(stand, changeStandInput) },
                            modifier = Modifier.weight(0.75f).padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(Color.Black, Color.White)
                        ) {
                            Text("Update Name")
                        }
                    }

                    Button(
                        onClick = { onDelete(stand) },
                        modifier = Modifier.fillMaxWidth(0.4f).padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                    ) {
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
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

@Preview(showBackground = true)
@Composable
fun PreviewStandOverview(){

}