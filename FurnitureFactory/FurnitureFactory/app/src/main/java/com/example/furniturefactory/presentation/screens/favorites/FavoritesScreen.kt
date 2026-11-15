package com.example.furniturefactory.presentation.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.data.repository.UserRepository
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.ProductGridCard
import com.example.furniturefactory.presentation.components.ScreenTopBar
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.ui.theme.FfColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(furniture: FurnitureRepository, private val users: UserRepository) : ViewModel() {
    /** null — ещё загружается. */
    val favorites: StateFlow<List<FurnitureProduct>?> = combine(furniture.products, users.favorites) { products, ids ->
        ids.mapNotNull { id -> products.firstOrNull { it.id == id } }
    }.stateIn<List<FurnitureProduct>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(productId: String) {
        viewModelScope.launch { users.removeFavorite(productId) }
    }
}

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onGoToCatalog: () -> Unit,
) {
    val vm = appViewModel { FavoritesViewModel(it.furnitureRepository, it.userRepository) }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Избранное", onBack = onBack)
        val list = favorites
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FfColors.Ink)
            }
            list.isEmpty() -> EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "В избранном пока пусто",
                subtitle = "Нажмите на сердечко на карточке товара, чтобы сохранить модель",
                actionText = "Перейти в каталог",
                onAction = onGoToCatalog,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        isFavorite = true,
                        onClick = { onProductClick(product.id) },
                        onToggleFavorite = { vm.remove(product.id) },
                    )
                }
            }
        }
    }
}
