package com.example.furniturefactory.data.model

/** Контактные данные и адрес доставки, введённые при оформлении. */
data class OrderContact(
    val fullName: String,
    val phone: String,
    val email: String,
    val city: String,
    val address: String,
    val comment: String,
)

/** Строка заказа хранит снимок цены и названия на момент покупки. */
data class OrderLine(
    val productId: String,
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
) {
    val total: Long get() = unitPrice * quantity
}

enum class OrderStatus(val title: String) {
    ACCEPTED("Принят"),
    IN_PRODUCTION("В производстве"),
    DELIVERING("Доставляется"),
    COMPLETED("Выполнен"),
}

data class Order(
    val number: String,
    val createdAtMillis: Long,
    val lines: List<OrderLine>,
    val contact: OrderContact,
    val status: OrderStatus = OrderStatus.ACCEPTED,
) {
    val total: Long get() = lines.sumOf { it.total }
    val itemsCount: Int get() = lines.sumOf { it.quantity }
}
