package com.example.furniturefactory.presentation.viewer

import android.content.Context
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Однократная загрузка нативных библиотек Filament + gltfio. */
internal object FilamentRuntime {
    @Volatile
    private var initialized: Boolean? = null

    fun ensureInitialized(): Boolean {
        initialized?.let { return it }
        val ok = try {
            Gltfio.init() // загружает filament-jni и gltfio-jni
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Filament init failed", t)
            false
        }
        initialized = ok
        return ok
    }
}

private const val TAG = "Furniture3D"

/**
 * Обёртка над Filament: движок, сцена, студийный свет, камера, загрузка GLB через gltfio,
 * проигрывание glTF-анимаций и цикл отрисовки на Choreographer.
 *
 * Все методы вызываются в главном потоке. Объект владеет нативными ресурсами —
 * обязательно вызывать [destroy] (это делает [com.example.furniturefactory.presentation.components.Furniture3DViewer]).
 */
internal class FilamentSceneHost private constructor(
    context: Context,
    private val textureView: TextureView,
    private val state: Furniture3DViewerState,
    private val lowEnd: Boolean,
) : Choreographer.FrameCallback {

    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity: Int = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val displayHelper = DisplayHelper(context)
    private var swapChain: SwapChain? = null

    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)

    private val colorGrading: ColorGrading
    private val indirectLight: IndirectLight
    private val sunEntity: Int = EntityManager.get().create()
    private val fillEntity: Int = EntityManager.get().create()

    private var asset: FilamentAsset? = null
    private var animator: Animator? = null
    private var sourceBuffer: ByteBuffer? = null
    private var resourcesPending = false
    private var loadStartedAtNanos = 0L
    private var preferredClipOrder: List<String> = emptyList()

    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var destroyed = false
    private var lastFrameNanos = 0L
    private var viewportWidth = 1
    private var viewportHeight = 1

    init {
        view.scene = scene
        view.camera = camera

        // Прозрачный фон: под 3D-сценой виден бежевый фон Compose — как студийная подложка каталога.
        view.blendMode = View.BlendMode.TRANSLUCENT
        renderer.clearOptions = renderer.clearOptions.apply {
            clear = true
            clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        }

        colorGrading = ColorGrading.Builder()
            .toneMapper(ToneMapper.Filmic())
            .build(engine)
        view.colorGrading = colorGrading

        if (lowEnd) {
            view.antiAliasing = View.AntiAliasing.FXAA
            view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply { enabled = true }
        } else {
            view.antiAliasing = View.AntiAliasing.NONE
            view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
                enabled = true
                sampleCount = 4
            }
            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = true }
        }

        camera.setExposure(16f, 1f / 125f, 100f)
        indirectLight = setupStudioLighting()
        scene.indirectLight = indirectLight

        uiHelper.isOpaque = false
        uiHelper.renderCallback = SurfaceCallback()
        textureView.isOpaque = false
        uiHelper.attachTo(textureView)
    }

    // ------------------------------------------------------------------ свет

    /** Мягкий студийный свет: ключевой источник с тенями, заполняющий свет и ровный рассеянный фон. */
    private fun setupStudioLighting(): IndirectLight {
        val shadowOptions = LightManager.ShadowOptions().apply {
            mapSize = if (lowEnd) 1024 else 2048
        }
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.97f, 0.93f)
            .intensity(78_000f)
            .direction(0.42f, -0.86f, -0.30f)
            .castShadows(true)
            .shadowOptions(shadowOptions)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        // Заполняющий точечный источник спереди-справа — смягчает тени на фасаде.
        LightManager.Builder(LightManager.Type.POINT)
            .color(1.0f, 0.98f, 0.95f)
            .intensity(240_000f)
            .position(3f, 2.5f, 4f)
            .falloff(25f)
            .castShadows(false)
            .build(engine, fillEntity)
        scene.addEntity(fillEntity)

        // Равномерное рассеянное освещение (сферические гармоники 1-го порядка).
        return IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.92f, 0.90f, 0.87f))
            .intensity(28_000f)
            .build(engine)
    }

    // ------------------------------------------------------------------ жизненный цикл

    fun resume() {
        if (destroyed || running) return
        running = true
        lastFrameNanos = 0L
        choreographer.postFrameCallback(this)
    }

    fun pause() {
        running = false
        choreographer.removeFrameCallback(this)
    }

    fun destroy() {
        if (destroyed) return
        pause()
        destroyed = true
        try {
            uiHelper.detach()
            destroyModel()
            scene.indirectLight = null
            engine.destroyIndirectLight(indirectLight)
            engine.destroyEntity(sunEntity)
            engine.destroyEntity(fillEntity)
            EntityManager.get().destroy(sunEntity)
            EntityManager.get().destroy(fillEntity)
            materialProvider.destroyMaterials()
            materialProvider.destroy()
            assetLoader.destroy()
            resourceLoader.destroy()
            engine.destroyColorGrading(colorGrading)
            engine.destroyRenderer(renderer)
            engine.destroyView(view)
            engine.destroyScene(scene)
            engine.destroyCameraComponent(cameraEntity)
            EntityManager.get().destroy(cameraEntity)
            engine.destroy()
        } catch (t: Throwable) {
            Log.e(TAG, "destroy failed", t)
        }
    }

    // ------------------------------------------------------------------ модель

    /**
     * Создаёт ассет из байтов GLB и запускает асинхронную загрузку текстур.
     * @return false, если файл не является корректным glTF/GLB.
     */
    fun loadModel(bytes: ByteArray, preferredOrder: List<String>): Boolean {
        if (destroyed) return false
        destroyModel()
        state.onLoadingStarted()
        val buffer = ByteBuffer.allocateDirect(bytes.size).also {
            it.put(bytes)
            it.flip()
        }
        val newAsset = try {
            assetLoader.createAsset(buffer)
        } catch (t: Throwable) {
            Log.e(TAG, "createAsset failed", t)
            null
        } ?: return false

        if (!resourceLoader.asyncBeginLoad(newAsset)) {
            assetLoader.destroyAsset(newAsset)
            return false
        }
        newAsset.releaseSourceData()
        asset = newAsset
        sourceBuffer = buffer
        animator = newAsset.instance.animator
        preferredClipOrder = preferredOrder
        resourcesPending = true
        loadStartedAtNanos = System.nanoTime()
        return true
    }

    private fun destroyModel() {
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()
        asset?.let {
            scene.removeEntities(it.entities)
            assetLoader.destroyAsset(it)
        }
        asset = null
        animator = null
        sourceBuffer = null
        resourcesPending = false
    }

    private fun onResourcesLoaded() {
        val a = asset ?: return
        resourcesPending = false
        sourceBuffer = null

        // Стартовая поза = первый кадр первого клипа (например, сложенный диван).
        val anim = animator
        val clips = mutableListOf<AnimationClip>()
        if (anim != null) {
            for (i in 0 until anim.animationCount) {
                val name = anim.getAnimationName(i).takeUnless { it.isNullOrBlank() } ?: "Анимация ${i + 1}"
                clips += AnimationClip(i, name, anim.getAnimationDuration(i))
            }
        }
        state.onModelReady(clips.filter { it.durationSec > 0f }, preferredClipOrder)
        state.currentClip?.let { clip ->
            anim?.applyAnimation(clip.index, 0f)
            anim?.updateBoneMatrices()
        }

        fitCameraToModel(a)
        scene.addEntities(a.entities)
    }

    /** Габариты модели без «контактной тени» — по мировым AABB всех рендеримых узлов. */
    private fun fitCameraToModel(a: FilamentAsset) {
        val rm = engine.renderableManager
        val tm = engine.transformManager
        val box = Box()
        val world = FloatArray(16)
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var found = false
        for (entity in a.renderableEntities) {
            val name = a.getName(entity) ?: ""
            if (name.contains("ContactShadow", ignoreCase = true)) continue
            val ri = rm.getInstance(entity)
            if (ri == 0) continue
            rm.getAxisAlignedBoundingBox(ri, box)
            val ti = tm.getInstance(entity)
            if (ti != 0) tm.getWorldTransform(ti, world) else identity(world)
            val c = box.center
            val h = box.halfExtent
            for (sx in -1..1 step 2) for (sy in -1..1 step 2) for (sz in -1..1 step 2) {
                val lx = c[0] + sx * h[0]; val ly = c[1] + sy * h[1]; val lz = c[2] + sz * h[2]
                // матрица Filament — column-major
                val wx = world[0] * lx + world[4] * ly + world[8] * lz + world[12]
                val wy = world[1] * lx + world[5] * ly + world[9] * lz + world[13]
                val wz = world[2] * lx + world[6] * ly + world[10] * lz + world[14]
                minX = min(minX, wx); minY = min(minY, wy); minZ = min(minZ, wz)
                maxX = max(maxX, wx); maxY = max(maxY, wy); maxZ = max(maxZ, wz)
                found = true
            }
        }
        if (!found) {
            val b = a.boundingBox
            val c = b.center
            val h = b.halfExtent
            minX = c[0] - h[0]; maxX = c[0] + h[0]
            minY = c[1] - h[1]; maxY = c[1] + h[1]
            minZ = c[2] - h[2]; maxZ = c[2] + h[2]
        }
        val dx = maxX - minX; val dy = maxY - minY; val dz = maxZ - minZ
        var radius = 0.5f * sqrt(dx * dx + dy * dy + dz * dz)
        // анимированные модели раскладываются — оставляем запас, чтобы не выйти за кадр
        if (state.hasAnimations) radius *= 1.18f
        state.camera.setModelBounds((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f, radius)
        state.camera.snapToDefault()
        // заполняющий свет — относительно размера модели
        val lm = engine.lightManager
        val li = lm.getInstance(fillEntity)
        if (li != 0) {
            val ox = radius * 2.2f
            val oy = (maxY - minY) / 2f + radius * 1.6f
            val oz = radius * 3f
            lm.setPosition(li, (minX + maxX) / 2f + ox, (minY + maxY) / 2f + oy, (minZ + maxZ) / 2f + oz)
            lm.setFalloff(li, radius * 12f)
            // ~18 000 лк в центре модели независимо от её размера: Φ = E · 4π · d²
            val d2 = ox * ox + oy * oy + oz * oz
            lm.setIntensity(li, FILL_LUX * 4f * Math.PI.toFloat() * d2)
        }
    }

    private fun identity(m: FloatArray) {
        for (i in 0 until 16) m[i] = if (i % 5 == 0) 1f else 0f
    }

    // ------------------------------------------------------------------ кадр

    override fun doFrame(frameTimeNanos: Long) {
        if (!running || destroyed) return
        choreographer.postFrameCallback(this)
        val dt = if (lastFrameNanos == 0L) 0f else ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f)
        lastFrameNanos = frameTimeNanos

        try {
            if (resourcesPending) {
                resourceLoader.asyncUpdateLoad()
                if (resourceLoader.asyncGetLoadProgress() >= 1f) {
                    onResourcesLoaded()
                } else if (frameTimeNanos - loadStartedAtNanos > LOAD_TIMEOUT_NANOS) {
                    destroyModel()
                    state.onError("Не удалось загрузить текстуры модели", "Проверьте, что GLB содержит встроенные текстуры")
                }
            }

            val anim = animator
            val clip = state.currentClip
            if (!resourcesPending && anim != null && clip != null && state.advance(dt)) {
                // gltfio зацикливает время по длительности клипа — держимся чуть раньше конца,
                // чтобы на последнем кадре модель оставалась в конечном положении.
                val t = min(state.time, clip.durationSec - 0.001f).coerceAtLeast(0f)
                anim.applyAnimation(clip.index, t)
                anim.updateBoneMatrices()
            }

            state.camera.update(dt)
            updateCamera()

            val sc = swapChain ?: return
            if (uiHelper.isReadyToRender && renderer.beginFrame(sc, frameTimeNanos)) {
                renderer.render(view)
                renderer.endFrame()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "frame failed", t)
            state.onError("Ошибка отображения 3D-модели", t.message)
            pause()
        }
    }

    private fun updateCamera() {
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)
        val cam = state.camera
        val (near, far) = cam.clipPlanes(FOV_DEG, aspect)
        camera.setProjection(FOV_DEG.toDouble(), aspect.toDouble(), near.toDouble(), far.toDouble(), Camera.Fov.VERTICAL)
        val eye = cam.eye(FOV_DEG, aspect)
        camera.lookAt(
            eye[0].toDouble(), eye[1].toDouble(), eye[2].toDouble(),
            cam.targetX.toDouble(), cam.targetY.toDouble(), cam.targetZ.toDouble(),
            0.0, 1.0, 0.0,
        )
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
            textureView.display?.let { displayHelper.attach(renderer, it) }
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            viewportWidth = width.coerceAtLeast(1)
            viewportHeight = height.coerceAtLeast(1)
            view.viewport = Viewport(0, 0, viewportWidth, viewportHeight)
        }
    }

    companion object {
        const val FOV_DEG = 30f
        private const val FILL_LUX = 18_000f
        private const val LOAD_TIMEOUT_NANOS = 20_000_000_000L

        /** null — устройство не поддерживает Filament (нет OpenGL ES 3.0 или нативных библиотек). */
        fun createOrNull(
            context: Context,
            textureView: TextureView,
            state: Furniture3DViewerState,
            lowEnd: Boolean,
        ): FilamentSceneHost? {
            if (!FilamentRuntime.ensureInitialized()) return null
            return try {
                FilamentSceneHost(context, textureView, state, lowEnd)
            } catch (t: Throwable) {
                Log.e(TAG, "Engine creation failed", t)
                null
            }
        }
    }
}
