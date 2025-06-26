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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val imageName: String = "",
    val price: Double = 0.0,
    val sellerId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val quantity: Int = 1
)

@Composable
fun CartScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid
    val cartItems = remember { mutableStateListOf<CartItem>() }

    var name by remember { mutableStateOf(TextFieldValue("")) }
    var phone by remember { mutableStateOf(TextFieldValue("")) }
    var address by remember { mutableStateOf(TextFieldValue("")) }

    // Load cart items (unchecked out only)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("carts")
                .whereEqualTo("buyerId", currentUser)
                .whereIn("checked_out", listOf(null, false))
                .addSnapshotListener { snapshot, _ ->
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            CartItem(
                                id = doc.id,
                                productId = doc.getString("productId") ?: "",
                                productTitle = doc.getString("productTitle") ?: "",
                                imageName = doc.getString("imageName") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                sellerId = doc.getString("sellerId") ?: "",
                                timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                quantity = (doc.getLong("quantity") ?: 1).toInt()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    cartItems.clear()
                    cartItems.addAll(items)
                }
        }
    }

    val totalPrice = cartItems.sumOf { it.price * it.quantity }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("🛒 Your Cart", style = MaterialTheme.typography.headlineSmall)

        if (cartItems.isEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Your cart is empty.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    CartItemCard(item)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Total Price: ₱%.2f".format(totalPrice), style = MaterialTheme.typography.titleMedium)

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

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (name.text.isNotEmpty() && phone.text.isNotEmpty() && address.text.isNotEmpty()) {
                        cartItems.forEach { item ->
                            firestore.collection("carts").document(item.id).update(
                                mapOf(
                                    "checked_out" to true,
                                    "name" to name.text,
                                    "phone" to phone.text,
                                    "address" to address.text,
                                    "checkout_time" to Timestamp.now()
                                )
                            )
                        }
                        Toast.makeText(context, "✅ Purchase successful!", Toast.LENGTH_LONG).show()
                        navController.navigate("checkout_success")
                    } else {
                        Toast.makeText(context, "Please fill out all checkout fields.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Checkout All")
            }
        }
    }
}

@Composable
fun CartItemCard(item: CartItem) {
    val context = LocalContext.current
    val imageResId = remember(item.imageName) {
        context.resources.getIdentifier(item.imageName, "drawable", context.packageName)
    }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = item.productTitle,
                    modifier = Modifier.size(60.dp)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(item.imageName),
                    contentDescription = item.productTitle,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(item.productTitle, style = MaterialTheme.typography.titleMedium)
                Text("₱%.2f x ${item.quantity}".format(item.price), style = MaterialTheme.typography.bodyMedium)
                // ✅ Trade status removed from display
            }
        }
    }
}
