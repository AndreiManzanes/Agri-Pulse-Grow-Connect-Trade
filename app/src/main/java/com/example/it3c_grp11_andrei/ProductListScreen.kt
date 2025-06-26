package com.example.it3c_grp11_andrei.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it3c_grp11_andrei.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.it3c_grp11_andrei.R
import com.google.firebase.Timestamp

@Composable
fun ProductListScreen(onAddClick: () -> Unit = {}) {
    val products = remember { mutableStateListOf<Product>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        FirebaseFirestore.getInstance()
            .collection("products")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirestoreDebug", "✅ Loaded ${snapshot.size()} products.")
                val loaded = snapshot.documents.mapNotNull { doc ->
                    Log.d("FirestoreDebug", "📄 Raw: ${doc.data}")
                    doc.toProductSafe()
                }
                products.clear()
                products.addAll(loaded)
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "❌ Firestore error: ${e.message}")
                isLoading = false
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Button(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Product")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            products.isEmpty() -> {
                Text(
                    text = "No products available at the moment.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products) { product ->
                        ProductCard(product)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    val imageId = getImageResId(product.imageName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (imageId != null) {
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = product.title,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(product.description, fontSize = 14.sp)
                Text("₱${"%.2f".format(product.price)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 🧠 Converts Firestore document to Product
private fun DocumentSnapshot.toProductSafe(): Product? {
    return try {
        val title = getString("title") ?: return null
        val description = getString("description") ?: ""
        val imageName = getString("imageName") ?: ""
        val price = (get("price") as? Number)?.toDouble() ?: 0.0
        val sellerId = getString("sellerId") ?: ""
        val timestamp = getTimestamp("timestamp") ?: Timestamp.now()

        Product(
            id = id,
            title = title,
            description = description,
            imageName = imageName,
            price = price,
            sellerId = sellerId,
            timestamp = timestamp
        )
    } catch (e: Exception) {
        Log.e("FirestoreDebug", "❌ Parsing error: ${e.message}")
        null
    }
}

// 🔄 Converts imageName to drawable resource ID
private fun getImageResId(imageName: String): Int? {
    return try {
        val resId = R.drawable::class.java.getField(imageName).getInt(null)
        resId
    } catch (e: Exception) {
        Log.e("ImageLoad", "⚠️ Image not found: $imageName")
        null
    }
}
