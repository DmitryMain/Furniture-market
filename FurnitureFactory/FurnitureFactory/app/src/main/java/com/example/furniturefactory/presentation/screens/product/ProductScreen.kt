package com.example.furniturefactory.presentation.screens.product

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.data.model.FurnitureProduct
import com.example.furniturefactory.presentation.components.AnimationPanel
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.Furniture3DViewer
import com.example.furniturefactory.presentation.components.FurnitureIcons
import com.example.furniturefactory.presentation.components.GroupCard
import com.example.furniturefactory.presentation.components.PrimaryButton
import com.example.furniturefactory.presentation.components.QuantityStepper
import com.example.furniturefactory.presentation.components.RoundIconButton
import com.example.furniturefactory.presentation.components.TransformationButton
import com.example.furniturefactory.presentation.components.ViewerQuickControls
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.formatWeight
import com.example.furniturefactory.presentation.viewer.Furniture3DViewerState
import com.example.furniturefactory.presentation.viewer.rememberFurniture3DViewerState
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun ProductScreen(
    productId: String,
    startFullscreen: Boolean,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
) {
    val vm = appViewModel(key = "product_$productId") {
        ProductViewModel(productId, it.furnitureRepository, it.cartRepository, it.userRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            when (event) {
                is ProductEvent.AddedToCart -> {
                    val result = snackbar.showSnackbar(
                        message = "Товар добавлен в корзину (${event.quantity} шт.)",
                        actionLabel = "Открыть",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onOpenCart()
                }
                is ProductEvent.Message -> snackbar.showSnackbar(event.text, duration = SnackbarDuration.Short)
            }
        }
    }

    val product = state.product
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FfColors.Ink)
        }
        product == null -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            EmptyState(
                icon = FurnitureIcons.Sofa,
                title = "Товар не найден",
                subtitle = "Возможно, он снят с производства",
                actionText = "Назад",
                onAction = onBack,
            )
        }
        else -> ProductContent(
            product = product,
            state = state,
            startFullscreen = startFullscreen,
            snackbar = snackbar,
            onBack = onBack,
            onIncrease = vm::increase,
            onDecrease = vm::decrease,
            onAddToCart = vm::addToCart,
            onToggleFavorite = vm::toggleFavorite,
            onToggleCompare = vm::toggleCompare,
        )
    }
}

