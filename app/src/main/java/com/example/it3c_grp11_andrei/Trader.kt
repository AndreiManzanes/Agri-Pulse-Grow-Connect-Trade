package com.example.it3c_grp11_andrei.model

import com.google.firebase.Timestamp

data class Trader(
    val tradeId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val imageName: String = "", // this is actually the full download URL
    val tradeProposal: String = "",
    val tradeCondition: String = "",
    val tradeValue: Double = 0.0,
    val tradeStatus: String = "",
    val responseStatus: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
