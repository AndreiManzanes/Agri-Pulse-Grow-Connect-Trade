package com.example.it3c_grp11_andrei

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class TradeResponseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TradeResponseScreen()
                }
            }
        }
    }
}

@Composable
fun TradeResponseScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser?.uid
    var responses by remember { mutableStateOf(listOf<Trader>()) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            db.collection("traders")
                .whereEqualTo("buyerId", it)
                .get()
                .addOnSuccessListener { result ->
                    val filtered = result.documents.mapNotNull { doc ->
                        val trade = doc.toObject(Trader::class.java)
                        if (trade?.responseStatus == "Accepted" || trade?.responseStatus == "Declined") {
                            trade
                        } else null
                    }
                    responses = filtered
                }
                .addOnFailureListener {
                    Toast.makeText(context, "⚠️ Failed to load trade responses", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("📬 My Trade Responses", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (responses.isEmpty()) {
            Text("No trade responses yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(responses) { trade ->
                    val imageUrl = remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(trade.imageName) {
                        FirebaseStorage.getInstance()
                            .reference.child(trade.imageName)
                            .downloadUrl
                            .addOnSuccessListener { url ->
                                imageUrl.value = url.toString()
                            }
                            .addOnFailureListener {
                                imageUrl.value = null
                            }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            imageUrl.value?.let { url ->
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text("Item: ${trade.productTitle}", style = MaterialTheme.typography.titleMedium)
                            Text("Proposal: ${trade.tradeProposal}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Status: ${trade.responseStatus}",
                                color = if (trade.responseStatus == "Accepted")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
