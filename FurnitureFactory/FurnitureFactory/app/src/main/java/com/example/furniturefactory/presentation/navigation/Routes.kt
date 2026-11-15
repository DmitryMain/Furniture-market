package com.example.furniturefactory.presentation.navigation

import android.net.Uri
import com.example.furniturefactory.data.model.FurnitureCategory

object Routes {
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val CATALOG_PATTERN = "catalog?category={category}"
    const val CART = "cart"
    const val PROFILE = "profile"

    const val PRODUCT_PATTERN = "product/{id}?fullscreen={fullscreen}"
    const val CHECKOUT = "checkout"
    const val ORDER_SUCCESS_PATTERN = "order_success/{number}"
    const val FAVORITES = "favorites"
    const val COMPARE = "compare"
    const val ORDERS = "orders"
    const val NOTIFICATIONS = "notifications"
    const val ABOUT = "about"
    const val SUPPORT = "support"
    const val SETTINGS = "settings"

    fun catalog(category: FurnitureCategory? = null): String =
        if (category == null) CATALOG else "catalog?category=${category.name}"

    fun product(id: String, fullscreen: Boolean = false): String = "product/${Uri.encode(id)}?fullscreen=$fullscreen"

    fun orderSuccess(number: String): String = "order_success/${Uri.encode(number)}"
}
