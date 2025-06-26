package com.example.it3c_grp11_andrei

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class PostTradeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PostTradeScreen()
        }
    }

    @Composable
    fun PostTradeScreen() {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var title by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var condition by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> imageUri = uri }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📤 Post an Item for Trade", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Button(onClick = { imagePicker.launch("image/*") }) {
                Text("Select Item Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Estimated Value (₱)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (imageUri == null || title.isBlank() || value.isBlank() || condition.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields and select an image.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isUploading = true

                    val filename = UUID.randomUUID().toString() + ".jpg"
                    val ref = storage.reference.child("trade_items/$filename")

                    ref.putFile(imageUri!!)
                        .addOnSuccessListener {
                            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                                val postData = hashMapOf(
                                    "userId" to auth.currentUser?.uid,
                                    "title" to title,
                                    "value" to value,
                                    "condition" to condition,
                                    "description" to description,
                                    "imageUrl" to downloadUrl.toString(),
                                    "timestamp" to Timestamp.now()
                                )

                                firestore.collection("tradePosts")
                                    .add(postData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Trade post uploaded!", Toast.LENGTH_SHORT).show()
                                        (context as? Activity)?.finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to save post.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Image upload failed.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { isUploading = false }
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isUploading) "Uploading..." else "Post Trade")
            }
        }
    }
}
