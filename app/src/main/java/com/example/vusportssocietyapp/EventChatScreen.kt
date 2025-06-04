package com.example.vusportssocietyapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Message Data Model
data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventChatScreen(eventId: String, navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageInput by remember { mutableStateOf(TextFieldValue("")) }

    // Listen for realtime chat updates
    LaunchedEffect(eventId) {
        db.collection("chats")
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Chat", "Error loading chat: ${error.message}")
                    return@addSnapshotListener
                }

                messages = snapshot?.documents?.mapNotNull { doc ->
                    ChatMessage(
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getTimestamp("timestamp")
                    )
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Chat") },
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
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(8.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    if (isMe) MaterialTheme.colorScheme.primary
                                    else Color.LightGray,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                                .widthIn(max = 250.dp)
                        ) {
                            if (!isMe) Text(msg.senderName, style = MaterialTheme.typography.labelSmall)
                            Text(msg.message, color = if (isMe) Color.White else Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val text = messageInput.text.trim()
                    if (text.isNotEmpty() && currentUserId != null) {
                        db.collection("users").document(currentUserId).get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("name") ?: "User"
                                val chat = hashMapOf(
                                    "eventId" to eventId,
                                    "senderId" to currentUserId,
                                    "senderName" to name,
                                    "message" to text,
                                    "timestamp" to Timestamp.now()
                                )
                                db.collection("chats").add(chat)
                                messageInput = TextFieldValue("")
                            }
                    }
                }) {
                    Text("Send")
                }

            }
        }
    }
}
