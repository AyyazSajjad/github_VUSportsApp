package com.example.vusportssocietyapp

import android.util.Log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Event(val id: String, val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantDashboardScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val events = remember { mutableStateListOf<Event>() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("events").get()
            .addOnSuccessListener { result ->
                events.clear()
                for (doc in result) {
                    val title = doc.getString("title") ?: "No Title"
                    val desc = doc.getString("description") ?: "No Description"
                    events.add(Event(doc.id, title, desc))
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                error = "Error fetching events: ${e.message}"
                Log.e("Firestore", e.message ?: "Unknown error")
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Participant Dashboard") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("auth") {
                            popUpTo("participant") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else {
                Button(
                    onClick = { navController.navigate("myRegistrations") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("My Registrations")
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = event.description)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row {
                                    Button(
                                        onClick = {
                                            navController.navigate("eventDetail/${event.id}")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("View Details")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            navController.navigate("chat/${event.id}")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Chat")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
