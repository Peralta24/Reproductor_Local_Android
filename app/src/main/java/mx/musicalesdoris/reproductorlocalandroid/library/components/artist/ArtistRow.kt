package mx.musicalesdoris.reproductorlocalandroid.library.components.artist

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun ArtistRow(artist: ArtistSummary, variantId: String, theme: PlayerTheme) {
    when (variantId) {
        "aurora" -> AuroraArtistRow(artist, theme)
        else -> NeonArtistRow(artist, theme)
    }
}
