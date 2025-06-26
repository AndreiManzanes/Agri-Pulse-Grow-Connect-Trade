package com.example.it3c_grp11_andrei

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoneDealActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tradeId = intent.getStringExtra("tradeId") ?: return
        val productTitle = intent.getStringExtra("productTitle") ?: "Untitled"
        val imageName = intent.getStringExtra("imageName") ?: ""

        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DoneDealForm(
                        tradeId = tradeId,
                        productTitle = productTitle,
                        imageName = imageName
                    )
                }
            }
        }
    }

    @Composable
    fun DoneDealForm(tradeId: String, productTitle: String, imageName: String) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("✅ Finalize Trade", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Number") })
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Delivery Address") })

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                if (name.isBlank() || phone.isBlank() || address.isBlank()) {
                    Toast.makeText(this@DoneDealActivity, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val dealData = hashMapOf(
                    "userId" to auth.currentUser?.uid,
                    "productTitle" to productTitle,
                    "imageName" to imageName,
                    "tradeId" to tradeId,
                    "customerName" to name,
                    "phone" to phone,
                    "address" to address,
                    "timestamp" to Timestamp.now(),
                    "type" to "Trade" // so you can distinguish between 'Buy' and 'Trade'
                )

                firestore.collection("order_history")
                    .add(dealData)
                    .addOnSuccessListener {
                        Toast.makeText(this@DoneDealActivity, "Trade successfully completed!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@DoneDealActivity, OrderHistoryActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@DoneDealActivity, "Failed to complete trade.", Toast.LENGTH_SHORT).show()
                    }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Done Deal")
            }
        }
    }
}
