package com.example.it3c_grp11_andrei

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.it3c_grp11_andrei.model.Trader
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyTradeProposalsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MyTradeProposalsScreen()
                }
            }
        }
    }
}

@Composable
fun MyTradeProposalsScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var myTrades by remember { mutableStateOf<List<Trader>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("traders")
                .whereEqualTo("buyerId", userId)
                .get()
                .addOnSuccessListener { result ->
                    myTrades = result.documents.mapNotNull { it.toObject(Trader::class.java) }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "❌ Failed to load proposals.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("📬 My Trade Proposals", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()
            myTrades.isEmpty() -> Text("You haven't sent any trade proposals.")
            else -> {
                LazyColumn {
                    items(myTrades) { trade ->
                        TradeProposalCard(trade = trade)
                    }
                }
            }
        }
    }
}

@Composable
fun TradeProposalCard(trade: Trader) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (trade.imageName.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(trade.imageName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("📦 Product: ${trade.productTitle}", style = MaterialTheme.typography.titleMedium)
            Text("🧾 Condition: ${trade.tradeCondition}", style = MaterialTheme.typography.bodyMedium)
            Text("💸 Value: ₱${trade.tradeValue}", style = MaterialTheme.typography.bodyMedium)
            Text("✉️ Proposal: ${trade.tradeProposal}", style = MaterialTheme.typography.bodySmall)
            Text(
                "📋 Status: ${trade.responseStatus}",
                style = MaterialTheme.typography.labelMedium,
                color = when (trade.responseStatus) {
                    "Accepted" -> MaterialTheme.colorScheme.primary
                    "Declined" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
