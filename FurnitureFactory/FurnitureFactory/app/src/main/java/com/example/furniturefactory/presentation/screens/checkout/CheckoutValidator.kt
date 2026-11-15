package com.example.furniturefactory.presentation.screens.checkout

data class CheckoutForm(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val address: String = "",
    val comment: String = "",
)

enum class CheckoutField { NAME, PHONE, EMAIL, CITY, ADDRESS }

object CheckoutValidator {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validate(form: CheckoutForm): Map<CheckoutField, String> = buildMap {
        if (form.fullName.trim().length < 2) put(CheckoutField.NAME, "Укажите имя и фамилию")
        val digits = form.phone.count { it.isDigit() }
        if (digits !in 10..15) put(CheckoutField.PHONE, "Введите номер телефона, например +7 900 123-45-67")
        if (!EMAIL.matches(form.email.trim())) put(CheckoutField.EMAIL, "Некорректный адрес электронной почты")
        if (form.city.isBlank()) put(CheckoutField.CITY, "Укажите город")
        if (form.address.trim().length < 5) put(CheckoutField.ADDRESS, "Укажите улицу, дом и квартиру")
    }
}
