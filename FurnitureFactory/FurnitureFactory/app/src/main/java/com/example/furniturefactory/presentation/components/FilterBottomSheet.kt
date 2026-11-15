package com.example.furniturefactory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.furniturefactory.domain.FilterOptions
import com.example.furniturefactory.domain.ProductFilter
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.ui.theme.FfColors
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * Фильтры каталога в BottomSheet: категория, цена от/до, материал, цвет, тип, наличие.
 * Изменения применяются только по кнопке «Применить».
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    initial: ProductFilter,
    options: FilterOptions,
    onApply: (ProductFilter) -> Unit,
    onDismiss: () -> Unit,
    showCategories: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(initial) }
    val minP = options.minPrice.toFloat()
    val maxP = options.maxPrice.toFloat().coerceAtLeast(minP + 1f)
    var range by remember {
        mutableStateOf((initial.priceFrom?.toFloat() ?: minP)..(initial.priceTo?.toFloat() ?: maxP))
    }

    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FfColors.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text("Фильтры", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (showCategories) {
                FilterSectionTitle("Категория мебели")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.categories.forEach { c ->
                        FilterChipItem(
                            text = c.title,
                            selected = c in draft.categories,
                            icon = { Icon(FurnitureIcons.forCategory(c), null, modifier = Modifier.size(18.dp)) },
                        ) { draft = draft.copy(categories = draft.categories.toggle(c)) }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            FilterSectionTitle("Цена")
            Row(Modifier.fillMaxWidth()) {
                PriceBox("от", formatPrice(range.start.roundToLong()), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PriceBox("до", formatPrice(range.endInclusive.roundToLong()), Modifier.weight(1f))
            }
            RangeSlider(
                value = range,
                onValueChange = { r -> range = r },
                valueRange = minP..maxP,
                colors = SliderDefaults.colors(
                    thumbColor = FfColors.Ink,
                    activeTrackColor = FfColors.Ink,
                    inactiveTrackColor = FfColors.BeigeDeep,
                ),
            )
            Spacer(Modifier.height(12.dp))

            FilterSectionTitle("Материал")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.materials.forEach { m ->
                    FilterChipItem(m, m in draft.materials) { draft = draft.copy(materials = draft.materials.toggle(m)) }
                }
            }
            Spacer(Modifier.height(20.dp))

            FilterSectionTitle("Цвет")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.colors.forEach { c ->
                    FilterChipItem(c, c in draft.colors) { draft = draft.copy(colors = draft.colors.toggle(c)) }
                }
            }
            Spacer(Modifier.height(20.dp))

            FilterSectionTitle("Тип мебели")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.types.forEach { t ->
                    FilterChipItem(t, t in draft.types) { draft = draft.copy(types = draft.types.toggle(t)) }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FfColors.Beige)
                    .clickable { draft = draft.copy(inStockOnly = !draft.inStockOnly) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Только в наличии на складе", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Switch(
                    checked = draft.inStockOnly,
                    onCheckedChange = { draft = draft.copy(inStockOnly = it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = FfColors.Ink, checkedThumbColor = FfColors.White),
                )
            }
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth()) {
                SecondaryButton("Сбросить", onClick = {
                    draft = ProductFilter()
                    range = minP..maxP
                }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                PrimaryButton("Применить", onClick = {
                    val from = range.start.roundToLong().takeIf { it > options.minPrice }
                    val to = range.endInclusive.roundToLong().takeIf { it < options.maxPrice }
                    val result = draft.copy(priceFrom = from, priceTo = to)
                    close { onApply(result) }
                }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun PriceBox(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, FfColors.Divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun FilterChipItem(
    text: String,
    selected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) FfColors.Ink else FfColors.Beige)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides if (selected) FfColors.White else FfColors.Ink,
            ) { icon() }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) FfColors.White else FfColors.Ink,
        )
    }
}
