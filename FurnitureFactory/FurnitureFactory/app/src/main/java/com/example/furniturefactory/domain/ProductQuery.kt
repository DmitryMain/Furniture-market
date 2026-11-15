package com.example.furniturefactory.domain

import com.example.furniturefactory.data.model.FurnitureCategory
import com.example.furniturefactory.data.model.FurnitureProduct

data class ProductFilter(
    val categories: Set<FurnitureCategory> = emptySet(),
    val priceFrom: Long? = null,
    val priceTo: Long? = null,
    val materials: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val inStockOnly: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(
            categories.isNotEmpty(),
            priceFrom != null || priceTo != null,
            materials.isNotEmpty(),
            colors.isNotEmpty(),
            types.isNotEmpty(),
            inStockOnly,
        ).count { it }

    val isActive: Boolean get() = activeCount > 0
}

enum class SortOrder(val title: String) {
    POPULAR("По популярности"),
    PRICE_ASC("Сначала дешевле"),
    PRICE_DESC("Сначала дороже"),
    NEWEST("По новизне"),
}

/** Значения, доступные в фильтрах — собираются из каталога. */
data class FilterOptions(
    val categories: List<FurnitureCategory>,
    val materials: List<String>,
    val colors: List<String>,
    val types: List<String>,
    val minPrice: Long,
    val maxPrice: Long,
) {
    companion object {
        fun from(products: List<FurnitureProduct>) = FilterOptions(
            categories = FurnitureCategory.entries.toList(),
            materials = products.map { it.material }.distinct().sorted(),
            colors = products.map { it.color }.distinct().sorted(),
            types = products.map { it.type }.distinct().sorted(),
            minPrice = products.minOfOrNull { it.price } ?: 0,
            maxPrice = products.maxOfOrNull { it.price } ?: 0,
        )
    }
}

/** Поиск, фильтрация и сортировка — чистая логика без Android-зависимостей. */
object ProductQuery {

    fun normalize(text: String): String =
        text.lowercase().replace('ё', 'е').replace("«", "").replace("»", "").replace("\"", "").trim()

    fun matchesSearch(product: FurnitureProduct, query: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val haystack = normalize(
            listOf(product.name, product.category.title, product.articleNumber, product.type, product.material, product.color)
                .joinToString(" ")
        )
        // каждое слово запроса должно встретиться (порядок не важен)
        return q.split(Regex("\\s+")).all { word -> haystack.contains(word) }
    }

    fun matchesFilter(product: FurnitureProduct, filter: ProductFilter): Boolean {
        if (filter.categories.isNotEmpty() && product.category !in filter.categories) return false
        filter.priceFrom?.let { if (product.price < it) return false }
        filter.priceTo?.let { if (product.price > it) return false }
        if (filter.materials.isNotEmpty() && product.material !in filter.materials) return false
        if (filter.colors.isNotEmpty() && product.color !in filter.colors) return false
        if (filter.types.isNotEmpty() && product.type !in filter.types) return false
        if (filter.inStockOnly && !product.inStock) return false
        return true
    }

    fun apply(
        products: List<FurnitureProduct>,
        query: String = "",
        filter: ProductFilter = ProductFilter(),
        category: FurnitureCategory? = null,
        sort: SortOrder = SortOrder.POPULAR,
    ): List<FurnitureProduct> {
        val filtered = products.filter { p ->
            (category == null || p.category == category) && matchesSearch(p, query) && matchesFilter(p, filter)
        }
        return when (sort) {
            SortOrder.POPULAR -> filtered.sortedByDescending { it.popularity }
            SortOrder.PRICE_ASC -> filtered.sortedBy { it.price }
            SortOrder.PRICE_DESC -> filtered.sortedByDescending { it.price }
            SortOrder.NEWEST -> filtered.sortedByDescending { it.addedAtEpochDay }
        }
    }
}
