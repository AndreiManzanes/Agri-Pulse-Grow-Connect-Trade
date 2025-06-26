package com.example.it3c_grp11_andrei

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)

class ConnectActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val senderId = auth.currentUser?.uid
        val recipientId = intent.getStringExtra("recipientId") // Optional

        if (senderId == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val chatId = if (!recipientId.isNullOrEmpty()) {
            if (senderId < recipientId) "${senderId}_$recipientId" else "${recipientId}_$senderId"
        } else {
            "general" // Public or group chat fallback
        }

        val messagesCollection = db.collection("chats").document(chatId).collection("messages")

        setContent {
            val context = LocalContext.current
            var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
            var newMessage by remember { mutableStateOf(TextFieldValue("")) }

            // Real-time listener
            LaunchedEffect(chatId) {
                messagesCollection
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("Chat", "Listen failed", e)
                            Toast.makeText(context, "Error loading messages", Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }

                        val updatedMessages = snapshot?.documents?.mapNotNull { doc ->
                            val sender = doc.getString("senderId") ?: return@mapNotNull null
                            val content = doc.getString("content") ?: return@mapNotNull null
                            val timestamp = doc.getTimestamp("timestamp")
                            ChatMessage(sender, content, timestamp)
                        } ?: emptyList()

                        messages = updatedMessages
                    }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💬 Chat", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.senderId == senderId
                            val time = msg.timestamp?.toDate()?.let {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                            } ?: "—"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        if (isMe) "You" else "Them",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                                    Text(time, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        TextField(
                            value = newMessage,
                            onValueChange = { newMessage = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your message...") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val msg = newMessage.text.trim()
                                if (msg.isNotEmpty()) {
                                    val message = hashMapOf(
                                        "senderId" to senderId,
                                        "content" to msg,
                                        "timestamp" to Timestamp.now()
                                    )
                                    messagesCollection.add(message)
                                        .addOnSuccessListener { newMessage = TextFieldValue("") }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to send", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}
