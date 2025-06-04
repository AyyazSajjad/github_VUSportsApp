package com.example.vusportssocietyapp

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// Data class for event
data class AdminEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Form state variables
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<String?>(null) }

    // UI dialog state
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEventForAction by remember { mutableStateOf<AdminEvent?>(null) }

    val eventList = remember { mutableStateListOf<AdminEvent>() }

    // Load events
    LaunchedEffect(Unit) {
        db.collection("events").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            eventList.clear()
            for (doc in snapshot.documents) {
                val event = AdminEvent(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    location = doc.getString("location") ?: "",
                    date = doc.getString("date") ?: ""
                )
                eventList.add(event)
            }
        }
    }

    // Date Picker
    val calendar = Calendar.getInstance()
    val showDatePicker = {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                selectedDate = "$year-${month + 1}-$day"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo("admin") { inclusive = true }
                    }
                    showLogoutDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Confirmation Dialog
    if (showEditDialog && selectedEventForAction != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Confirm Edit") },
            text = { Text("Are you sure you want to edit this event?") },
            confirmButton = {
                TextButton(onClick = {
                    val event = selectedEventForAction!!
                    title = event.title
                    description = event.description
                    location = event.location
                    selectedDate = event.date
                    editingEventId = event.id
                    isEditMode = true
                    showEditDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedEventForAction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("events").document(selectedEventForAction!!.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "🗑️ Event deleted", Toast.LENGTH_SHORT).show()
                        }
                    showDeleteDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Form UI
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker() }) {
                OutlinedTextField(value = selectedDate, onValueChange = {}, label = { Text("Date") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker() }) {}
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank() && location.isNotBlank() && selectedDate.isNotBlank()) {
                        val event = hashMapOf("title" to title, "description" to description, "location" to location, "date" to selectedDate)
                        if (isEditMode && editingEventId != null) {
                            db.collection("events").document(editingEventId!!).set(event).addOnSuccessListener {
                                Toast.makeText(context, "✅ Event updated", Toast.LENGTH_SHORT).show()
                                isEditMode = false
                                editingEventId = null
                            }
                        } else {
                            db.collection("events").add(event).addOnSuccessListener {
                                Toast.makeText(context, "✅ Event added", Toast.LENGTH_SHORT).show()
                            }
                        }
                        title = ""; description = ""; location = ""; selectedDate = ""
                    } else {
                        Toast.makeText(context, "⚠️ Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditMode) "Update Event" else "Save Event")
            }

            Divider()

            Text("All Events", style = MaterialTheme.typography.titleLarge)
            LazyColumn {
                items(eventList) { event ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text("📍 ${event.location}")
                            Text("📅 ${event.date}")
                            Text(event.description)
                            Button(onClick = {
                                navController.navigate("chat/${event.id}")
                            }) {
                                Text("Chat")
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = {
                                    selectedEventForAction = event
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    selectedEventForAction = event
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }

                            Button(
                                onClick = {
                                    navController.navigate("eventRegistrations/${event.id}")
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("View Registrations")
                            }
                            Button(onClick = {
                                navController.navigate("admin_performance") // ✅ This is what you asked for
                            },modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("View Performance Records")
                            }
                            Button(onClick = {
                                navController.navigate("leaderboard")
                            },modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("View Leaderboard")
                            }

                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRegistrationsScreen(eventId: String, navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventId) {
        db.collection("registrations")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { regSnapshot ->
                val userIds = regSnapshot.mapNotNull { it.getString("userId") }

                if (userIds.isEmpty()) {
                    participants = listOf("No participants registered.")
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Fetch user documents one by one based on document ID
                val fetchTasks = userIds.map { userId ->
                    db.collection("users").document(userId).get()
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(
                    fetchTasks
                )
                    .addOnSuccessListener { userDocs ->
                        participants = userDocs.map { it.getString("name") ?: "Unknown User" }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        error = "Failed to load participant names."
                        isLoading = false
                    }
            }
            .addOnFailureListener {
                error = "Failed to load registrations."
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Registrations") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text("❌ $error", color = MaterialTheme.colorScheme.error)
                }

                participants.isEmpty() -> {
                    Text("No participants registered.")
                }

                else -> {
                    LazyColumn {
                        items(participants) { name ->
                            Text("👤 $name", modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
