package com.example.furniturefactory.data.model

data class CartItem(
    val product: FurnitureProduct,
    val quantity: Int,
) {
    val total: Long get() = product.price * quantity
}

data class CartSummary(val items: List<CartItem>) {
    val totalPrice: Long get() = items.sumOf { it.total }
    val totalQuantity: Int get() = items.sumOf { it.quantity }
    val isEmpty: Boolean get() = items.isEmpty()
}
