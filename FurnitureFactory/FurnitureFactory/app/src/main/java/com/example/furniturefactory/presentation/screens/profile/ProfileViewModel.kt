package com.example.furniturefactory.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.NotificationSettings
import com.example.furniturefactory.data.model.Order
import com.example.furniturefactory.data.model.UserProfile
import com.example.furniturefactory.data.repository.OrderRepository
import com.example.furniturefactory.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val favoritesCount: Int = 0,
    val compareCount: Int = 0,
    val ordersCount: Int = 0,
)

class ProfileViewModel(private val users: UserRepository, orders: OrderRepository) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        users.profile, users.favorites, users.compare, orders.orders,
    ) { profile, fav, cmp, ord ->
        ProfileUiState(profile, fav.size, cmp.size, ord.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun logout() {
        viewModelScope.launch { users.logout() }
    }
}

class SettingsViewModel(private val users: UserRepository) : ViewModel() {
    val profile: StateFlow<UserProfile?> = users.profile
        .stateIn<UserProfile?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(name: String, email: String, phone: String, city: String, onDone: () -> Unit) {
        viewModelScope.launch {
            users.saveProfile(name.trim(), email.trim(), phone.trim(), city.trim())
            onDone()
        }
    }
}

class NotificationsViewModel(private val users: UserRepository) : ViewModel() {
    val settings: StateFlow<NotificationSettings> = users.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    fun update(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { users.saveNotifications(transform(settings.value)) }
    }
}

class OrdersViewModel(orders: OrderRepository) : ViewModel() {
    val orders: StateFlow<List<Order>?> = orders.orders
        .stateIn<List<Order>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
