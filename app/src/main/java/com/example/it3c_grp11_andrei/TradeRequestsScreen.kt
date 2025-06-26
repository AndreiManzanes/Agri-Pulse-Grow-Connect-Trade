package com.example.it3c_grp11_andrei.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class TradeRequest(
    val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val imageName: String = "",
    val price: Double = 0.0,
    val sellerId: String = "",
    val buyerId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val tradeProposal: String? = "",
    val tradeStatus: String? = ""
)

@Composable
fun TradeRequestsScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid
    val tradeRequests = remember { mutableStateListOf<TradeRequest>() }

    DisposableEffect(Unit) {
        var listener: ListenerRegistration? = null

        if (currentUser != null) {
            listener = firestore.collection("carts")
                .whereEqualTo("sellerId", currentUser)
                .whereEqualTo("tradeStatus", "pending")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            TradeRequest(
                                id = doc.id,
                                productId = doc.getString("productId") ?: "",
                                productTitle = doc.getString("productTitle") ?: "",
                                imageName = doc.getString("imageName") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                sellerId = doc.getString("sellerId") ?: "",
                                buyerId = doc.getString("buyerId") ?: "",
                                timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                tradeProposal = doc.getString("tradeProposal"),
                                tradeStatus = doc.getString("tradeStatus")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    tradeRequests.clear()
                    tradeRequests.addAll(items)
                }
        }

        onDispose {
            listener?.remove()
        }
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(tradeRequests) { item ->
            var isLoading by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val imageResId = remember(item.imageName) {
                        context.resources.getIdentifier(item.imageName, "drawable", context.packageName)
                    }

                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = item.productTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("🧺 Product: ${item.productTitle}", style = MaterialTheme.typography.titleMedium)
                    Text("📦 Offered Item: ${item.tradeProposal ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text("💰 Price: ₱%.2f".format(item.price), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    when (item.tradeStatus) {
                        "trade_success" -> Text("✅ Trade Approved", color = MaterialTheme.colorScheme.primary)
                        "declined" -> Text("❌ Trade Declined", color = MaterialTheme.colorScheme.error)
                        else -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    enabled = !isLoading,
                                    onClick = {
                                        isLoading = true
                                        firestore.collection("carts").document(item.id)
                                            .update("tradeStatus", "trade_success")
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "✅ Trade approved", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "❌ Approval failed", Toast.LENGTH_SHORT).show()
                                            }.addOnCompleteListener { isLoading = false }
                                    }
                                ) { Text("Approve") }

                                Button(
                                    enabled = !isLoading,
                                    onClick = {
                                        isLoading = true
                                        firestore.collection("carts").document(item.id)
                                            .update("tradeStatus", "declined")
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "❌ Trade declined", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "⚠️ Decline failed", Toast.LENGTH_SHORT).show()
                                            }.addOnCompleteListener { isLoading = false }
                                    }
                                ) { Text("Decline") }
                            }
                        }
                    }
                }
            }
        }
    }
}
