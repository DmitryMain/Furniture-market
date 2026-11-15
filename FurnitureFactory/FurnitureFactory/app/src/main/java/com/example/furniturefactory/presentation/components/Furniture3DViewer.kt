package com.example.furniturefactory.presentation.components

import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.furniturefactory.presentation.viewer.DeviceProfile
import com.example.furniturefactory.presentation.viewer.FilamentSceneHost
import com.example.furniturefactory.presentation.viewer.Furniture3DViewerState
import com.example.furniturefactory.presentation.viewer.ModelAssetCache
import com.example.furniturefactory.presentation.viewer.ModelLoadState
import com.example.furniturefactory.presentation.viewer.rememberFurniture3DViewerState
import com.example.furniturefactory.ui.theme.FfColors
import kotlinx.coroutines.CancellationException
import java.io.FileNotFoundException

/**
 * Универсальный компонент 3D-просмотра мебели (Filament + gltfio).
 *
 * ```
 * Furniture3DViewer(modelPath = "models/sofa_komfort.glb")
 * ```
 *
 *  * асинхронная загрузка GLB (с кэшем), индикатор загрузки и экран ошибки с повтором;
 *  * вращение пальцем, масштаб двумя пальцами, двойное касание — сброс камеры;
 *  * glTF-анимации из файла управляются через [state] (см. [Furniture3DViewerState]);
 *  * ресурсы движка освобождаются, когда компонент уходит с экрана; при сворачивании
 *    приложения отрисовка приостанавливается.
 *
 * @param liteModelPath облегчённая версия модели — используется на слабых устройствах.
 * @param animationOrder желаемый порядок клипов (имена из Blender).
 */
@Composable
fun Furniture3DViewer(
    modelPath: String,
    modifier: Modifier = Modifier,
    state: Furniture3DViewerState = rememberFurniture3DViewerState(),
    liteModelPath: String? = null,
    animationOrder: List<String> = emptyList(),
    background: Color = FfColors.Beige,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lowEnd = remember { DeviceProfile.isLowEnd(context) }
    val path = if (lowEnd && liteModelPath != null) liteModelPath else modelPath
    var host by remember { mutableStateOf<FilamentSceneHost?>(null) }
    var engineUnavailable by remember { mutableStateOf(false) }

    Box(modifier.background(background)) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { textureView ->
                    val created = FilamentSceneHost.createOrNull(ctx, textureView, state, lowEnd)
                    if (created == null) engineUnavailable = true
                    host = created
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Слой жестов поверх 3D: перехватывает касания, чтобы прокрутка экрана не мешала вращению.
        Box(
            Modifier
                .fillMaxSize()
                .viewerGestures(state),
        )

        val load = state.loadState
        AnimatedVisibility(
            visible = load is ModelLoadState.Loading && !engineUnavailable,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = FfColors.Ink, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text("Загрузка 3D-модели…", style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
            }
        }

        if (engineUnavailable) {
            ViewerMessage(
                title = "3D-просмотр недоступен на этом устройстве",
                details = "Требуется поддержка OpenGL ES 3.0",
                onRetry = null,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (load is ModelLoadState.Error) {
            ViewerMessage(
                title = load.title,
                details = load.details,
                onRetry = { state.retry() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    // Загрузка модели: при появлении движка, смене пути или нажатии «Повторить».
    LaunchedEffect(host, path, state.reloadToken) {
        val h = host ?: return@LaunchedEffect
        state.onLoadingStarted()
        try {
            val bytes = ModelAssetCache.load(context, path)
            if (!h.loadModel(bytes, animationOrder)) {
                state.onError("Файл 3D-модели повреждён", "Формат не распознан как glTF/GLB: $path")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FileNotFoundException) {
            state.onError("3D-модель не найдена", "Ожидался файл assets/$path")
        } catch (e: Exception) {
            state.onError("Не удалось загрузить 3D-модель", e.message)
        }
    }

    // Пауза/возобновление отрисовки вместе с жизненным циклом экрана.
    DisposableEffect(host, lifecycleOwner) {
        val h = host
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> h?.resume()
                Lifecycle.Event.ON_PAUSE -> h?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Освобождение нативных ресурсов Filament.
    DisposableEffect(host) {
        val h = host
        onDispose { h?.destroy() }
    }
}

private fun Modifier.viewerGestures(state: Furniture3DViewerState): Modifier = this
    .pointerInput(state) {
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pastSlop = false
            var accumulated = Offset.Zero
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                if (!pastSlop) {
                    accumulated += pan
                    if (accumulated.getDistance() > slop || zoom != 1f) {
                        pastSlop = true
                        state.camera.interacting = true
                    }
                }
                if (pastSlop) {
                    if (pressed >= 2 && zoom != 1f) state.camera.onPinch(zoom)
                    if (pressed == 1 && pan != Offset.Zero) state.camera.onDrag(pan.x, pan.y, size.height.toFloat())
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
            state.camera.interacting = false
        }
    }
    .pointerInput(state) {
        detectTapGestures(onDoubleTap = { state.resetCamera() })
    }

@Composable
private fun ViewerMessage(
    title: String,
    details: String?,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 320.dp)
            .background(FfColors.White, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        if (!details.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(details, style = MaterialTheme.typography.bodySmall, color = FfColors.Gray, textAlign = TextAlign.Center)
        }
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Повторить", color = FfColors.Ink, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
