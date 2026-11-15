package com.example.furniturefactory.presentation.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NBSP = ' '

/** 89990 → «89 990 ₽» (неразрывные пробелы, чтобы цена не переносилась). */
fun formatPrice(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(NBSP.toString()).reversed()
    return (if (negative) "-" else "") + grouped + NBSP + "₽"
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru")).format(Date(millis))

fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

fun itemsCountText(n: Int): String = "$n ${pluralRu(n, "товар", "товара", "товаров")}"

fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()} кг" else "${"%.1f".format(Locale.forLanguageTag("ru"), kg)} кг"

fun formatSeconds(sec: Float): String {
    val s = sec.coerceAtLeast(0f)
    return "%.1f с".format(Locale.forLanguageTag("ru"), s)
}
