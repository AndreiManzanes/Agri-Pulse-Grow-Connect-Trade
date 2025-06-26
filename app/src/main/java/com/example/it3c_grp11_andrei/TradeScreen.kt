package com.example.it3c_grp11_andrei.screens

sealed class TradeScreen(val route: String) {
    object ProductList : TradeScreen("product_list")
    object AddProduct : TradeScreen("add_product")
    object TradeOffer : TradeScreen("trade_offer/{productId}/{sellerId}") {
        fun createRoute(productId: String, sellerId: String): String =
            "trade_offer/$productId/$sellerId"
    }
    object TradeRequests : TradeScreen("trade_requests")
}
