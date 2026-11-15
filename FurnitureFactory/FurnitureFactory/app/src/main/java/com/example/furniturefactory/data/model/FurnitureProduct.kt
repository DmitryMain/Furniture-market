package com.example.furniturefactory.data.model

enum class FurnitureCategory(val title: String) {
    SOFAS("Диваны"),
    ARMCHAIRS("Кресла"),
    TABLES("Столы"),
    CHAIRS("Стулья"),
    WARDROBES("Шкафы"),
    DRESSERS("Комоды"),
}

/** Габариты в сантиметрах: ширина × глубина × высота. */
data class Dimensions(val widthCm: Int, val depthCm: Int, val heightCm: Int) {
    val formatted: String get() = "$widthCm × $depthCm × $heightCm см"
}

/**
 * Товар каталога.
 *
 * @param imageUrl путь к миниатюре в assets (например, «images/sofa_komfort.png») или URL.
 * @param model3DPath путь к GLB-модели в assets («models/sofa_komfort.glb»).
 * @param modelLitePath облегчённая версия модели для слабых устройств (необязательно).
 * @param animationNames имена анимационных клипов внутри GLB — в том порядке, в котором
 *   их показывает приложение. Пустой список — у товара нет трансформации.
 */
data class FurnitureProduct(
    val id: String,
    val name: String,
    val shortDescription: String,
    val description: String,
    val price: Long,
    val category: FurnitureCategory,
    val type: String,
    val imageUrl: String,
    val model3DPath: String,
    val modelLitePath: String? = null,
    val animationNames: List<String> = emptyList(),
    val dimensions: Dimensions,
    val transformedDimensions: Dimensions? = null,
    val transformedLabel: String? = null,
    val weightKg: Double,
    val material: String,
    val materialDetails: String,
    val color: String,
    val manufacturer: String,
    val articleNumber: String,
    val inStock: Boolean = true,
    val popularity: Int = 0,
    val addedAtEpochDay: Long = 0,
    val isPopular: Boolean = false,
    val features: List<String> = emptyList(),
) {
    val hasAnimations: Boolean get() = animationNames.isNotEmpty()
}
