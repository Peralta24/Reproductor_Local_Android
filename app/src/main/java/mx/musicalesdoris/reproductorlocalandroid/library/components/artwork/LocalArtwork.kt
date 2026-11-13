package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtworkBitmapCache {
    private val bitmaps = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = max(1, value.allocationByteCount / 1024)
    }

    fun decode(file: File): Bitmap? {
        val key = file.absolutePath
        bitmaps.get(key)?.let { return it }
        if (!file.isFile) return null
        return BitmapFactory.decodeFile(key)?.also { bitmap -> bitmaps.put(key, bitmap) }
    }
}

/** Decodes private artwork away from Compose's main thread and falls back for missing/bad images. */
@Composable
fun LocalArtwork(
    file: File?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    fallback: @Composable () -> Unit
) {
    val image by produceState<Bitmap?>(initialValue = null, key1 = file?.absolutePath) {
        value = withContext(Dispatchers.IO) {
            try {
                file?.let(ArtworkBitmapCache::decode)
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { fallback() }
        }
    }
}
