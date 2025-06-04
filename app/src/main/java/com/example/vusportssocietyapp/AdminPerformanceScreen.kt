package com.example.vusportssocietyapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPerformanceScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var allPerformance by remember { mutableStateOf<List<PerformanceRecord>>(emptyList()) }
    var filteredPerformance by remember { mutableStateOf<List<PerformanceRecord>>(emptyList()) }

    var events by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // eventId to title
    var participants by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // userId to name

    var selectedEventId by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    // Load events, users, and performance data
    LaunchedEffect(true) {
        db.collection("events").get().addOnSuccessListener { snapshot ->
            events = snapshot.documents.map { it.id to (it.getString("title") ?: "") }
        }

        db.collection("users").get().addOnSuccessListener { snapshot ->
            participants = snapshot.documents.map { it.id to (it.getString("name") ?: "") }
        }

        db.collection("performanceData").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { doc ->
                val eventId = doc.getString("eventId") ?: return@mapNotNull null
                val participantId = doc.getString("participantId") ?: return@mapNotNull null
                val score = doc.getDouble("score") ?: return@mapNotNull null
                val remarks = doc.getString("remarks") ?: ""

                PerformanceRecord(
                    id = doc.id,
                    eventId = eventId,
                    participantId = participantId,
                    score = score,
                    remarks = remarks
                )
            }
            allPerformance = list
            filteredPerformance = list
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin: View Performance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenuFilter(
                        label = "Event",
                        items = listOf("All") + events.map { it.second },
                        selected = events.find { it.first == selectedEventId }?.second ?: "All",
                        onSelected = { name ->
                            selectedEventId = events.find { it.second == name }?.first ?: ""
                            filteredPerformance = allPerformance.filter {
                                (selectedEventId.isBlank() || it.eventId == selectedEventId) &&
                                        (selectedUserId.isBlank() || it.participantId == selectedUserId)
                            }
                        }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenuFilter(
                        label = "Participant",
                        items = listOf("All") + participants.map { it.second },
                        selected = participants.find { it.first == selectedUserId }?.second ?: "All",
                        onSelected = { name ->
                            selectedUserId = participants.find { it.second == name }?.first ?: ""
                            filteredPerformance = allPerformance.filter {
                                (selectedEventId.isBlank() || it.eventId == selectedEventId) &&
                                        (selectedUserId.isBlank() || it.participantId == selectedUserId)
                            }
                        }
                    )
                }
            }
            Button(
                onClick = {
                    selectedEventId = ""
                    selectedUserId = ""
                    filteredPerformance = allPerformance
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Clear Filters")
            }


            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredPerformance.isEmpty()) {
                Text("No performance records found.")
            } else {
                LazyColumn {
                    items(filteredPerformance) { record ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val eventName = events.find { it.first == record.eventId }?.second ?: "Unknown Event"
                                val participantName = participants.find { it.first == record.participantId }?.second ?: "Unknown Participant"

                                Text("Event: $eventName")
                                Text("Participant: $participantName")
                                Text("Score: ${record.score}")
                                Text("Remarks: ${record.remarks}")
                                Spacer(modifier = Modifier.height(16.dp))



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
fun DropdownMenuFilter(label: String, items: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onSelected(item)
                    expanded = false
                })
            }
        }
    }
}

data class PerformanceRecord(
    val id: String,
    val eventId: String,
    val participantId: String,
    val score: Double,
    val remarks: String
)
