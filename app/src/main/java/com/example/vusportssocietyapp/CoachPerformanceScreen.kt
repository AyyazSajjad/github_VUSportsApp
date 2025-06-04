package com.example.vusportssocietyapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachPerformanceScreen(
    navController: NavController,
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    var selectedEventId by remember { mutableStateOf("") }
    var selectedParticipantId by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var eventList by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var participantList by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    var eventExpanded by remember { mutableStateOf(false) }
    var participantExpanded by remember { mutableStateOf(false) }

    // Load events and participants
    LaunchedEffect(true) {
        db.collection("events").get().addOnSuccessListener { snapshot ->
            eventList = snapshot.documents.map {
                it.id to (it.getString("title") ?: "Unnamed Event")
            }
        }
        db.collection("users")
            .whereEqualTo("role", "Participant")
            .get()
            .addOnSuccessListener { snapshot ->
                participantList = snapshot.documents.map {
                    it.id to (it.getString("name") ?: it.getString("fullName") ?: "Unnamed")
                }

            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach: Enter Performance") },
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
            Text("Enter Performance Data", style = MaterialTheme.typography.titleLarge)

            // Event Dropdown
            ExposedDropdownMenuBox(
                expanded = eventExpanded,
                onExpandedChange = { eventExpanded = !eventExpanded }
            ) {
                TextField(
                    value = eventList.find { it.first == selectedEventId }?.second ?: "Select Event",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventExpanded)
                    },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = eventExpanded,
                    onDismissRequest = { eventExpanded = false }
                ) {
                    eventList.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedEventId = id
                                eventExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Participant Dropdown
            ExposedDropdownMenuBox(
                expanded = participantExpanded,
                onExpandedChange = { participantExpanded = !participantExpanded }
            ) {
                TextField(
                    value = participantList.find { it.first == selectedParticipantId }?.second ?: "Select Participant",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Participant") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = participantExpanded)
                    },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = participantExpanded,
                    onDismissRequest = { participantExpanded = false }
                ) {
                    participantList.forEach { (uid, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedParticipantId = uid
                                participantExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = score,
                onValueChange = { score = it },
                label = { Text("Score") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Remarks") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedEventId.isNotBlank() && selectedParticipantId.isNotBlank() && score.toDoubleOrNull() != null) {
                        val data = hashMapOf(
                            "eventId" to selectedEventId,
                            "participantId" to selectedParticipantId,
                            "score" to score.toDoubleOrNull(),
                            "remarks" to remarks,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                        db.collection("performanceData").add(data)
                            .addOnSuccessListener {
                                successMessage = "✅ Performance saved!"
                                selectedEventId = ""
                                selectedParticipantId = ""
                                score = ""
                                remarks = ""
                            }
                            .addOnFailureListener {
                                successMessage = "❌ Failed: ${it.message}"
                            }
                    } else {
                        successMessage = "⚠️ Please select event/participant and enter numeric score!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Performance")
            }

            successMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = when {
                        it.startsWith("✅") -> Color.Green
                        it.startsWith("❌") || it.startsWith("⚠️") -> Color.Red
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        }
    }
}
