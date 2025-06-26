package com.example.it3c_grp11_andrei

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MarketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProductMarketScreen()
                }
            }
        }
    }
}

@Composable
fun ProductMarketScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val products = remember { mutableStateListOf<Product>() }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("products")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val loaded = result.documents.mapNotNull { it.toProductSafe() }
                products.clear()
                products.addAll(loaded)
            }
            .addOnFailureListener {
                Log.e("MarketActivity", "❌ Failed to load products: ${it.message}")
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 Market", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = {
                context.startActivity(Intent(context, CartActivity::class.java))
            }) {
                Text("View Cart")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (products.isEmpty()) {
            Text("No products available at the moment.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products) { product ->
                    ProductMarketCard(product, userId)
                }
            }
        }
    }
}

@Composable
fun ProductMarketCard(product: Product, userId: String) {
    val context = LocalContext.current
    val imageResId = remember(product.imageName) {
        context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = product.title,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 12.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(product.description, fontSize = 14.sp)
                    Text("₱%.2f".format(product.price), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    addToCart(product, userId) {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Cart")
            }
        }
    }
}

private fun DocumentSnapshot.toProductSafe(): Product? {
    return try {
        Product(
            id = id,
            title = getString("title") ?: return null,
            description = getString("description") ?: "",
            price = (get("price") as? Number)?.toDouble() ?: 0.0,
            sellerId = getString("sellerId") ?: "",
            timestamp = getTimestamp("timestamp") ?: Timestamp.now(),
            imageName = getString("imageName") ?: ""
        )
    } catch (e: Exception) {
        Log.e("MarketActivity", "❌ Error parsing product: ${e.message}")
        null
    }
}

fun addToCart(product: Product, userId: String, onResult: (String) -> Unit) {
    if (userId.isBlank()) {
        onResult("User not logged in.")
        return
    }

    val db = FirebaseFirestore.getInstance()

    db.collection("carts")
        .whereEqualTo("buyerId", userId)
        .whereEqualTo("productId", product.id)
        .whereEqualTo("checked_out", false)
        .get()
        .addOnSuccessListener { result ->
            if (result.isEmpty) {
                val cartItem = hashMapOf(
                    "buyerId" to userId,
                    "productId" to product.id,
                    "productTitle" to product.title,
                    "price" to product.price,
                    "imageName" to product.imageName,
                    "quantity" to 1,
                    "checked_out" to false, // ✅ FIXED HERE
                    "timestamp" to Timestamp.now(),
                    "sellerId" to product.sellerId
                )

                db.collection("carts")
                    .add(cartItem)
                    .addOnSuccessListener {
                        onResult("✅ Added to cart!")
                    }
                    .addOnFailureListener {
                        onResult("❌ Failed to add to cart.")
                    }
            } else {
                onResult("⚠️ Product already in cart.")
            }
        }
        .addOnFailureListener {
            onResult("❌ Cart check failed.")
        }
}
