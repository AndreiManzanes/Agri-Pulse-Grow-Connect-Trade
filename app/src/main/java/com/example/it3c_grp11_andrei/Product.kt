package com.example.it3c_grp11_andrei

import com.google.firebase.Timestamp

data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val sellerId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val imageName: String = "" // <- reference to local drawable image
)
