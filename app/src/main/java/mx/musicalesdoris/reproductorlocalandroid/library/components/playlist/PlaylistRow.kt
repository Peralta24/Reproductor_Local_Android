package mx.musicalesdoris.reproductorlocalandroid.library.components.playlist

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable
import java.io.File

@Composable
fun PlaylistRow(playlist: PlaylistSummary, variantId: String, theme: PlayerTheme, artwork: File? = null) {
    when (variantId) {
        "aurora" -> AuroraPlaylistRow(playlist, theme, artwork)
        else -> NeonPlaylistRow(playlist, theme, artwork)
    }
}
