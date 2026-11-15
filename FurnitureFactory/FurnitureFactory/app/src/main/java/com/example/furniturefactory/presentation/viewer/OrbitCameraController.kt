package com.example.furniturefactory.presentation.viewer

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * Орбитальная камера вокруг модели.
 *
 *  * один палец — вращение (влево/вправо — вокруг вертикальной оси, вверх/вниз — наклон);
 *  * два пальца — приближение/отдаление с ограничениями (модель нельзя «потерять» или «пройти насквозь»);
 *  * автоповорот 360° и плавный сброс в исходный ракурс.
 *
 * Чистый Kotlin без зависимостей от Filament — легко тестируется.
 */
class OrbitCameraController {

    // Параметры модели (мировые координаты)
    var targetX = 0f; private set
    var targetY = 0f; private set
    var targetZ = 0f; private set
    var radius = 1f; private set

    // Текущие (сглаженные) и целевые значения
    private var yaw = DEFAULT_YAW
    private var pitch = DEFAULT_PITCH
    private var zoom = 1f
    private var goalYaw = DEFAULT_YAW
    private var goalPitch = DEFAULT_PITCH
    private var goalZoom = 1f

    @Volatile var autoRotate = false
    @Volatile var interacting = false

    val currentYawDegrees: Float get() = yaw
    val currentPitchDegrees: Float get() = pitch
    val currentZoom: Float get() = zoom

    fun setModelBounds(cx: Float, cy: Float, cz: Float, r: Float) {
        targetX = cx
        targetY = cy
        targetZ = cz
        radius = r.coerceAtLeast(0.01f)
    }

    /** Смещение пальца в пикселях; [viewSizePx] — высота области, чтобы скорость не зависела от экрана. */
    fun onDrag(dxPx: Float, dyPx: Float, viewSizePx: Float) {
        val degPerPx = 220f / viewSizePx.coerceAtLeast(1f)
        goalYaw -= dxPx * degPerPx
        goalPitch = (goalPitch + dyPx * degPerPx).coerceIn(MIN_PITCH, MAX_PITCH)
    }

    /** [scale] > 1 — пальцы разводятся (приближение). */
    fun onPinch(scale: Float) {
        if (scale <= 0f) return
        goalZoom = (goalZoom / scale).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun reset() {
        // приводим текущий угол к [-180, 180), чтобы сброс шёл по короткой дуге
        val turns = kotlin.math.floor((yaw + 180f) / 360f)
        yaw -= turns * 360f
        goalYaw = DEFAULT_YAW
        goalPitch = DEFAULT_PITCH
        goalZoom = 1f
    }

    /** Мгновенно ставит камеру в исходное положение (при загрузке новой модели). */
    fun snapToDefault() {
        yaw = DEFAULT_YAW; goalYaw = DEFAULT_YAW
        pitch = DEFAULT_PITCH; goalPitch = DEFAULT_PITCH
        zoom = 1f; goalZoom = 1f
    }

    /** Шаг сглаживания. Возвращает true, если камера ещё движется. */
    fun update(dtSeconds: Float): Boolean {
        val dt = dtSeconds.coerceIn(0f, 0.1f)
        if (autoRotate && !interacting) goalYaw += AUTO_ROTATE_DEG_PER_SEC * dt
        val a = 1f - exp(-dt * SMOOTHING)
        yaw += (goalYaw - yaw) * a
        pitch += (goalPitch - pitch) * a
        zoom += (goalZoom - zoom) * a
        return kotlin.math.abs(goalYaw - yaw) > 0.01f || kotlin.math.abs(goalPitch - pitch) > 0.01f ||
            kotlin.math.abs(goalZoom - zoom) > 0.0005f || autoRotate
    }

    /**
     * Расстояние, на котором модель целиком помещается в кадр
     * (учитывается и вертикальный, и горизонтальный угол обзора).
     */
    fun fitDistance(verticalFovDeg: Float, aspect: Float): Float {
        val halfV = (verticalFovDeg / 2f) * (PI.toFloat() / 180f)
        val halfH = atan(tan(halfV) * aspect)
        val half = min(halfV, halfH)
        return radius / sin(half) * FIT_MARGIN
    }

    /** Позиция глаза камеры: [x, y, z]. */
    fun eye(verticalFovDeg: Float, aspect: Float): FloatArray {
        val d = fitDistance(verticalFovDeg, aspect) * zoom
        val yr = yaw * (PI.toFloat() / 180f)
        val pr = pitch * (PI.toFloat() / 180f)
        return floatArrayOf(
            targetX + d * sin(yr) * cos(pr),
            targetY + d * sin(pr),
            targetZ + d * cos(yr) * cos(pr),
        )
    }

    /** Ближняя/дальняя плоскости отсечения с запасом. */
    fun clipPlanes(verticalFovDeg: Float, aspect: Float): Pair<Float, Float> {
        val d = fitDistance(verticalFovDeg, aspect) * MAX_ZOOM
        return (radius * 0.02f).coerceAtLeast(0.005f) to (d + radius * 4f)
    }

    companion object {
        const val DEFAULT_YAW = -28f
        const val DEFAULT_PITCH = 14f
        const val MIN_PITCH = -8f      // чуть ниже горизонта
        const val MAX_PITCH = 82f      // почти сверху
        const val MIN_ZOOM = 0.42f     // ближе нельзя — камера не войдёт в модель
        const val MAX_ZOOM = 1.9f
        const val AUTO_ROTATE_DEG_PER_SEC = 24f
        const val SMOOTHING = 12f
        const val FIT_MARGIN = 1.04f
    }
}
