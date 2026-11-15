package com.example.furniturefactory.data.repository

import com.example.furniturefactory.data.local.AppPreferences
import com.example.furniturefactory.data.model.CartItem
import com.example.furniturefactory.data.model.Order
import com.example.furniturefactory.data.model.OrderContact
import com.example.furniturefactory.data.model.OrderLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

interface OrderRepository {
    val orders: Flow<List<Order>>

    /** Оформляет заказ. Сейчас — демо без оплаты; в будущем здесь будет запрос к API завода. */
    suspend fun placeOrder(contact: OrderContact, items: List<CartItem>): Order
}

class LocalOrderRepository(private val prefs: AppPreferences) : OrderRepository {

    override val orders: Flow<List<Order>> = prefs.orders

    override suspend fun placeOrder(contact: OrderContact, items: List<CartItem>): Order {
        require(items.isNotEmpty()) { "Корзина пуста" }
        delay(700) // имитация сетевого запроса
        val existing = prefs.orders.first().map { it.number }.toSet()
        val number = generateSequence { newNumber() }.first { it !in existing }
        val order = Order(
            number = number,
            createdAtMillis = System.currentTimeMillis(),
            lines = items.map { OrderLine(it.product.id, it.product.name, it.product.price, it.quantity) },
            contact = contact,
        )
        prefs.addOrder(order)
        return order
    }

    private fun newNumber(): String {
        val date = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        return "FF-$date-${Random.nextInt(1000, 10000)}"
    }
}
