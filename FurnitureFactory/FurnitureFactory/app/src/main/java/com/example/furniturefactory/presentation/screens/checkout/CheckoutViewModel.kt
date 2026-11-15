package com.example.furniturefactory.presentation.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.CartSummary
import com.example.furniturefactory.data.model.OrderContact
import com.example.furniturefactory.data.repository.CartRepository
import com.example.furniturefactory.data.repository.OrderRepository
import com.example.furniturefactory.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val form: CheckoutForm = CheckoutForm(),
    val errors: Map<CheckoutField, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
)

class CheckoutViewModel(
    private val cart: CartRepository,
    private val orders: OrderRepository,
    users: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    /** null — корзина ещё загружается. */
    val cartSummary: StateFlow<CartSummary?> = cart.cart
        .stateIn<CartSummary?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _placed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Номер оформленного заказа. */
    val placed: SharedFlow<String> = _placed.asSharedFlow()

    init {
        // подставляем данные профиля
        viewModelScope.launch {
            val profile = users.profile.first()
            if (!profile.isGuest) {
                _state.update {
                    it.copy(
                        form = it.form.copy(
                            fullName = it.form.fullName.ifBlank { profile.name },
                            email = it.form.email.ifBlank { profile.email },
                            phone = it.form.phone.ifBlank { profile.phone },
                            city = it.form.city.ifBlank { profile.city },
                        ),
                    )
                }
            }
        }
    }

    fun update(transform: (CheckoutForm) -> CheckoutForm) {
        _state.update { s ->
            val form = transform(s.form)
            // ошибки пересчитываем только для уже подсвеченных полей
            val errors = if (s.errors.isEmpty()) s.errors else CheckoutValidator.validate(form).filterKeys { it in s.errors }
            s.copy(form = form, errors = errors, submitError = null)
        }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = CheckoutValidator.validate(current.form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val items = cart.cart.first().items
                val f = current.form
                val order = orders.placeOrder(
                    OrderContact(f.fullName.trim(), f.phone.trim(), f.email.trim(), f.city.trim(), f.address.trim(), f.comment.trim()),
                    items,
                )
                cart.clear()
                _placed.emit(order.number)
            } catch (e: Exception) {
                _state.update { it.copy(submitError = e.message ?: "Не удалось оформить заказ") }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
