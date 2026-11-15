package com.example.furniturefactory.data.repository

import com.example.furniturefactory.data.local.DemoCatalog
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.domain.FilterOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Источник товаров. Сейчас — локальный демо-каталог; для подключения сервера завода
 * достаточно реализовать этот интерфейс (например, RemoteFurnitureRepository на Retrofit)
 * и подменить реализацию в [com.example.furniturefactory.AppContainer].
 */
interface FurnitureRepository {
    val products: StateFlow<List<FurnitureProduct>>
    fun product(id: String): Flow<FurnitureProduct?>
    fun findById(id: String): FurnitureProduct?
    val filterOptions: Flow<FilterOptions>
}

class LocalFurnitureRepository(
    initial: List<FurnitureProduct> = DemoCatalog.products,
) : FurnitureRepository {

    private val state = MutableStateFlow(initial)

    override val products: StateFlow<List<FurnitureProduct>> = state.asStateFlow()

    override fun product(id: String): Flow<FurnitureProduct?> = state.map { list -> list.firstOrNull { it.id == id } }

    override fun findById(id: String): FurnitureProduct? = state.value.firstOrNull { it.id == id }

    override val filterOptions: Flow<FilterOptions> = state.map { FilterOptions.from(it) }
}
