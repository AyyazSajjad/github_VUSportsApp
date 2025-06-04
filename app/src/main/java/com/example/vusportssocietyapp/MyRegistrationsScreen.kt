package com.yourapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRegistrationsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var registeredEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            error = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("registrations")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { regSnapshot ->
                val eventIds = regSnapshot.documents.mapNotNull { it.getString("eventId") }.filter { it.isNotBlank() }

                if (eventIds.isEmpty()) {
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Firestore whereIn supports max 10 items
                val chunks = eventIds.chunked(10)
                val tempList = mutableListOf<Event>()
                var completed = 0

                chunks.forEach { chunk ->
                    db.collection("events")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .addOnSuccessListener { eventSnapshot ->
                            val events = eventSnapshot.documents.mapNotNull { doc ->
                                val id = doc.id
                                val title = doc.getString("title") ?: ""
                                val description = doc.getString("description") ?: ""
                                val date = doc.getString("date") ?: ""
                                val location = doc.getString("location") ?: ""
                                Log.d("FirestoreDebug", "Fetched event: title=$title, date=$date, location=$location")


                                Event(id = id, title = title, description = description, date = date, location = location)
                            }


                            tempList.addAll(events)
                            completed++
                            if (completed == chunks.size) {
                                registeredEvents = tempList
                                isLoading = false
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Failed to load events: ${e.message}")
                            Toast.makeText(context, "Failed to load events", Toast.LENGTH_SHORT).show()
                            error = "Failed to load events"
                            isLoading = false
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading registrations", Toast.LENGTH_SHORT).show()
                error = "Error loading registrations"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Registrations") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                registeredEvents.isEmpty() -> {
                    Text(
                        text = "You have not registered for any events.",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(registeredEvents) { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                                    Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Date: ${event.date}")
                                    Text(text = "Location: ${event.location}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
