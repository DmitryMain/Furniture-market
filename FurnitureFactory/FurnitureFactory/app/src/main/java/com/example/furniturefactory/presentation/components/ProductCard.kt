package com.example.furniturefactory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.ui.theme.FfColors

/**
 * Большая карточка «Популярные модели» на главном экране: изображение на бежевом фоне,
 * бейдж 3D, кнопка открытия 3D во весь экран, название, описание и цена.
 */
@Composable
fun ProductCardLarge(
    product: FurnitureProduct,
    onClick: () -> Unit,
    onOpen3D: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .clip(RoundedCornerShape(22.dp))
                .background(FfColors.Beige),
        ) {
            ProductImage(product, Modifier.fillMaxSize().padding(12.dp))
            Badge3D(Modifier.align(Alignment.TopEnd).padding(12.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FfColors.White.copy(alpha = 0.92f))
                    .clickable(onClick = onOpen3D),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.OpenInFull, contentDescription = "Открыть 3D во весь экран", tint = FfColors.Ink, modifier = Modifier.size(18.dp))
            }
            if (!product.inStock) StockTag(Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                product.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = FfColors.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(formatPrice(product.price), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Карточка сетки каталога (2 колонки). */
@Composable
fun ProductGridCard(
    product: FurnitureProduct,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x14000000), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.White)
            .border(1.dp, FfColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .padding(6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FfColors.Beige),
        ) {
            ProductImage(product, Modifier.fillMaxSize().padding(6.dp))
            Badge3D(Modifier.align(Alignment.TopEnd).padding(8.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                    tint = FfColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (!product.inStock) StockTag(Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                product.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(formatPrice(product.price), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StockTag(modifier: Modifier = Modifier) {
    Text(
        "Под заказ",
        style = MaterialTheme.typography.labelSmall,
        color = FfColors.White,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FfColors.Ink.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
