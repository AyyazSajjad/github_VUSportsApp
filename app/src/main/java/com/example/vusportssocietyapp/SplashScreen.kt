package com.example.vusportssocietyapp
import androidx.compose.ui.platform.LocalContext


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        delay(2000) // Optional: show splash for 2 seconds

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    when (role) {
                        "Participant" -> {
                            navController.navigate("participant") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        "Coach" -> {
                            navController.navigate("coach") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        "Admin" -> {
                            navController.navigate("admin") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        else -> {
                            Toast.makeText(context, "Unknown role", Toast.LENGTH_SHORT).show()
                            navController.navigate("auth") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }


                }
                .addOnFailureListener {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
        } else {
            navController.navigate("auth") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Simple UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VU Sports Society", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
