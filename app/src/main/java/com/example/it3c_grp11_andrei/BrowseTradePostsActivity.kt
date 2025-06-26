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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

data class TradePost(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val value: String = "",
    val condition: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

class BrowseTradePostsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowseTradePostsScreen()
                }
            }
        }
    }

    @Composable
    fun BrowseTradePostsScreen() {
        val context = LocalContext.current
        var posts by remember { mutableStateOf(listOf<TradePost>()) }
        var loading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            FirebaseFirestore.getInstance()
                .collection("tradePosts")
                .get()
                .addOnSuccessListener { result ->
                    posts = result.documents.mapNotNull { doc ->
                        val post = doc.toObject<TradePost>()
                        post?.copy(id = doc.id)
                    }
                    loading = false
                }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Trade Item Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("📦 ${post.title}", style = MaterialTheme.typography.titleMedium)
                            Text("💰 Value: ${post.value}")
                            Text("📋 Condition: ${post.condition}")
                            if (post.description.isNotBlank()) {
                                Text("📝 ${post.description}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(context, ProposeTradeActivity::class.java)
                                    intent.putExtra("postId", post.id)
                                    intent.putExtra("ownerId", post.userId)
                                    intent.putExtra("postTitle", post.title)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🤝 Want to Trade?")
                            }
                        }
                    }
                }
            }
        }
    }
}
