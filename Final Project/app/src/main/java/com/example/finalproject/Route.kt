package com.example.finalproject

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object StandScreen

    @Serializable
    data object AddStandScreen
    @Serializable
    data object LoginScreen

    @Serializable
    data object AllStandsScreen
    @Serializable
    data object SignUpScreen
}
