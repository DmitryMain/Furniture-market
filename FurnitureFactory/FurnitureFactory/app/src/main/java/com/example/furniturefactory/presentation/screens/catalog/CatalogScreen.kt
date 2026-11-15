package com.example.furniturefactory.presentation.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.data.model.FurnitureCategory
import com.example.furniturefactory.domain.SortOrder
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.FilterBottomSheet
import com.example.furniturefactory.presentation.components.FurnitureIcons
import com.example.furniturefactory.presentation.components.ProductGridCard
import com.example.furniturefactory.presentation.components.SearchBar
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun CatalogScreen(
    initialCategory: FurnitureCategory?,
    onProductClick: (String) -> Unit,
) {
    val vm = appViewModel { CatalogViewModel(it.furnitureRepository, it.userRepository, initialCategory) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            SearchBar(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                onFilterClick = { showFilters = true },
                activeFilters = state.filter.activeCount,
            )
        }
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Каталог",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        items(FurnitureCategory.entries, key = { "cat_${it.name}" }) { category ->
            CategoryButton(
                category = category,
                selected = category == state.category,
                onClick = { vm.onCategoryClick(category) },
            )
        }
        item(key = "sortRow", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    itemsCountText(state.products.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FfColors.Gray,
                    modifier = Modifier.weight(1f),
                )
                SortMenu(current = state.sort, onSelect = vm::setSort)
            }
        }
        if (state.products.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Ничего не найдено",
                    subtitle = "Измените запрос, категорию или фильтры",
                    actionText = "Показать всё",
                    onAction = vm::resetAll,
                )
            }
        }
        items(state.products, key = { it.id }) { product ->
            ProductGridCard(
                product = product,
                isFavorite = product.id in state.favorites,
                onClick = { onProductClick(product.id) },
                onToggleFavorite = { vm.toggleFavorite(product.id) },
            )
        }
    }

    val options = state.options
    if (showFilters && options != null) {
        FilterBottomSheet(
            initial = state.filter,
            options = options,
            onApply = { vm.applyFilter(it); showFilters = false },
            onDismiss = { showFilters = false },
            showCategories = true,
        )
    }
}

@Composable
private fun CategoryButton(category: FurnitureCategory, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) FfColors.Ink else FfColors.Beige)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            FurnitureIcons.forCategory(category),
            contentDescription = null,
            tint = if (selected) FfColors.White else FfColors.Ink,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            category.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) FfColors.White else FfColors.Ink,
        )
    }
}

@Composable
private fun SortMenu(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.title, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Сортировка", modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = FfColors.White,
        ) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.title) },
                    onClick = {
                        onSelect(order)
                        expanded = false
                    },
                    trailingIcon = {
                        if (order == current) Icon(Icons.Filled.Check, contentDescription = null, tint = FfColors.Ink)
                    },
                )
            }
        }
    }
}
