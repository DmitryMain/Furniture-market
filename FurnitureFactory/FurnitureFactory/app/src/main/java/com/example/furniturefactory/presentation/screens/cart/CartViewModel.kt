package com.example.furniturefactory.presentation.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.CartSummary
import com.example.furniturefactory.data.repository.CartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(val isLoading: Boolean = true, val summary: CartSummary = CartSummary(emptyList()))

class CartViewModel(private val cart: CartRepository) : ViewModel() {

    val state: StateFlow<CartUiState> = cart.cart
        .map { CartUiState(isLoading = false, summary = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    fun increase(productId: String, current: Int) = setQuantity(productId, current + 1)

    fun decrease(productId: String, current: Int) = setQuantity(productId, (current - 1).coerceAtLeast(1))

    private fun setQuantity(productId: String, quantity: Int) {
        viewModelScope.launch { cart.setQuantity(productId, quantity) }
    }

    fun remove(productId: String) {
        viewModelScope.launch { cart.remove(productId) }
    }

    fun clear() {
        viewModelScope.launch { cart.clear() }
    }
}
