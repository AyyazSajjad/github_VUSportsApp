package com.example.vusportssocietyapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavHostController,
    eventId: String?
) {
    var event by remember { mutableStateOf<Event?>(null) }
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var isRegistered by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Fetch event details
    LaunchedEffect(eventId) {
        eventId?.let {
            db.collection("events").document(it)
                .get()
                .addOnSuccessListener { doc ->
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    event = Event(id = doc.id, title = title, description = description)
                }
        }
    }

    // Check if user already registered
    LaunchedEffect(eventId, userId) {
        if (eventId != null && userId != null) {
            db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    isRegistered = !result.isEmpty
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Event Detail") })
        }
    ) { padding ->
        event?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(text = it.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it.description)
                Spacer(modifier = Modifier.height(32.dp))

                if (isRegistered) {
                    Text("✅ You are already registered for this event.")
                } else {
                    val context = LocalContext.current // at the top of your Composable if not already declared

                    Button(onClick = {
                        if (eventId == null) {
                            Toast.makeText(context, "❌ Missing Event ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(context, "❌ User not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val registration = hashMapOf(
                            "userId" to userId, // ✅ Correct UID
                            "eventId" to eventId,
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        FirebaseFirestore.getInstance().collection("registrations")
                            .add(registration)
                            .addOnSuccessListener {
                                Toast.makeText(context, "✅ Registered successfully!", Toast.LENGTH_SHORT).show()
                                isRegistered = true
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "❌ Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    }) {
                        Text("Register")
                    }


                }

                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.primary)
                }
            }
            Button(onClick = {
                navController.navigate("event_chat/${event!!.id}")
            }) {
                Text("Open Chat")
            }


        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
