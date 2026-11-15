package com.example.furniturefactory.data.repository

import com.example.furniturefactory.data.local.AppPreferences
import com.example.furniturefactory.data.model.NotificationSettings
import com.example.furniturefactory.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Профиль, избранное, сравнение и настройки пользователя. */
interface UserRepository {
    val profile: Flow<UserProfile>
    val favorites: Flow<List<String>>
    val compare: Flow<List<String>>
    val notifications: Flow<NotificationSettings>

    suspend fun toggleFavorite(productId: String)
    suspend fun removeFavorite(productId: String)

    /** @return false, если список сравнения переполнен. */
    suspend fun toggleCompare(productId: String): CompareResult
    suspend fun removeFromCompare(productId: String)
    suspend fun clearCompare()

    suspend fun saveProfile(name: String, email: String, phone: String, city: String)
    suspend fun logout()
    suspend fun saveNotifications(settings: NotificationSettings)
}

enum class CompareResult { ADDED, REMOVED, LIMIT_REACHED }

class LocalUserRepository(private val prefs: AppPreferences) : UserRepository {

    override val profile = prefs.profile
    override val favorites = prefs.favorites
    override val compare = prefs.compare
    override val notifications = prefs.notifications

    override suspend fun toggleFavorite(productId: String) {
        prefs.updateFavorites { list -> if (productId in list) list - productId else list + productId }
    }

    override suspend fun removeFavorite(productId: String) {
        prefs.updateFavorites { it - productId }
    }

    override suspend fun toggleCompare(productId: String): CompareResult {
        var result = CompareResult.ADDED
        prefs.updateCompare { list ->
            when {
                productId in list -> {
                    result = CompareResult.REMOVED
                    list - productId
                }
                list.size >= MAX_COMPARE -> {
                    result = CompareResult.LIMIT_REACHED
                    list
                }
                else -> list + productId
            }
        }
        return result
    }

    override suspend fun removeFromCompare(productId: String) {
        prefs.updateCompare { it - productId }
    }

    override suspend fun clearCompare() {
        prefs.updateCompare { emptyList() }
    }

    override suspend fun saveProfile(name: String, email: String, phone: String, city: String) =
        prefs.saveProfile(name, email, phone, city)

    override suspend fun logout() = prefs.logout()

    override suspend fun saveNotifications(settings: NotificationSettings) = prefs.saveNotifications(settings)

    companion object {
        const val MAX_COMPARE = 4
    }
}
