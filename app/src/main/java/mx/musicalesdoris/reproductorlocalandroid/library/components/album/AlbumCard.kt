package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable
import java.io.File

@Composable
fun AlbumCard(album: AlbumSummary, artworkFile: File?, variantId: String, theme: PlayerTheme, onClick: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraAlbumCard(album, artworkFile, theme, onClick)
        else -> NeonAlbumCard(album, artworkFile, theme, onClick)
    }
}
