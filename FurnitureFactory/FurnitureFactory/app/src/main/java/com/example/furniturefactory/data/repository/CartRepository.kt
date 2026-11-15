package com.example.furniturefactory.data.repository

import com.example.furniturefactory.data.local.AppPreferences
import com.example.furniturefactory.data.model.CartItem
import com.example.furniturefactory.data.model.CartSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface CartRepository {
    val cart: Flow<CartSummary>
    suspend fun add(productId: String, quantity: Int = 1)
    suspend fun setQuantity(productId: String, quantity: Int)
    suspend fun remove(productId: String)
    suspend fun clear()
}

class LocalCartRepository(
    private val prefs: AppPreferences,
    private val furniture: FurnitureRepository,
) : CartRepository {

    override val cart: Flow<CartSummary> = combine(prefs.cart, furniture.products) { map, products ->
        CartSummary(
            map.mapNotNull { (id, qty) -> products.firstOrNull { it.id == id }?.let { CartItem(it, qty) } }
        )
    }

    override suspend fun add(productId: String, quantity: Int) {
        prefs.updateCart { current ->
            current.toMutableMap().apply { this[productId] = ((this[productId] ?: 0) + quantity).coerceIn(1, MAX_QTY) }
        }
    }

    override suspend fun setQuantity(productId: String, quantity: Int) {
        prefs.updateCart { current ->
            current.toMutableMap().apply {
                if (quantity <= 0) remove(productId) else this[productId] = quantity.coerceAtMost(MAX_QTY)
            }
        }
    }

    override suspend fun remove(productId: String) {
        prefs.updateCart { it - productId }
    }

    override suspend fun clear() {
        prefs.updateCart { emptyMap() }
    }

    companion object {
        const val MAX_QTY = 99
    }
}
