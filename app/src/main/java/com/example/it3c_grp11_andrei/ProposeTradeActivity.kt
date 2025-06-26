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

class ProposeTradeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postId = intent.getStringExtra("postId") ?: ""
        val ownerId = intent.getStringExtra("ownerId") ?: ""
        val postTitle = intent.getStringExtra("postTitle") ?: ""

        setContent {
            IT3C_Grp11_ANDREITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProposeTradeScreen(
                        postId = postId,
                        ownerId = ownerId,
                        postTitle = postTitle,
                        finishActivity = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun ProposeTradeScreen(
    postId: String,
    ownerId: String,
    postTitle: String,
    finishActivity: () -> Unit
) {
    val context = LocalContext.current
    val buyerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var proposalText by remember { mutableStateOf("") }
    var tradeCondition by remember { mutableStateOf("") }
    var tradeValue by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("💬 Propose Trade for:", style = MaterialTheme.typography.labelLarge)
        Text(postTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Upload Image of Your Item")
        }

        imageUri?.let { uri ->
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tradeCondition,
            onValueChange = { tradeCondition = it },
            label = { Text("Condition of your item") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tradeValue,
            onValueChange = { tradeValue = it },
            label = { Text("Estimated value (₱)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = proposalText,
            onValueChange = { proposalText = it },
            label = { Text("Describe your trade offer") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            singleLine = false,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (proposalText.isBlank() || tradeCondition.isBlank() ||
                    tradeValue.isBlank() || imageUri == null
                ) {
                    Toast.makeText(context, "❗ Complete all fields before submitting.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val valueDouble = tradeValue.toDoubleOrNull()
                if (valueDouble == null) {
                    Toast.makeText(context, "❗ Enter a valid numeric value.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSubmitting = true
                val fileName = "trade_uploads/${UUID.randomUUID()}.jpg"
                val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

                storageRef.putFile(imageUri!!)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val newDoc = FirebaseFirestore.getInstance().collection("traders").document()
                            val trade = hashMapOf(
                                "tradeId" to newDoc.id,
                                "productId" to postId,
                                "productTitle" to postTitle,
                                "imageName" to downloadUri.toString(),
                                "tradeProposal" to proposalText,
                                "tradeCondition" to tradeCondition,
                                "tradeValue" to valueDouble,
                                "tradeStatus" to "Pending",
                                "responseStatus" to "Pending",
                                "buyerId" to buyerId,
                                "sellerId" to ownerId,
                                "timestamp" to Timestamp.now()
                            )
                            newDoc.set(trade)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "✅ Proposal submitted!", Toast.LENGTH_SHORT).show()
                                    finishActivity()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "❌ Failed to save proposal.", Toast.LENGTH_SHORT).show()
                                    isSubmitting = false
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "❌ Upload failed.", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text("Submit Trade Proposal")
        }
    }
}
