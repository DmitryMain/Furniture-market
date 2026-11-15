package com.example.furniturefactory.presentation.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.FurnitureCategory
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.data.repository.UserRepository
import com.example.furniturefactory.domain.FilterOptions
import com.example.furniturefactory.domain.ProductFilter
import com.example.furniturefactory.domain.ProductQuery
import com.example.furniturefactory.domain.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val category: FurnitureCategory? = null,
    val filter: ProductFilter = ProductFilter(),
    val sort: SortOrder = SortOrder.POPULAR,
    val products: List<FurnitureProduct> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val options: FilterOptions? = null,
)

private data class CatalogQuery(
    val query: String,
    val category: FurnitureCategory?,
    val filter: ProductFilter,
    val sort: SortOrder,
)

class CatalogViewModel(
    private val repository: FurnitureRepository,
    private val users: UserRepository,
    initialCategory: FurnitureCategory?,
) : ViewModel() {

    private val params = MutableStateFlow(CatalogQuery("", initialCategory, ProductFilter(), SortOrder.POPULAR))

    val state: StateFlow<CatalogUiState> = combine(
        repository.products, params, users.favorites, repository.filterOptions,
    ) { products, p, favorites, options ->
        CatalogUiState(
            query = p.query,
            category = p.category,
            filter = p.filter,
            sort = p.sort,
            products = ProductQuery.apply(products, p.query, p.filter, p.category, p.sort),
            favorites = favorites.toSet(),
            options = options,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState(category = initialCategory))

    fun onQueryChange(value: String) {
        params.value = params.value.copy(query = value)
    }

    /** Повторное нажатие на выбранную категорию снимает выбор. */
    fun onCategoryClick(category: FurnitureCategory) {
        val current = params.value.category
        params.value = params.value.copy(category = if (current == category) null else category)
    }

    fun setCategory(category: FurnitureCategory?) {
        params.value = params.value.copy(category = category)
    }

    fun applyFilter(filter: ProductFilter) {
        params.value = params.value.copy(filter = filter)
    }

    fun setSort(sort: SortOrder) {
        params.value = params.value.copy(sort = sort)
    }

    fun resetAll() {
        params.value = CatalogQuery("", null, ProductFilter(), params.value.sort)
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch { users.toggleFavorite(productId) }
    }
}
