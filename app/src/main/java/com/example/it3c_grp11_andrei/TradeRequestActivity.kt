package com.example.it3c_grp11_andrei

import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ✅ Include Trader data class (used by Firestore .toObject())
data class Trader(
    val tradeId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val imageName: String = "",
    val tradeProposal: String = "",
    val tradeStatus: String = "",
    val responseStatus: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class TradeRequestsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TradeRequestsScreen()
                }
            }
        }
    }
}

@Composable
fun TradeRequestsScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var proposals by remember { mutableStateOf(listOf<Trader>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            db.collection("traders")
                .whereEqualTo("sellerId", currentUserId)
                .whereEqualTo("responseStatus", "Pending")
                .get()
                .addOnSuccessListener { result ->
                    proposals = result.documents.mapNotNull { it.toObject(Trader::class.java) }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "❌ Failed to load trade requests", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("📥 Incoming Trade Proposals", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (proposals.isEmpty()) {
            Text("No pending proposals.")
        } else {
            LazyColumn {
                items(proposals) { proposal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📦 Product: ${proposal.productTitle}", style = MaterialTheme.typography.titleMedium)
                            Text("💬 Proposal: ${proposal.tradeProposal}", style = MaterialTheme.typography.bodyMedium)
                            Text("⏳ Status: ${proposal.responseStatus}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row {
                                Button(
                                    onClick = {
                                        updateProposalStatus(proposal.tradeId, "Accepted", context)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("✅ Accept")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        updateProposalStatus(proposal.tradeId, "Declined", context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("❌ Decline", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔄 Firestore update function
fun updateProposalStatus(tradeId: String, status: String, context: android.content.Context) {
    val db = FirebaseFirestore.getInstance()
    db.collection("traders").document(tradeId)
        .update("responseStatus", status)
        .addOnSuccessListener {
            Toast.makeText(context, "Proposal $status", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update proposal", Toast.LENGTH_SHORT).show()
        }
}
