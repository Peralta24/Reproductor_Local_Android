package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun AlbumCard(album: AlbumSummary, variantId: String, theme: PlayerTheme) {
    when (variantId) {
        "aurora" -> AuroraAlbumCard(album, theme)
        else -> NeonAlbumCard(album, theme)
    }
}
