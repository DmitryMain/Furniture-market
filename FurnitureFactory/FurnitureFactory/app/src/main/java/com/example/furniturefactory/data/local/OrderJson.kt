package com.example.furniturefactory.data.local

import com.example.furniturefactory.data.model.Order
import com.example.furniturefactory.data.model.OrderContact
import com.example.furniturefactory.data.model.OrderLine
import com.example.furniturefactory.data.model.OrderStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Сериализация заказов в JSON (org.json встроен в Android — без лишних зависимостей). */
object OrderJson {

    fun encodeList(orders: List<Order>): String {
        val arr = JSONArray()
        orders.forEach { arr.put(encode(it)) }
        return arr.toString()
    }

    fun decodeList(raw: String?): List<Order> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> runCatching { decode(arr.getJSONObject(i)) }.getOrNull() }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun encode(order: Order): JSONObject = JSONObject().apply {
        put("number", order.number)
        put("createdAt", order.createdAtMillis)
        put("status", order.status.name)
        put("contact", JSONObject().apply {
            put("fullName", order.contact.fullName)
            put("phone", order.contact.phone)
            put("email", order.contact.email)
            put("city", order.contact.city)
            put("address", order.contact.address)
            put("comment", order.contact.comment)
        })
        put("lines", JSONArray().apply {
            order.lines.forEach { line ->
                put(JSONObject().apply {
                    put("productId", line.productId)
                    put("name", line.name)
                    put("unitPrice", line.unitPrice)
                    put("quantity", line.quantity)
                })
            }
        })
    }

    private fun decode(o: JSONObject): Order {
        val c = o.getJSONObject("contact")
        val linesArr = o.getJSONArray("lines")
        val lines = (0 until linesArr.length()).map { i ->
            val l = linesArr.getJSONObject(i)
            OrderLine(
                productId = l.getString("productId"),
                name = l.getString("name"),
                unitPrice = l.getLong("unitPrice"),
                quantity = l.getInt("quantity"),
            )
        }
        return Order(
            number = o.getString("number"),
            createdAtMillis = o.getLong("createdAt"),
            status = runCatching { OrderStatus.valueOf(o.getString("status")) }.getOrDefault(OrderStatus.ACCEPTED),
            contact = OrderContact(
                fullName = c.optString("fullName"),
                phone = c.optString("phone"),
                email = c.optString("email"),
                city = c.optString("city"),
                address = c.optString("address"),
                comment = c.optString("comment"),
            ),
            lines = lines,
        )
    }
}
