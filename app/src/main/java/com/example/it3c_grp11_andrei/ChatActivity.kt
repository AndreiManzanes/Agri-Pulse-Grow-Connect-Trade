package com.example.it3c_grp11_andrei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class ChatActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val productId = intent.getStringExtra("productId") ?: return
        val sellerId = intent.getStringExtra("sellerId") ?: return
        val currentUserId = auth.currentUser?.uid ?: return

        setContent {
            ChatScreen(productId = productId, currentUserId = currentUserId, sellerId = sellerId)
        }
    }
}

@Composable
fun ChatScreen(productId: String, currentUserId: String, sellerId: String) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()

    // Realtime listener for chat messages
    LaunchedEffect(Unit) {
        db.collection("chats")
            .document(productId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { message ->
                val isCurrentUser = message.senderId == currentUserId
                val senderLabel = if (isCurrentUser) "You" else "Them"

                Text(
                    text = "$senderLabel: ${message.text}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    color = if (isCurrentUser) Color.Blue else Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .height(56.dp)
                    .fillMaxWidth()
            )

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val message = Message(
                            senderId = currentUserId,
                            text = inputText,
                            timestamp = Timestamp.now()
                        )
                        db.collection("chats")
                            .document(productId)
                            .collection("messages")
                            .add(message)
                        inputText = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}
