package com.example.finalproject

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.StandScreen,
        modifier = modifier
    ) {
        composable<Route.StandScreen> {
            StandScreen(
                viewModel = viewModel,
                onNewStand = { navController.navigate(Route.AddStandScreen) }
            )
        }
        // Using a Dialog with a
        composable <Route.AddStandScreen>{
                AddStandScreen(
                    viewModel = viewModel,
                    onAddStand = {navController.navigate(Route.StandScreen)},
                )
        }
        composable <Route.SignUpScreen>{
            SignUpScreen(
                viewModel = viewModel,
                onBack = {navController.popBackStack()}
            )
        }
        composable <Route.LoginScreen>{
            LoginScreen(
                viewModel = viewModel,
                onSignUp = {navController.navigate(Route.SignUpScreen)}
            )
        }
        composable<Route.AllStandsScreen> {
            AllStandsScreen(
                viewModel = viewModel
            )
        }
    }
}