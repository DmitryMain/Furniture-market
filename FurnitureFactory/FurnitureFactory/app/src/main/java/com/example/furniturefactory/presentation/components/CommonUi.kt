package com.example.furniturefactory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.ui.theme.FfColors

/** Путь из каталога → модель для Coil: относительные пути считаются файлами из assets. */
fun productImageModel(path: String): String =
    if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) path
    else "file:///android_asset/$path"

/** Изображение товара; при отсутствии файла — аккуратная заглушка «Фото временно недоступно». */
@Composable
fun ProductImage(
    product: FurnitureProduct,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    compactPlaceholder: Boolean = false,
) {
    SubcomposeAsyncImage(
        model = productImageModel(product.imageUrl),
        contentDescription = product.name,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().background(FfColors.Beige)) },
        error = { PhotoPlaceholder(compact = compactPlaceholder) },
    )
}

@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier = modifier.fillMaxSize().background(FfColors.Beige),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Image,
            contentDescription = null,
            tint = FfColors.Gray,
            modifier = Modifier.size(if (compact) 24.dp else 40.dp),
        )
        if (!compact) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Фото временно\nнедоступно",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Маленький бейдж «3D» на карточке. */
@Composable
fun Badge3D(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(10.dp), clip = false)
            .background(FfColors.White, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("3D", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FfColors.Ink)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FfColors.Ink,
    background: Color = FfColors.White,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(3.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Счётчик количества «− 1 +» на бежевой плашке. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 99,
    height: Dp = 36.dp,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(FfColors.Beige),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease, enabled = quantity > minQuantity, modifier = Modifier.size(height)) {
            Icon(
                Icons.Filled.Remove, contentDescription = "Уменьшить",
                tint = if (quantity > minQuantity) FfColors.Ink else FfColors.GrayLight,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrease, enabled = quantity < maxQuantity, modifier = Modifier.size(height)) {
            Icon(Icons.Filled.Add, contentDescription = "Увеличить", tint = FfColors.Ink, modifier = Modifier.size(18.dp))
        }
    }
}

/** Основная чёрная кнопка-«таблетка». */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = FfColors.Ink,
            contentColor = FfColors.White,
            disabledContainerColor = FfColors.GrayLight,
            disabledContentColor = FfColors.White,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, FfColors.Ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FfColors.Ink),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Верхняя панель вложенных экранов: стрелка «назад» и заголовок. */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = FfColors.Ink)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            maxLines = 1,
        )
        actions()
    }
}

/** Пустое состояние с иллюстрацией. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(FfColors.Beige, CircleShape)
                .border(10.dp, FfColors.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(78.dp).background(FfColors.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray, textAlign = TextAlign.Center)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(actionText, onClick = onAction, modifier = Modifier.fillMaxWidth(0.8f))
        }
    }
}

/** Белая карточка-группа с тонкой рамкой (меню профиля, характеристики). */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.White)
            .border(1.dp, FfColors.Border, RoundedCornerShape(18.dp)),
    ) {
        content()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}
