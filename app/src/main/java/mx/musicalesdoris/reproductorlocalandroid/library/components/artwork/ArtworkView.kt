package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.SongLibrary
import java.io.File

@Composable
fun ArtworkView(file: File?, theme: PlayerTheme, size: Dp, seed: String = "", circular: Boolean = false,
                artworkRevision: Any? = null) {
    val bitmap = rememberArtworkBitmap(file, size, artworkRevision)
    val shape = remember(circular, theme.id) {
        if (circular) CircleShape else RoundedCornerShape(if (theme.id == "soniq") 7.dp else 10.dp)
    }
    Box(Modifier.size(size).clip(shape).background(if (theme.id == "soniq") Color(0xFF0D172B) else theme.surface),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, "Carátula", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (theme.id == "soniq") {
            val placeholderBrush = remember(seed) {
                val blue = seed.sumOf { it.code } % 2 == 0
                Brush.sweepGradient(if (blue) listOf(Color(0xFF163074), Color.Cyan, Color(0xFF163074))
                    else listOf(Color(0xFF4A175D), Color.Magenta, Color.Yellow, Color(0xFF4A175D)))
            }
            Box(Modifier.size(size * .82f).clip(CircleShape).background(placeholderBrush),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(size * .12f).clip(CircleShape).background(Color(0xFF071024)))
            }
        } else Text("♫", color = theme.accent, fontSize = (size.value * .4f).sp)
    }
}

@Composable
internal fun rememberArtworkBitmap(file: File?, size: Dp, artworkRevision: Any? = null): ImageBitmap? {
    val density = LocalDensity.current
    val targetPx = with(density) { size.roundToPx().coerceAtLeast(1) }
    val revisionKey = artworkRevision ?: file?.absolutePath
    return produceState<ImageBitmap?>(null, file?.absolutePath, targetPx, revisionKey) {
        value = ArtworkBitmapLoader.decodeFileImage(file, targetPx)
    }.value
}

@Composable
internal fun rememberArtworkRevisionToken(revision: Any?, library: SongLibrary? = null): Any {
    val indexRevision = library?.artworkIndexRevision?.collectAsState()?.value ?: 0L
    return remember(ArtworkRevisionKey(revision, indexRevision)) { Any() }
}

internal class ArtworkRevisionKey(private val value: Any?, private val indexRevision: Long = 0L) {
    override fun equals(other: Any?): Boolean = other is ArtworkRevisionKey &&
        indexRevision == other.indexRevision && when {
            value is String && other.value is String -> value == other.value
            else -> value === other.value
        }

    override fun hashCode(): Int = 31 * (if (value is String) value.hashCode() else System.identityHashCode(value)) + indexRevision.hashCode()
}
