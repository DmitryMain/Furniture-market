package com.example.furniturefactory.data.model

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val city: String,
    val isGuest: Boolean,
) {
    companion object {
        val Demo = UserProfile(
            name = "Петр Петров",
            email = "petr12345@gmail.com",
            phone = "",
            city = "",
            isGuest = false,
        )
        val Guest = UserProfile(name = "Гость", email = "Войдите, чтобы сохранять данные", phone = "", city = "", isGuest = true)
    }
}

data class NotificationSettings(
    val orderStatus: Boolean = true,
    val promotions: Boolean = false,
    val newCollections: Boolean = true,
)
