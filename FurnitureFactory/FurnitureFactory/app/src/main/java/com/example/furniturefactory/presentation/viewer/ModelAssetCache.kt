package com.example.furniturefactory.presentation.viewer

import android.app.ActivityManager
import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * Кэш байтов GLB в памяти: повторное открытие товара не перечитывает файл с диска.
 * Чтение — в фоновом потоке (Dispatchers.IO), UI-поток не блокируется.
 */
object ModelAssetCache {

    private val cache = object : LruCache<String, ByteArray>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    /**
     * @param path путь внутри assets («models/sofa.glb») или абсолютный путь к файлу.
     * @throws FileNotFoundException если модели нет.
     */
    suspend fun load(context: Context, path: String): ByteArray {
        cache.get(path)?.let { return it }
        val bytes = withContext(Dispatchers.IO) {
            if (path.startsWith("/")) {
                val file = File(path)
                if (!file.exists()) throw FileNotFoundException(path)
                file.readBytes()
            } else {
                context.applicationContext.assets.open(path).use { it.readBytes() }
            }
        }
        if (bytes.size < 12) throw IllegalArgumentException("Файл слишком мал для GLB")
        cache.put(path, bytes)
        return bytes
    }

    fun clear() = cache.evictAll()

    private const val MAX_BYTES = 48 * 1024 * 1024
}

object DeviceProfile {
    /** Слабое устройство — используем облегчённую модель и упрощённую графику. */
    fun isLowEnd(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        if (am.isLowRamDevice) return true
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem < 3L * 1024 * 1024 * 1024 || am.memoryClass <= 128
    }
}
