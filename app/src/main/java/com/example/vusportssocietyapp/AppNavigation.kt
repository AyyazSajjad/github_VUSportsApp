package com.example.vusportssocietyapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yourapp.ui.screens.MyRegistrationsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(onLoginSuccess = { role ->
                when (role) {
                    "Participant" -> navController.navigate("participant") {
                        popUpTo("auth") { inclusive = true }
                    }
                    "Coach" -> navController.navigate("coach") {
                        popUpTo("auth") { inclusive = true }
                    }
                    "Admin" -> navController.navigate("admin") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            })
        }
        composable("event_chat/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventChatScreen(eventId = eventId, navController = navController)
        }

        composable("admin_performance") {
            AdminPerformanceScreen(navController)
        }

        composable("coach_performance") {
            CoachPerformanceScreen(navController)
        }

        composable("myRegistrations") {
            MyRegistrationsScreen(navController)
        }



        composable("participant") {
            ParticipantScreen(navController = navController)
        }
        composable("coach") {
            CoachScreen(navController = navController)
        }
        composable("admin") {
            AdminScreen(navController = navController)
        }
    }
}
