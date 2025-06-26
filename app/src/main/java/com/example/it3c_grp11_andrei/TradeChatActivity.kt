@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.it3c_grp11_andrei

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class TradeChatActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tradeId = intent.getStringExtra("tradeId") ?: return
        val productTitle = intent.getStringExtra("productTitle") ?: "Trade Item"
        val buyerId = intent.getStringExtra("buyerId") ?: ""
        val sellerId = intent.getStringExtra("sellerId") ?: ""

        setContent {
            TradeChatScreen(
                tradeId = tradeId,
                productTitle = productTitle,
                buyerId = buyerId,
                sellerId = sellerId
            )
        }
    }

    @Composable
    fun TradeChatScreen(
        tradeId: String,
        productTitle: String,
        buyerId: String,
        sellerId: String
    ) {
        val context = LocalContext.current
        var showDealForm by remember { mutableStateOf(false) }

        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Trade Chat") })
            },
            bottomBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showDealForm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🤝 Deal")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Text("Chat messages will appear here...", Modifier.padding(16.dp))
            }

            if (showDealForm) {
                AlertDialog(
                    onDismissRequest = { showDealForm = false },
                    title = { Text("Finalize Trade") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Delivery Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                                saveTradeToHistory(
                                    tradeId,
                                    productTitle,
                                    buyerId,
                                    sellerId,
                                    name,
                                    phone,
                                    address
                                ) {
                                    Toast.makeText(context, "✅ Trade Completed!", Toast.LENGTH_LONG).show()
                                    showDealForm = false
                                }
                            } else {
                                Toast.makeText(context, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Confirm Deal")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDealForm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    private fun saveTradeToHistory(
        tradeId: String,
        productTitle: String,
        buyerId: String,
        sellerId: String,
        name: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit
    ) {
        // First find the trader document by tradeId
        db.collection("traders")
            .whereEqualTo("tradeId", tradeId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docRef = documents.first().reference
                    docRef.update("tradeStatus", "completed", "deal_confirmed", true)
                }

                val orderRef = db.collection("order_history").document()

                val historyData = mapOf(
                    "orderId" to orderRef.id,
                    "productTitle" to productTitle,
                    "buyerId" to buyerId,
                    "sellerId" to sellerId,
                    "name" to name,
                    "phone" to phone,
                    "address" to address,
                    "type" to "trade",
                    "timestamp" to Timestamp.now()
                )

                orderRef.set(historyData)
                    .addOnSuccessListener {
                        onSuccess()
                    }
            }
    }
}
