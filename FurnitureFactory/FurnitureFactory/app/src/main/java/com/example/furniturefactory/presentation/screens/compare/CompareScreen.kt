package com.example.furniturefactory.presentation.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.data.repository.UserRepository
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.FurnitureIcons
import com.example.furniturefactory.presentation.components.ProductImage
import com.example.furniturefactory.presentation.components.ScreenTopBar
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.formatWeight
import com.example.furniturefactory.ui.theme.FfColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompareViewModel(furniture: FurnitureRepository, private val users: UserRepository) : ViewModel() {
    val products: StateFlow<List<FurnitureProduct>?> = combine(furniture.products, users.compare) { all, ids ->
        ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }.stateIn<List<FurnitureProduct>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(id: String) {
        viewModelScope.launch { users.removeFromCompare(id) }
    }

    fun clear() {
        viewModelScope.launch { users.clearCompare() }
    }
}

private val COLUMN_WIDTH = 168.dp
private val LABEL_WIDTH = 112.dp

/** Строки сравнения: подпись → значение для товара. */
private val compareRows: List<Pair<String, (FurnitureProduct) -> String>> = listOf(
    "Цена" to { p: FurnitureProduct -> formatPrice(p.price) },
    "Размеры" to { p: FurnitureProduct -> p.dimensions.formatted },
    "Трансформация" to { p: FurnitureProduct -> p.transformedDimensions?.let { "${p.transformedLabel ?: "Да"}: ${it.formatted}" } ?: if (p.hasAnimations) "Есть" else "Нет" },
    "Материал" to { p: FurnitureProduct -> p.materialDetails },
    "Цвет" to { p: FurnitureProduct -> p.color },
    "Вес" to { p: FurnitureProduct -> formatWeight(p.weightKg) },
    "Тип" to { p: FurnitureProduct -> p.type },
    "Наличие" to { p: FurnitureProduct -> if (p.inStock) "В наличии" else "Под заказ" },
    "Артикул" to { p: FurnitureProduct -> p.articleNumber },
)

@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onGoToCatalog: () -> Unit,
) {
    val vm = appViewModel { CompareViewModel(it.furnitureRepository, it.userRepository) }
    val products by vm.products.collectAsStateWithLifecycle()
    val list = products

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Сравнение", onBack = onBack) {
            if (!list.isNullOrEmpty()) {
                TextButton(onClick = vm::clear) { Text("Очистить", color = FfColors.Ink) }
            }
        }
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FfColors.Ink)
            }
            list.isEmpty() -> EmptyState(
                icon = FurnitureIcons.Compare,
                title = "Нет моделей для сравнения",
                subtitle = "Откройте товар и нажмите значок весов рядом с названием",
                actionText = "Перейти в каталог",
                onAction = onGoToCatalog,
            )
            else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                if (list.size == 1) {
                    Text(
                        "Добавьте ещё хотя бы одну модель, чтобы сравнить характеристики",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FfColors.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.width(LABEL_WIDTH)) {
                        Spacer(Modifier.height(HEADER_HEIGHT))
                        compareRows.forEach { (label, _) ->
                            CompareCell(label, isLabel = true)
                        }
                    }
                    list.forEach { product ->
                        Column(Modifier.width(COLUMN_WIDTH).padding(horizontal = 4.dp)) {
                            CompareHeader(product, onClick = { onProductClick(product.id) }, onRemove = { vm.remove(product.id) })
                            compareRows.forEach { (_, value) -> CompareCell(value(product), isLabel = false) }
                        }
                    }
                }
            }
        }
    }
}

private val HEADER_HEIGHT = 186.dp

@Composable
private fun CompareHeader(product: FurnitureProduct, onClick: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.height(HEADER_HEIGHT).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FfColors.Beige),
        ) {
            ProductImage(product, Modifier.fillMaxSize().padding(6.dp), compactPlaceholder = true)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(FfColors.White)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Убрать из сравнения", tint = FfColors.Ink, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompareCell(text: String, isLabel: Boolean) {
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLabel) FfColors.Gray else FfColors.Ink,
            fontWeight = if (isLabel) FontWeight.Normal else FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(84.dp).padding(vertical = 10.dp, horizontal = 4.dp),
        )
        HorizontalDivider(color = FfColors.Divider)
    }
}
