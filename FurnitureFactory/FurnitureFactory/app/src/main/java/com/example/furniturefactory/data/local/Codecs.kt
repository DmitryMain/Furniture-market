package com.example.furniturefactory.data.local

/** Корзина хранится строкой «id:количество;id:количество». */
object CartCodec {
    fun encode(map: Map<String, Int>): String =
        map.filter { it.value > 0 }.entries.joinToString(";") { "${it.key}:${it.value}" }

    fun decode(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<String, Int>()
        raw.split(';').forEach { entry ->
            val parts = entry.split(':')
            val qty = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && parts[0].isNotBlank() && qty != null && qty > 0) result[parts[0]] = qty
        }
        return result
    }
}

object IdListCodec {
    fun encode(ids: List<String>): String = ids.distinct().joinToString(",")
    fun decode(raw: String?): List<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
}
