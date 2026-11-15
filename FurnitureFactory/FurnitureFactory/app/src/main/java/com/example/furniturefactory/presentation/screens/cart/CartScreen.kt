package com.example.furniturefactory.presentation.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.data.model.CartItem
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.PrimaryButton
import com.example.furniturefactory.presentation.components.ProductImage
import com.example.furniturefactory.presentation.components.QuantityStepper
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    onGoToCatalog: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val vm = appViewModel { CartViewModel(it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val summary = state.summary

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Корзина", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (!summary.isEmpty) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Очистить корзину", tint = FfColors.Ink)
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FfColors.Ink)
            }
            summary.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Ваша корзина пока пуста",
                    subtitle = "Добавьте понравившуюся мебель из каталога",
                    actionText = "Перейти в каталог",
                    onAction = onGoToCatalog,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(summary.items, key = { it.product.id }) { item ->
                        CartRow(
                            item = item,
                            onClick = { onProductClick(item.product.id) },
                            onIncrease = { vm.increase(item.product.id, item.quantity) },
                            onDecrease = { vm.decrease(item.product.id, item.quantity) },
                            onRemove = { vm.remove(item.product.id) },
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    HorizontalDivider(color = FfColors.Divider)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Итого:", style = MaterialTheme.typography.titleMedium)
                            Text(itemsCountText(summary.totalQuantity), style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                        }
                        Text(formatPrice(summary.totalPrice), style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Оформить заказ", onClick = onCheckout, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить корзину?") },
            text = { Text("Все товары будут удалены из корзины.") },
            confirmButton = {
                TextButton(onClick = { vm.clear(); confirmClear = false }) { Text("Очистить", color = FfColors.Ink) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена", color = FfColors.Gray) }
            },
            containerColor = FfColors.White,
        )
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(width = 112.dp, height = 96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FfColors.Beige),
        ) {
            ProductImage(item.product, Modifier.fillMaxSize().padding(6.dp), compactPlaceholder = true)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Удалить", tint = FfColors.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Text(formatPrice(item.total), style = MaterialTheme.typography.titleMedium)
            if (item.quantity > 1) {
                Text(
                    "${formatPrice(item.product.price)} × ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FfColors.Gray,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                QuantityStepper(quantity = item.quantity, onDecrease = onDecrease, onIncrease = onIncrease)
            }
        }
    }
}
