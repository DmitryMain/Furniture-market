package com.example.furniturefactory.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.furniturefactory.data.model.NotificationSettings
import com.example.furniturefactory.data.model.Order
import com.example.furniturefactory.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "furniture_factory")

/**
 * Локальное хранилище состояния (DataStore Preferences): корзина, избранное, сравнение,
 * заказы, профиль и настройки уведомлений. Всё сохраняется между запусками.
 */
class AppPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val CART = stringPreferencesKey("cart")
        val FAVORITES = stringPreferencesKey("favorites")
        val COMPARE = stringPreferencesKey("compare")
        val ORDERS = stringPreferencesKey("orders")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
        val PROFILE_PHONE = stringPreferencesKey("profile_phone")
        val PROFILE_CITY = stringPreferencesKey("profile_city")
        val PROFILE_GUEST = booleanPreferencesKey("profile_guest")
        val NOTIFY_ORDERS = booleanPreferencesKey("notify_orders")
        val NOTIFY_PROMO = booleanPreferencesKey("notify_promo")
        val NOTIFY_COLLECTIONS = booleanPreferencesKey("notify_collections")
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // ---------- корзина

    val cart: Flow<Map<String, Int>> = data.map { CartCodec.decode(it[Keys.CART]) }.distinctUntilChanged()

    suspend fun updateCart(transform: (Map<String, Int>) -> Map<String, Int>) {
        store.edit { prefs ->
            val current = CartCodec.decode(prefs[Keys.CART])
            prefs[Keys.CART] = CartCodec.encode(transform(current))
        }
    }

    // ---------- избранное и сравнение (порядок важен — храним как список)

    val favorites: Flow<List<String>> = data.map { IdListCodec.decode(it[Keys.FAVORITES]) }.distinctUntilChanged()
    val compare: Flow<List<String>> = data.map { IdListCodec.decode(it[Keys.COMPARE]) }.distinctUntilChanged()

    suspend fun updateFavorites(transform: (List<String>) -> List<String>) = updateList(Keys.FAVORITES, transform)
    suspend fun updateCompare(transform: (List<String>) -> List<String>) = updateList(Keys.COMPARE, transform)

    private suspend fun updateList(key: Preferences.Key<String>, transform: (List<String>) -> List<String>) {
        store.edit { prefs -> prefs[key] = IdListCodec.encode(transform(IdListCodec.decode(prefs[key]))) }
    }

    // ---------- заказы

    val orders: Flow<List<Order>> = data.map { OrderJson.decodeList(it[Keys.ORDERS]) }.distinctUntilChanged()

    suspend fun addOrder(order: Order) {
        store.edit { prefs ->
            val list = OrderJson.decodeList(prefs[Keys.ORDERS])
            prefs[Keys.ORDERS] = OrderJson.encodeList(listOf(order) + list)
        }
    }

    // ---------- профиль

    val profile: Flow<UserProfile> = data.map { p ->
        val guest = p[Keys.PROFILE_GUEST] ?: false
        if (guest) {
            UserProfile.Guest
        } else {
            UserProfile(
                name = p[Keys.PROFILE_NAME] ?: UserProfile.Demo.name,
                email = p[Keys.PROFILE_EMAIL] ?: UserProfile.Demo.email,
                phone = p[Keys.PROFILE_PHONE] ?: "",
                city = p[Keys.PROFILE_CITY] ?: "",
                isGuest = false,
            )
        }
    }.distinctUntilChanged()

    suspend fun saveProfile(name: String, email: String, phone: String, city: String) {
        store.edit { p ->
            p[Keys.PROFILE_NAME] = name
            p[Keys.PROFILE_EMAIL] = email
            p[Keys.PROFILE_PHONE] = phone
            p[Keys.PROFILE_CITY] = city
            p[Keys.PROFILE_GUEST] = false
        }
    }

    suspend fun logout() {
        store.edit { p ->
            p.remove(Keys.PROFILE_NAME)
            p.remove(Keys.PROFILE_EMAIL)
            p.remove(Keys.PROFILE_PHONE)
            p.remove(Keys.PROFILE_CITY)
            p[Keys.PROFILE_GUEST] = true
        }
    }

    // ---------- уведомления

    val notifications: Flow<NotificationSettings> = data.map { p ->
        NotificationSettings(
            orderStatus = p[Keys.NOTIFY_ORDERS] ?: true,
            promotions = p[Keys.NOTIFY_PROMO] ?: false,
            newCollections = p[Keys.NOTIFY_COLLECTIONS] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun saveNotifications(settings: NotificationSettings) {
        store.edit { p ->
            p[Keys.NOTIFY_ORDERS] = settings.orderStatus
            p[Keys.NOTIFY_PROMO] = settings.promotions
            p[Keys.NOTIFY_COLLECTIONS] = settings.newCollections
        }
    }
}
