package com.example.it3c_grp11_andrei

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

data class TradeItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val condition: String = "",
    val imageUrl: String = "",
    val value: Double = 0.0,
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

class TradeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TradeScreen()
                }
            }
        }
    }
}

@Composable
fun TradeScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var items by remember { mutableStateOf(listOf<TradeItem>()) }

    LaunchedEffect(Unit) {
        db.collection("trade_items")
            .get()
            .addOnSuccessListener { result ->
                items = result.documents.mapNotNull { doc ->
                    val item = doc.toObject<TradeItem>()
                    item?.copy(id = doc.id)
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                context.startActivity(Intent(context, AddTradeItemActivity::class.java))
            }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("♻️ Trade Market", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Text("No trade items available.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(items) { item ->
                        TradeItemCard(item = item, onProposeClick = {
                            val intent = Intent(context, ProposeTradeActivity::class.java).apply {
                                putExtra("postId", item.id)
                                putExtra("ownerId", item.userId)
                                putExtra("postTitle", item.title)
                                putExtra("postValue", item.value)
                                putExtra("postCondition", item.condition)
                                putExtra("postImage", item.imageUrl)
                            }
                            context.startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun TradeItemCard(item: TradeItem, onProposeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = rememberAsyncImagePainter(item.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("📋 Condition: ${item.condition}")
            Text("📝 ${item.description}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onProposeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Propose Trade")
            }
        }
    }
}