@Composable
private fun ProductContent(
    product: FurnitureProduct,
    state: ProductUiState,
    startFullscreen: Boolean,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit,
) {
    var fullscreen by rememberSaveable { mutableStateOf(startFullscreen) }
    val viewer = rememberFurniture3DViewerState()
    val scroll = rememberScrollState()

    BackHandler(enabled = fullscreen) { fullscreen = false }
    ImmersiveMode(enabled = fullscreen)

    BoxWithConstraints(Modifier.fillMaxSize().background(FfColors.White)) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val normalHeight = (maxHeight * 0.5f).coerceIn(320.dp, 470.dp) + topInset
        val viewerHeight by animateDpAsState(
            targetValue = if (fullscreen) maxHeight else normalHeight,
            animationSpec = tween(320),
            label = "viewerHeight",
        )
        val corner by animateDpAsState(if (fullscreen) 0.dp else 28.dp, tween(320), label = "viewerCorner")

        // ---------- информация о товаре (прокручивается под 3D-областью)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, enabled = !fullscreen),
        ) {
            Spacer(Modifier.height(normalHeight))
            ProductDetails(
                product = product,
                viewer = viewer,
                isFavorite = state.isFavorite,
                isInCompare = state.isInCompare,
                onToggleFavorite = onToggleFavorite,
                onToggleCompare = onToggleCompare,
            )
            Spacer(Modifier.height(170.dp))
        }

        // ---------- 3D-область: в обычном режиме едет вместе с прокруткой, в полноэкранном — на весь экран
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewerHeight)
                .graphicsLayer { translationY = if (fullscreen) 0f else -scroll.value.toFloat() }
                .clip(RoundedCornerShape(bottomStart = corner, bottomEnd = corner)),
        ) {
            Furniture3DViewer(
                modelPath = product.model3DPath,
                liteModelPath = product.modelLitePath,
                animationOrder = product.animationNames,
                state = viewer,
                modifier = Modifier.fillMaxSize(),
            )
            RoundIconButton(
                icon = if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.OpenInFull,
                contentDescription = if (fullscreen) "Выйти из полноэкранного режима" else "Полноэкранный режим",
                onClick = { fullscreen = !fullscreen },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = (if (fullscreen) 0.dp else topInset) + 12.dp, end = 16.dp),
                iconSize = 18.dp,
            )
            if (viewer.isReady) {
                ViewerQuickControls(
                    state = viewer,
                    modifier = Modifier
                        .align(if (fullscreen) Alignment.TopStart else Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            top = if (fullscreen) 12.dp else 0.dp,
                            bottom = if (fullscreen) 0.dp else 16.dp,
                        ),
                )
            }
            if (fullscreen) {
                FullscreenAnimationOverlay(
                    viewer = viewer,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // ---------- кнопка «назад»
        AnimatedVisibility(
            visible = !fullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).padding(top = topInset + 12.dp, start = 16.dp),
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack,
                iconSize = 20.dp,
            )
        }

        // ---------- цена и покупка
        AnimatedVisibility(
            visible = !fullscreen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BuyBar(
                product = product,
                quantity = state.quantity,
                inCart = state.inCartQuantity,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onAddToCart = onAddToCart,
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (fullscreen) 16.dp else 150.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = FfColors.Ink,
                contentColor = FfColors.White,
                actionColor = FfColors.Sand,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetails(
    product: FurnitureProduct,
    viewer: Furniture3DViewerState,
    isFavorite: Boolean,
    isInCompare: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${product.type} · ${product.category.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = FfColors.Gray,
                )
                Spacer(Modifier.height(4.dp))
                Text(product.name, style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                    tint = FfColors.Ink,
                )
            }
            IconButton(onClick = onToggleCompare) {
                Icon(
                    FurnitureIcons.Compare,
                    contentDescription = if (isInCompare) "Убрать из сравнения" else "Сравнить",
                    tint = if (isInCompare) FfColors.Ink else FfColors.GrayLight,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(product.shortDescription, style = MaterialTheme.typography.bodyLarge, color = FfColors.Gray)
        Spacer(Modifier.height(18.dp))

        // ---------- управление трансформацией
        when {
            viewer.isReady && viewer.hasAnimations -> {
                TransformationButton(viewer, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FfColors.Beige)
                        .padding(16.dp),
                ) {
                    AnimationPanel(viewer)
                }
            }
            viewer.isReady -> InfoNote(
                if (product.hasAnimations) "В файле модели не найдено анимаций — доступен только 3D-осмотр"
                else "У этой модели нет механизма трансформации — вращайте и рассматривайте её со всех сторон",
            )
            product.hasAnimations -> InfoNote("Анимация трансформации станет доступна после загрузки 3D-модели")
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = FfColors.Gray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Проведите пальцем — вращение, двумя пальцами — масштаб, двойное касание — исходный ракурс",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
            )
        }

        Spacer(Modifier.height(26.dp))
        Text("Характеристики", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        GroupCard {
            val rows = buildList {
                add("Материал" to product.materialDetails)
                add("Цвет" to product.color)
                add("Размеры (Ш × Г × В)" to product.dimensions.formatted)
                if (product.transformedDimensions != null) {
                    add((product.transformedLabel ?: "После трансформации") to product.transformedDimensions.formatted)
                }
                add("Вес" to formatWeight(product.weightKg))
                add("Производитель" to product.manufacturer)
                add("Артикул" to product.articleNumber)
                add("Наличие" to if (product.inStock) "В наличии на складе" else "Под заказ, 2–3 недели")
            }
            rows.forEachIndexed { index, (label, value) ->
                SpecRow(label, value)
                if (index < rows.lastIndex) HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Описание", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(product.description, style = MaterialTheme.typography.bodyLarge, color = FfColors.Ink)
        if (product.features.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                product.features.forEach { feature ->
                    Text(
                        feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(FfColors.Beige)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = FfColors.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FfColors.Beige)
            .padding(16.dp),
    )
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray, modifier = Modifier.weight(0.42f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun BuyBar(
    product: FurnitureProduct,
    quantity: Int,
    inCart: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddToCart: () -> Unit,
) {
    Surface(
        color = FfColors.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.bodySmall, color = FfColors.Gray, maxLines = 1)
                    Text(formatPrice(product.price * quantity), style = MaterialTheme.typography.headlineSmall)
                    if (inCart > 0) {
                        Text("Уже в корзине: $inCart шт.", style = MaterialTheme.typography.labelSmall, color = FfColors.Gray)
                    }
                }
                QuantityStepper(quantity = quantity, onDecrease = onDecrease, onIncrease = onIncrease, height = 40.dp)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "Добавить в корзину",
                onClick = onAddToCart,
                icon = Icons.Outlined.ShoppingCart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Нижняя панель полноэкранного режима: трансформация и управление анимацией. */
@Composable
private fun FullscreenAnimationOverlay(viewer: Furniture3DViewerState, modifier: Modifier = Modifier) {
    if (!viewer.isReady || !viewer.hasAnimations) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FfColors.White.copy(alpha = 0.94f))
            .padding(16.dp),
    ) {
        TransformationButton(viewer, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        AnimationPanel(viewer)
    }
}

/** Скрывает системные панели в полноэкранном 3D-режиме. */
@Composable
private fun ImmersiveMode(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (enabled && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (enabled) controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
