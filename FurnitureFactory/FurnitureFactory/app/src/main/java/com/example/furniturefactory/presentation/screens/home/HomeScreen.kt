package com.example.furniturefactory.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.FilterBottomSheet
import com.example.furniturefactory.presentation.components.FilterButton
import com.example.furniturefactory.presentation.components.ProductCardLarge
import com.example.furniturefactory.presentation.components.SearchBar
import com.example.furniturefactory.presentation.components.SectionHeader
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onOpen3D: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.furnitureRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val list = if (state.isSearching) state.results else state.popular

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "search") {
            SearchBar(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                onFilterClick = { showFilters = true },
                activeFilters = state.filter.activeCount,
            )
        }
        item(key = "filters") {
            Spacer(Modifier.height(12.dp))
            FilterButton(onClick = { showFilters = true }, activeCount = state.filter.activeCount)
            Spacer(Modifier.height(24.dp))
        }
        item(key = "header") {
            if (state.isSearching) {
                SectionHeader("Результаты поиска") {
                    Text(itemsCountText(list.size), style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
                }
            } else {
                SectionHeader("Популярные модели") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onSeeAll)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Все", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Весь каталог", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (state.isSearching && list.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Ничего не найдено",
                    subtitle = "Попробуйте изменить запрос или сбросить фильтры",
                    actionText = "Сбросить",
                    onAction = vm::resetSearch,
                )
            }
        }
        items(list, key = { it.id }) { product ->
            ProductCardLarge(
                product = product,
                onClick = { onProductClick(product.id) },
                onOpen3D = { onOpen3D(product.id) },
                modifier = Modifier.padding(bottom = 18.dp),
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
        )
    }
}
