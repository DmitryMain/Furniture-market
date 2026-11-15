package com.example.furniturefactory.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.domain.FilterOptions
import com.example.furniturefactory.domain.ProductFilter
import com.example.furniturefactory.domain.ProductQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val query: String = "",
    val filter: ProductFilter = ProductFilter(),
    val popular: List<FurnitureProduct> = emptyList(),
    val results: List<FurnitureProduct> = emptyList(),
    val options: FilterOptions? = null,
) {
    /** Пользователь ищет или фильтрует — показываем результаты по всему каталогу. */
    val isSearching: Boolean get() = query.isNotBlank() || filter.isActive
}

class HomeViewModel(repository: FurnitureRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ProductFilter())

    val state: StateFlow<HomeUiState> = combine(
        repository.products, query, filter, repository.filterOptions,
    ) { products, q, f, options ->
        HomeUiState(
            query = q,
            filter = f,
            popular = products.filter { it.isPopular }.sortedByDescending { it.popularity },
            results = if (q.isNotBlank() || f.isActive) ProductQuery.apply(products, q, f) else emptyList(),
            options = options,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun applyFilter(value: ProductFilter) {
        filter.value = value
    }

    fun resetSearch() {
        query.value = ""
        filter.value = ProductFilter()
    }
}
