package com.example.furniturefactory.presentation.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed interface ModelLoadState {
    data object Loading : ModelLoadState
    data object Ready : ModelLoadState
    data class Error(val title: String, val details: String? = null) : ModelLoadState
}

/** Анимационный клип, найденный внутри GLB. [index] — индекс в файле. */
data class AnimationClip(val index: Int, val name: String, val durationSec: Float)

/**
 * Состояние и команды 3D-просмотрщика. Создаётся в UI через [rememberFurniture3DViewerState],
 * читается движком на каждом кадре (главный поток).
 */
@Stable
class Furniture3DViewerState internal constructor() {

    val camera = OrbitCameraController()

    var loadState: ModelLoadState by mutableStateOf(ModelLoadState.Loading)
        private set

    /** Клипы анимации из файла модели (пусто — у модели нет анимаций). */
    var clips: List<AnimationClip> by mutableStateOf(emptyList())
        private set

    var selectedClipIndex: Int by mutableIntStateOf(0)
        private set

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    /** Текущее время анимации, с. */
    var time: Float by mutableFloatStateOf(0f)
        private set

    var autoRotate: Boolean by mutableStateOf(false)
        private set

    internal var reloadToken: Int by mutableIntStateOf(0)
        private set

    /** true — нужно заново применить позу анимации (после перемотки, смены клипа и т.п.). */
    internal var animationDirty = true

    val currentClip: AnimationClip? get() = clips.getOrNull(selectedClipIndex)
    val hasAnimations: Boolean get() = clips.isNotEmpty()
    val isReady: Boolean get() = loadState == ModelLoadState.Ready

    // ------------------------------------------------------------------ команды UI

    fun play() {
        val clip = currentClip ?: return
        if (time >= clip.durationSec - END_EPSILON) time = 0f
        isPlaying = true
        animationDirty = true
    }

    fun pause() {
        isPlaying = false
    }

    fun togglePlay() = if (isPlaying) pause() else play()

    /** «С начала»: перемотать в начало и воспроизвести. */
    fun restart() {
        if (currentClip == null) return
        time = 0f
        isPlaying = true
        animationDirty = true
    }

    fun seekTo(seconds: Float) {
        val clip = currentClip ?: return
        time = seconds.coerceIn(0f, clip.durationSec)
        animationDirty = true
    }

    /** Показать исходное положение клипа (например, «сложенный диван»). */
    fun showStartPose() {
        isPlaying = false
        seekTo(0f)
    }

    /** Показать конечное положение клипа (например, «полностью разложенный диван»). */
    fun showEndPose() {
        val clip = currentClip ?: return
        isPlaying = false
        seekTo(clip.durationSec)
    }

    fun selectClip(position: Int, autoPlay: Boolean = true) {
        if (position !in clips.indices) return
        selectedClipIndex = position
        time = 0f
        isPlaying = autoPlay
        animationDirty = true
    }

    /** Кнопка «Показать трансформацию» — первый клип с начала. */
    fun playTransformation() = selectClip(0, autoPlay = true)

    fun toggleAutoRotate() {
        autoRotate = !autoRotate
        camera.autoRotate = autoRotate
    }

    fun resetCamera() = camera.reset()

    /** Повторная загрузка после ошибки. */
    fun retry() {
        reloadToken++
    }

    // ------------------------------------------------------------------ вызовы движка

    internal fun onLoadingStarted() {
        loadState = ModelLoadState.Loading
        clips = emptyList()
        selectedClipIndex = 0
        isPlaying = false
        time = 0f
        animationDirty = true
    }

    internal fun onModelReady(foundClips: List<AnimationClip>, preferredOrder: List<String>) {
        clips = if (preferredOrder.isEmpty()) foundClips else foundClips.sortedBy { clip ->
            preferredOrder.indexOf(clip.name).let { if (it < 0) Int.MAX_VALUE else it }
        }
        selectedClipIndex = 0
        time = 0f
        isPlaying = false
        animationDirty = true
        loadState = ModelLoadState.Ready
    }

    internal fun onError(title: String, details: String? = null) {
        isPlaying = false
        loadState = ModelLoadState.Error(title, details)
    }

    /** Продвигает время анимации. @return true, если позу нужно применить на этом кадре. */
    internal fun advance(dtSeconds: Float): Boolean {
        val clip = currentClip ?: return false
        if (isPlaying) {
            val next = time + dtSeconds
            if (next >= clip.durationSec) {
                time = clip.durationSec
                isPlaying = false // останавливаемся в конечном положении
            } else {
                time = next
            }
            animationDirty = false
            return true
        }
        if (animationDirty) {
            animationDirty = false
            return true
        }
        return false
    }

    companion object {
        internal const val END_EPSILON = 0.02f
    }
}

@Composable
fun rememberFurniture3DViewerState(): Furniture3DViewerState = remember { Furniture3DViewerState() }
