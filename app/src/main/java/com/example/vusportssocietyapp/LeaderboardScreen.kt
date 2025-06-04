package com.example.vusportssocietyapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var eventList by remember { mutableStateOf(listOf<Pair<String, String>>()) } // eventId to title
    var selectedEventId by remember { mutableStateOf("") }
    var eventExpanded by remember { mutableStateOf(false) }

    var leaderboard by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) } // uid, name, totalScore
    var isLoading by remember { mutableStateOf(true) }

    // Load events for filter dropdown
    LaunchedEffect(true) {
        db.collection("events").get().addOnSuccessListener { result ->
            eventList = result.documents.map { it.id to (it.getString("title") ?: "Untitled") }
        }
    }

    // Fetch performance data + participant names
    LaunchedEffect(selectedEventId) {
        isLoading = true
        val query = if (selectedEventId.isNotBlank()) {
            db.collection("performanceData").whereEqualTo("eventId", selectedEventId)
        } else {
            db.collection("performanceData")
        }

        query.get().addOnSuccessListener { performanceDocs ->
            val grouped = performanceDocs.documents
                .mapNotNull { doc ->
                    val uid = doc.getString("participantId") ?: return@mapNotNull null
                    val score = doc.getDouble("score") ?: 0.0
                    uid to score
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, scores) -> scores.sum() }

            val uids = grouped.keys.toList()
            if (uids.isEmpty()) {
                leaderboard = emptyList()
                isLoading = false
                return@addOnSuccessListener
            }

            db.collection("users")
                .whereIn(FieldPath.documentId(), uids.take(10))
                .get()
                .addOnSuccessListener { userSnapshot ->
                    val nameMap = userSnapshot.documents.associateBy({ it.id }, { it.getString("name") ?: "Unknown" })
                    leaderboard = grouped.map { (uid, score) ->
                        Triple(uid, nameMap[uid] ?: "Unknown", score)
                    }.sortedByDescending { it.third }
                    isLoading = false
                }
        }.addOnFailureListener {
            Log.e("Leaderboard", "Error loading data: ${it.message}")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🏆 Leaderboard") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            ExposedDropdownMenuBox(
                expanded = eventExpanded,
                onExpandedChange = { eventExpanded = !eventExpanded }
            ) {
                TextField(
                    value = eventList.find { it.first == selectedEventId }?.second ?: "All Events",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by Event") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = eventExpanded,
                    onDismissRequest = { eventExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Events") },
                        onClick = {
                            selectedEventId = ""
                            eventExpanded = false
                        }
                    )
                    eventList.forEach { (id, title) ->
                        DropdownMenuItem(
                            text = { Text(title) },
                            onClick = {
                                selectedEventId = id
                                eventExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (leaderboard.isEmpty()) {
                Text("No performance data found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(leaderboard) { (uid, name, score) ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("$name", style = MaterialTheme.typography.titleMedium)
                                Text("Total Score: $score", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
