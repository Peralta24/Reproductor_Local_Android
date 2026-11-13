package mx.musicalesdoris.reproductorlocalandroid.library.components.artist

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import java.io.File

@Composable
fun AuroraArtistRow(artist: ArtistSummary, artwork: File?, theme: PlayerTheme, onClick: () -> Unit) {
    ArtistRowLayout(artist, artwork, theme, onClick)
}
