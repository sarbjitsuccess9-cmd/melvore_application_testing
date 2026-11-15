package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class ShopRoute(
    val category: String = "all",
    val collection: String = "",
    val query: String = "",
    val brandId: Int = 0
)

@Serializable
object CategoriesRoute

@Serializable
data class ProductDetailsRoute(
    val productId: Int
)

@Serializable
object CheckoutRoute

@Serializable
data class OrderTrackingRoute(
    val orderNumber: String = ""
)

@Serializable
data class AuthRoute(
    val initialTab: String = "login"
)

@Serializable
data class ProfileRoute(
    val tab: String = "overview"
)

@Serializable
object AboutRoute

@Serializable
object BlogRoute

@Serializable
object FaqRoute

@Serializable
object ContactRoute
