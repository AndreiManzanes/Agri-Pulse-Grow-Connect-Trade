package com.example.it3c_grp11_andrei.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object NotificationUtils {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Sends a notification to a user by adding a document to the `notifications` collection.
     *
     * @param userId The user to receive the notification.
     * @param title The title of the notification.
     * @param message The message or body of the notification.
     * @param type A string to identify the type of notification (optional, e.g., "trade", "checkout").
     */
    fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        type: String = "info"
    ) {
        val notification = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "timestamp" to Timestamp.now(),
            "type" to type,
            "read" to false
        )

        firestore.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                // You can log or toast here if needed
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
