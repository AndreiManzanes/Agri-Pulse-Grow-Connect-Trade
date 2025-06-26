package com.example.it3c_grp11_andrei.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TradeOfferScreen(productId: String, sellerId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val offerProducts = remember { mutableStateListOf<Product>() }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var productToTradeFor by remember { mutableStateOf<Product?>(null) }

    // Fetch product info
    LaunchedEffect(productId) {
        db.collection("products").document(productId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    productToTradeFor = Product(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                        imageName = doc.getString("imageName") ?: "",
                        sellerId = doc.getString("sellerId") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    )
                }
            }

        // Fetch user's own products
        db.collection("products")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    Product(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                        imageName = doc.getString("imageName") ?: "",
                        sellerId = doc.getString("sellerId") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    )
                }
                offerProducts.clear()
                offerProducts.addAll(list)
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (productToTradeFor == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading product info...")
            }
            return
        }

        Text(
            "📦 Trade Offer for: ${productToTradeFor!!.title}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (offerProducts.isEmpty()) {
            Text("You have no products available to offer.")
        } else {
            Text("Select a product to offer in exchange:", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(offerProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedProduct = product },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val imageRes = context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
                            if (imageRes != 0) {
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.title, fontWeight = FontWeight.Bold)
                                Text("₱${"%.2f".format(product.price)}")
                            }

                            if (selectedProduct?.id == product.id) {
                                Text("✔", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedProduct == null) {
                        Toast.makeText(context, "Please select a product to offer.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Save to Firestore
                    val tradeData = hashMapOf(
                        "buyerId" to userId,
                        "sellerId" to productToTradeFor!!.sellerId,
                        "productId" to productToTradeFor!!.id,
                        "productTitle" to productToTradeFor!!.title,
                        "imageName" to productToTradeFor!!.imageName,
                        "price" to productToTradeFor!!.price,
                        "quantity" to 1,
                        "tradeProposal" to selectedProduct!!.title,
                        "tradeProductId" to selectedProduct!!.id,
                        "tradeStatus" to "pending",
                        "timestamp" to Timestamp.now()
                    )

                    db.collection("carts")
                        .add(tradeData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "✅ Trade offer sent!", Toast.LENGTH_SHORT).show()
                            selectedProduct = null
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "❌ Failed to send trade offer.", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Trade Offer")
            }
        }
    }
}
