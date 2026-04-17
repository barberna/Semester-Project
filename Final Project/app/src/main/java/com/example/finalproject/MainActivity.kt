package com.example.finalproject

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import com.example.finalproject.ui.theme.FinalProjectTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.finalproject.ui.theme.HunterOrange
import com.example.finalproject.ui.theme.grayGreen
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Add Map initializer Here
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) {
            Log.d("MapsInit", "Google Maps Renderer initialized: $it")
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            FinalProjectTheme {
                // The Root Box holds the camo image
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_1), // Replace with your camo drawable
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f
                    )

                    // The Scaffold sits on top but is TRANSPARENT
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent, // This kills the white background
                        topBar = { NavBar() },
                        bottomBar = { BottomNavigationBar(navController) }
                    ) { innerPadding ->
                        // 3. The NavHost content
                        AppNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination
    Surface(
        modifier = Modifier
            .padding(bottom = 30.dp, start = 20.dp, end = 20.dp)
            .fillMaxWidth()
            .height(75.dp),
        shape = CircleShape,
        color = grayGreen.copy(),
        shadowElevation = 12.dp,
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0,0,0,0),
        ) {
            NavigationBarItem(
                selected = currentRoute?.hasRoute<Route.StandScreen>() == true,
                onClick = { navController.navigate(Route.StandScreen) },
                label = { Text(text = "Home", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = HunterOrange,
                    unselectedTextColor = Color.Black,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.Black,
                    indicatorColor = HunterOrange
                )
            )
            NavigationBarItem(
                selected = currentRoute?.hasRoute<Route.AllStandsScreen>() == true,
                onClick = { navController.navigate(Route.AllStandsScreen) },
                label = { Text(text = "Stands", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))},
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = HunterOrange,
                    unselectedTextColor = Color.Black,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.Black,
                    indicatorColor = HunterOrange
                )
            )
            NavigationBarItem(
                selected = currentRoute?.hasRoute<Route.AddStandScreen>() == true,
                onClick = { navController.navigate(Route.AddStandScreen) },
                label = { Text(text = "Add Stand", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = HunterOrange,
                    unselectedTextColor = Color.Black,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.Black,
                    indicatorColor = HunterOrange
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBar() {
    CenterAlignedTopAppBar(
        colors = TopAppBarColors(
            containerColor = grayGreen,
            scrolledContainerColor = grayGreen,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.Black,
            actionIconContentColor = Color.Black
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = HunterOrange
                )
                Text(
                    text = "Hunt Health",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// This use to be in the Topbar, but no longer have logout button
// This will be reimplemented later when Firebase Auth is added and setting page is added.
@Composable
fun Menu(viewModel: AppViewModel, onLogout: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(50.dp)) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(40.dp)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = (-8).dp, y = 0.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    showMenu = false
                    onLogout()
                    viewModel.Logout()
                },
                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
            )
        }
    }
}


