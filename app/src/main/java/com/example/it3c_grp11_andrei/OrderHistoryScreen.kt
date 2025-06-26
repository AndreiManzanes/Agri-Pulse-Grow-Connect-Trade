package com.example.it3c_grp11_andrei.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class OrderItem(
    val id: String = "",
    val productTitle: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val totalPrice: Double = 0.0,
    val type: String = "", // "Purchased" or "Traded"
    val timestamp: String = "",
    val status: String = ""
)

@Composable
fun OrderHistoryScreen() {
    val orders = remember { mutableStateListOf<OrderItem>() }
    val loading = remember { mutableStateOf(true) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val tempList = mutableListOf<OrderItem>()

        // ✅ 1. Market Purchases from "carts"
        firestore.collection("carts")
            .whereEqualTo("buyerId", userId)
            .whereEqualTo("checked_out", true)
            .orderBy("checkout_time", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getTimestamp("checkout_time")?.toDate()
                    val quantity = doc.getLong("quantity")?.toInt() ?: 1
                    val price = doc.getDouble("price") ?: 0.0
                    val totalPrice = doc.getDouble("totalPrice") ?: price * quantity

                    tempList.add(
                        OrderItem(
                            id = doc.id,
                            productTitle = doc.getString("productTitle") ?: "",
                            price = price,
                            quantity = quantity,
                            totalPrice = totalPrice,
                            type = "Purchased",
                            timestamp = timestamp?.let {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                            } ?: "",
                            status = "Purchased"
                        )
                    )
                }
                orders.clear()
                orders.addAll(tempList)
                loading.value = false
            }

        // ✅ 2. Finalized Trade Deals from "order_history"
        firestore.collection("order_history")
            .whereEqualTo("buyerId", userId)
            .whereEqualTo("type", "trade")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val quantity = doc.getLong("quantity")?.toInt() ?: 1
                    val price = doc.getDouble("price") ?: 0.0

                    tempList.add(
                        OrderItem(
                            id = doc.id,
                            productTitle = doc.getString("productTitle") ?: "",
                            price = price,
                            quantity = quantity,
                            totalPrice = price * quantity,
                            type = "Traded",
                            timestamp = timestamp?.let {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                            } ?: "",
                            status = "Traded"
                        )
                    )
                }

                // Final sort and refresh list
                orders.clear()
                orders.addAll(tempList.sortedByDescending { it.timestamp })
                loading.value = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📦 My Order History", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading.value -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            orders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders yet.")
                }
            }

            else -> {
                LazyColumn {
                    items(orders) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(order.productTitle, style = MaterialTheme.typography.titleMedium)
                                Text("Unit Price: ₱${"%.2f".format(order.price)}")
                                Text("Quantity: ${order.quantity}")
                                Text("Total: ₱${"%.2f".format(order.totalPrice)}")
                                Text("Type: ${order.type}")
                                Text("Status: ${order.status}")
                                Text("Date: ${order.timestamp}")
                            }
                        }
                    }
                }
            }
        }
    }
}
