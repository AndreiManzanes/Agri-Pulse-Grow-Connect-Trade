package com.example.it3c_grp11_andrei.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it3c_grp11_andrei.Product
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun TradeHomeScreen(
    onNavigateToCart: () -> Unit
) {
    val products = remember { mutableStateListOf<Product>() }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("products")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { it.toProductSafe() }
                products.clear()
                products.addAll(items)
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 Market", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onNavigateToCart) {
                Text("View Cart")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onAddToCart = { context -> addProductToCart(context, product) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onAddToCart: (Context) -> Unit
) {
    val context = LocalContext.current
    val imageResId = remember(product.imageName) {
        context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                            .padding(end = 16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(product.description, fontSize = 14.sp)
                    Text("₱%.2f".format(product.price), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddToCart(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Cart")
                }
            }
        }
    }
}

fun addProductToCart(context: Context, product: Product) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    if (product.sellerId == currentUserId) {
        Toast.makeText(context, "⚠️ You cannot buy your own product.", Toast.LENGTH_SHORT).show()
        return
    }

    db.collection("carts")
        .whereEqualTo("buyerId", currentUserId)
        .whereEqualTo("productId", product.id)
        .whereEqualTo("checked_out", false)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                Toast.makeText(context, "⚠️ Already in cart!", Toast.LENGTH_SHORT).show()
            } else {
                val cartData = hashMapOf(
                    "buyerId" to currentUserId,
                    "sellerId" to product.sellerId,
                    "productId" to product.id,
                    "productTitle" to product.title,
                    "imageName" to product.imageName,
                    "price" to product.price,
                    "quantity" to 1,
                    "tradeProposal" to null,
                    "tradeStatus" to "none",
                    "checked_out" to false,
                    "timestamp" to Timestamp.now()
                )

                db.collection("carts")
                    .add(cartData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "✅ Added to cart!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "❌ Failed to add to cart.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
}

fun DocumentSnapshot.toProductSafe(): Product? {
    return try {
        Product(
            id = id,
            title = getString("title") ?: return null,
            description = getString("description") ?: "",
            imageName = getString("imageName") ?: "",
            price = (get("price") as? Number)?.toDouble() ?: 0.0,
            sellerId = getString("sellerId") ?: "",
            timestamp = getTimestamp("timestamp") ?: Timestamp.now()
        )
    } catch (e: Exception) {
        null
    }
}
