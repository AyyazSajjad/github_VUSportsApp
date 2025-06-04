package com.example.vusportssocietyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vusportssocietyapp.ui.theme.VUSportsSocietyAppTheme
import com.yourapp.ui.screens.MyRegistrationsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VUSportsSocietyAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) { SplashScreen(navController) }

        composable(Routes.Auth) {
            AuthScreen(onLoginSuccess = { role ->
                when (role) {
                    "Participant" -> navController.navigate(Routes.Participant) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                    "Coach" -> navController.navigate(Routes.Coach) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                    "Admin" -> navController.navigate(Routes.Admin) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                }
            })
        }
        composable("chat/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            if (eventId != null) {
                EventChatScreen(eventId = eventId, navController = navController)
            }
        }
        composable("chat/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventChatScreen(eventId = eventId, navController = navController)
        }




        composable(Routes.Leaderboard) {
            LeaderboardScreen(navController)
        }

        composable(Routes.CoachPerformance) {
            CoachPerformanceScreen(navController)
        }


        composable(Routes.EventDetail) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EventDetailScreen(navController, eventId)
        }
        composable(Routes.AdminPerformance) {
            AdminPerformanceScreen(navController)
        }


        composable("myRegistrations") {
            MyRegistrationsScreen(navController)
        }

        composable(Routes.Participant) {
            ParticipantDashboardScreen(navController)
        }

        composable("my_registrations") {
            MyRegistrationsScreen(navController)
        }

        composable(Routes.Coach) {
            CoachScreen(navController)
        }

        composable(Routes.Admin) {
            AdminScreen(navController)

        }

        // ✅ NEW: View registrations for a specific event
        composable("eventRegistrations/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            if (eventId != null) {
                EventRegistrationsScreen(eventId = eventId, navController = navController)
            }
        }
    }
}
