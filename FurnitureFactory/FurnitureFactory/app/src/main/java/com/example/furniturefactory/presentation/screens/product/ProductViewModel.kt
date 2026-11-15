package com.example.furniturefactory.presentation.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.data.repository.CartRepository
import com.example.furniturefactory.data.repository.CompareResult
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.data.repository.LocalUserRepository
import com.example.furniturefactory.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductUiState(
    val isLoading: Boolean = true,
    val product: FurnitureProduct? = null,
    val isFavorite: Boolean = false,
    val isInCompare: Boolean = false,
    val quantity: Int = 1,
    val inCartQuantity: Int = 0,
)

sealed interface ProductEvent {
    data class AddedToCart(val quantity: Int) : ProductEvent
    data class Message(val text: String) : ProductEvent
}

class ProductViewModel(
    private val productId: String,
    furniture: FurnitureRepository,
    private val cart: CartRepository,
    private val users: UserRepository,
) : ViewModel() {

    private val quantity = MutableStateFlow(1)
    private val _events = MutableSharedFlow<ProductEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProductEvent> = _events.asSharedFlow()

    val state: StateFlow<ProductUiState> = combine(
        furniture.product(productId), users.favorites, users.compare, cart.cart, quantity,
    ) { product, favorites, compare, cartSummary, qty ->
        ProductUiState(
            isLoading = false,
            product = product,
            isFavorite = productId in favorites,
            isInCompare = productId in compare,
            quantity = qty,
            inCartQuantity = cartSummary.items.firstOrNull { it.product.id == productId }?.quantity ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductUiState())

    fun increase() {
        quantity.value = (quantity.value + 1).coerceAtMost(99)
    }

    fun decrease() {
        quantity.value = (quantity.value - 1).coerceAtLeast(1)
    }

    fun addToCart() {
        val qty = quantity.value
        viewModelScope.launch {
            cart.add(productId, qty)
            quantity.value = 1
            _events.emit(ProductEvent.AddedToCart(qty))
        }
    }

    fun toggleFavorite() {
        val wasFavorite = state.value.isFavorite
        viewModelScope.launch {
            users.toggleFavorite(productId)
            _events.emit(ProductEvent.Message(if (wasFavorite) "Удалено из избранного" else "Добавлено в избранное"))
        }
    }

    fun toggleCompare() {
        viewModelScope.launch {
            val text = when (users.toggleCompare(productId)) {
                CompareResult.ADDED -> "Добавлено к сравнению"
                CompareResult.REMOVED -> "Удалено из сравнения"
                CompareResult.LIMIT_REACHED -> "Можно сравнивать до ${LocalUserRepository.MAX_COMPARE} моделей"
            }
            _events.emit(ProductEvent.Message(text))
        }
    }
}
