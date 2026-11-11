package mx.musicalesdoris.reproductorlocalandroid.library.components.playlist

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun PlaylistRow(playlist: PlaylistSummary, variantId: String, theme: PlayerTheme) {
    when (variantId) {
        "aurora" -> AuroraPlaylistRow(playlist, theme)
        else -> NeonPlaylistRow(playlist, theme)
    }
}
