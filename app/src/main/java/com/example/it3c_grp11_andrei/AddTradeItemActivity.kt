package com.example.it3c_grp11_andrei

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddTradeItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AddTradeItemScreen()
                }
            }
        }
    }
}

@Composable
fun AddTradeItemScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()) {

        Text("📤 Upload Trade Item", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Choose Image")
        }

        Spacer(modifier = Modifier.height(12.dp))

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Estimated Value (₱)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = condition,
            onValueChange = { condition = it },
            label = { Text("Condition (e.g. New, Used)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (imageUri != null && title.isNotBlank() && value.isNotBlank() && condition.isNotBlank()) {
                    isUploading = true
                    uploadImageToFirebase(imageUri!!) { imageUrl ->
                        val tradeId = db.collection("trade_items").document().id
                        val userId = auth.currentUser?.uid ?: "unknown"

                        // Safely convert value
                        val valueDouble = value.toDoubleOrNull() ?: 0.0

                        val data = hashMapOf<String, Any>(
                            "id" to tradeId,
                            "userId" to userId,
                            "title" to title,
                            "value" to valueDouble,
                            "description" to "Condition: $condition",
                            "condition" to condition,
                            "imageUrl" to imageUrl,
                            "timestamp" to Timestamp.now()
                        )

                        db.collection("trade_items").document(tradeId).set(data)
                            .addOnSuccessListener {
                                Toast.makeText(context, "✅ Trade item posted!", Toast.LENGTH_SHORT).show()
                                // Reset form
                                title = ""
                                value = ""
                                condition = ""
                                imageUri = null
                                isUploading = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "❌ Failed to post item.", Toast.LENGTH_SHORT).show()
                                isUploading = false
                            }
                    }
                } else {
                    Toast.makeText(context, "❗ Please complete all fields and choose an image", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isUploading) "Uploading..." else "Post Item")
        }
    }
}

fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileName = "trade_items/${UUID.randomUUID()}.jpg"
    val imageRef = storageRef.child(fileName)

    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                onSuccess(downloadUrl.toString())
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
        }
}
