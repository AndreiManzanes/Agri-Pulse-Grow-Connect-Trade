package com.example.it3c_grp11_andrei

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CartScreen()
                }
            }
        }
    }
}

@Composable
fun CartScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val cartItems = remember { mutableStateListOf<CartItem>() }

    val name = remember { mutableStateOf(TextFieldValue()) }
    val phone = remember { mutableStateOf(TextFieldValue()) }
    val address = remember { mutableStateOf(TextFieldValue()) }

    var total by remember { mutableStateOf(0.0) }

    suspend fun loadCart() {
        val snapshot = db.collection("carts")
            .whereEqualTo("buyerId", userId)
            .whereEqualTo("checked_out", false)
            .get().await()

        cartItems.clear()
        cartItems.addAll(snapshot.documents.mapNotNull {
            try {
                CartItem(
                    id = it.id,
                    title = it.getString("productTitle") ?: "",
                    price = it.getDouble("price") ?: 0.0,
                    imageName = it.getString("imageName") ?: "",
                    quantity = mutableStateOf((it.getLong("quantity") ?: 1).toInt())
                )
            } catch (e: Exception) {
                null
            }
        })

        total = cartItems.sumOf { it.price * it.quantity.value }
    }

    LaunchedEffect(Unit) {
        loadCart()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("🛒 Your Cart", style = MaterialTheme.typography.headlineSmall)

        if (cartItems.isEmpty()) {
            Text("Cart is empty.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    CartItemCard(
                        item,
                        onRemove = {
                            db.collection("carts").document(item.id).delete()
                            cartItems.remove(item)
                            total = cartItems.sumOf { it.price * it.quantity.value }
                        },
                        onQuantityChange = { newQty ->
                            db.collection("carts").document(item.id)
                                .update("quantity", newQty)
                            item.quantity.value = newQty
                            total = cartItems.sumOf { it.price * it.quantity.value }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: ₱%.2f".format(total), style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(name.value, { name.value = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone.value, { phone.value = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address.value, { address.value = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                if (name.value.text.isBlank() || phone.value.text.isBlank() || address.value.text.isBlank()) {
                    Toast.makeText(context, "Fill all info to checkout", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                cartItems.forEach { item ->
                    db.collection("carts").document(item.id).update(
                        mapOf(
                            "checked_out" to true,
                            "customer_name" to name.value.text,
                            "customer_phone" to phone.value.text,
                            "customer_address" to address.value.text,
                            "checkout_time" to Timestamp.now()
                        )
                    )
                }

                Toast.makeText(context, "✅ Checkout successful!", Toast.LENGTH_LONG).show()

                val intent = Intent(context, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)

            }, modifier = Modifier.fillMaxWidth()) {
                Text("Checkout")
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
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
                    contentDescription = item.title,
                    modifier = Modifier.size(60.dp)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageName),
                    contentDescription = item.title,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = MaterialTheme.typography.titleMedium.fontWeight)
                Text("₱%.2f x ${item.quantity.value}".format(item.price))
                // ✅ Trade status removed
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    if (item.quantity.value > 1) {
                        val newQty = item.quantity.value - 1
                        item.quantity.value = newQty
                        onQuantityChange(newQty)
                    }
                }) { Text("-") }

                Text("${item.quantity.value}", modifier = Modifier.padding(vertical = 4.dp))

                Button(onClick = {
                    val newQty = item.quantity.value + 1
                    item.quantity.value = newQty
                    onQuantityChange(newQty)
                }) { Text("+") }

                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onRemove, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Remove", color = Color.White)
                }
            }
        }
    }
}

data class CartItem(
    val id: String,
    val title: String,
    val price: Double,
    val imageName: String,
    var quantity: MutableState<Int>
)
