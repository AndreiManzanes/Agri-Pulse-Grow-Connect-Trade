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
import java.util.*

class TradeDealFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tradeId = intent.getStringExtra("tradeId") ?: ""

        setContent {
            TradeDealFormScreen(tradeId)
        }
    }

    @Composable
    fun TradeDealFormScreen(tradeId: String) {
        val context = LocalContext.current
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {

            Text("📝 Complete the Trade", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Delivery Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                        FirebaseFirestore.getInstance()
                            .collection("traders")
                            .document(tradeId)
                            .update(
                                mapOf(
                                    "deal_confirmed" to true,
                                    "customer_name" to name,
                                    "customer_phone" to phone,
                                    "customer_address" to address,
                                    "checkout_time" to Timestamp(Date())
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(context, "Trade marked as Done Deal!", Toast.LENGTH_SHORT).show()
                                (context as? ComponentActivity)?.finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to confirm trade", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✅ Done Deal")
            }
        }
    }
}
