package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                file?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
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
