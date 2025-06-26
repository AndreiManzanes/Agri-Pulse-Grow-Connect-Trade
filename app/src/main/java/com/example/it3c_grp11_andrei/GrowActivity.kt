package com.example.it3c_grp11_andrei

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

data class GrowResource(
    val title: String = "",
    val content: String = "",
    val url: String = ""
)

class GrowActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("GrowActivity", "Activity created")
        setContent {
            val context = LocalContext.current
            var resources by remember { mutableStateOf<List<GrowResource>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                db.collection("grow_resources").get()
                    .addOnSuccessListener { result ->
                        resources = result.map { doc ->
                            GrowResource(
                                title = doc.getString("title") ?: "No Title",
                                content = doc.getString("content") ?: "No Content",
                                url = doc.getString("url") ?: ""
                            )
                        }
                        Log.d("GrowActivity", "Fetched ${resources.size} grow resources")
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = e.message ?: "Unknown error"
                        Log.e("GrowActivity", "Error fetching grow resources", e)
                        isLoading = false
                        Toast.makeText(context, "Failed to load resources: $errorMessage", Toast.LENGTH_LONG).show()
                    }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Grow Resources", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isLoading -> Text("Loading resources...")
                        errorMessage != null -> Text("Error: $errorMessage")
                        resources.isEmpty() -> Text("No grow resources available.")
                        else -> LazyColumn {
                            items(resources) { resource ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(resource.title, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(resource.content, style = MaterialTheme.typography.bodyMedium)

                                        // Show the button only if a URL is present
                                        if (resource.url.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.url))
                                                    context.startActivity(intent)
                                                }
                                            ) {
                                                Text("Watch Tutorial")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
