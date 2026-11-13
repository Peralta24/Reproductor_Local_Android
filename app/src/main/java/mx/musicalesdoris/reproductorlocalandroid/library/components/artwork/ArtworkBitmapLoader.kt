package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ContentResolver
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Small, size-aware cache shared by Compose artwork and system media metadata. */
object ArtworkBitmapLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun decodeFile(file: File, maxDimensionPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        val key = "${file.absolutePath}:${file.lastModified()}:${file.length()}:$maxDimensionPx"
        cache.get(key)?.let { return@withContext it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateArtworkSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, options)?.also { cache.put(key, it) }
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

internal fun calculateArtworkSampleSize(width: Int, height: Int, limit: Int): Int {
    if (width <= 0 || height <= 0 || limit <= 0) return 1
    var sample = 1
    while (maxOf(width / (sample * 2), height / (sample * 2)) >= limit) sample *= 2
    return sample
}
