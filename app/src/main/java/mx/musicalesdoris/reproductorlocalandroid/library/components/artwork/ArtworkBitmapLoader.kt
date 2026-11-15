package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ContentResolver
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.util.HashMap

/** Small, size-aware cache shared by Compose artwork and system media metadata. */
object ArtworkBitmapLoader {
    private val cache = object : LruCache<ArtworkCacheKey, Bitmap>((Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt()) {
        override fun sizeOf(key: ArtworkCacheKey, value: Bitmap): Int = value.byteCount / 1024
    }

    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loads = ArtworkLoadCoordinator(
        scope = loadScope,
        cached = cache::get,
        cache = { key, bitmap -> cache.put(key, bitmap) },
        loader = ::decodeBitmap
    )

    suspend fun decodeFile(file: File, maxDimensionPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        val key = ArtworkCacheKey(file.absolutePath, file.lastModified(), file.length(), maxDimensionPx)
        loads.load(key)
    }

    private fun decodeBitmap(key: ArtworkCacheKey): Bitmap? {
        val file = File(key.absolutePath)
        if (!file.isFile || file.lastModified() != key.lastModifiedMs || file.length() != key.lengthBytes) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateArtworkSampleSize(bounds.outWidth, bounds.outHeight, key.maxDimensionPx)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        if (file.lastModified() != key.lastModifiedMs || file.length() != key.lengthBytes) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    suspend fun decodeFileImage(file: File?, maxDimensionPx: Int): ImageBitmap? =
        file?.let { decodeFile(it, maxDimensionPx)?.asImageBitmap() }

    suspend fun decodeUriImage(resolver: ContentResolver, uri: Uri, maxDimensionPx: Int): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val opened = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
                true
            } ?: false
            if (!opened) return@withContext null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateArtworkSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
        }

}

internal data class ArtworkCacheKey(
    val absolutePath: String,
    val lastModifiedMs: Long,
    val lengthBytes: Long,
    val maxDimensionPx: Int
)

/** Shares one independent decode job per key; canceling a waiter does not cancel other waiters. */
internal class ArtworkLoadCoordinator<K : Any, V : Any>(
    private val scope: CoroutineScope,
    private val cached: (K) -> V?,
    private val cache: (K, V) -> Unit,
    private val loader: suspend (K) -> V?
) {
    private val lock = Any()
    private val inFlight = HashMap<K, Deferred<V?>>()

    suspend fun load(key: K): V? {
        val shared = synchronized(lock) {
            cached(key)?.let { return it }
            inFlight[key] ?: run {
                lateinit var created: Deferred<V?>
                created = scope.async(start = CoroutineStart.LAZY) {
                    try {
                        loader(key)?.also { cache(key, it) }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    } finally {
                        synchronized(lock) {
                            if (inFlight[key] === created) inFlight.remove(key)
                        }
                    }
                }
                inFlight[key] = created
                created.start()
                created
            }
        }
        return shared.await()
    }
}

internal fun calculateArtworkSampleSize(width: Int, height: Int, limit: Int): Int {
    if (width <= 0 || height <= 0 || limit <= 0) return 1
    var sample = 1
    while (maxOf(width / (sample * 2), height / (sample * 2)) >= limit) sample *= 2
    return sample
}
