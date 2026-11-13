package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import java.io.File

@Composable
fun SoniqAlbumCard(album: AlbumSummary, artwork: File?, theme: PlayerTheme, onClick: () -> Unit) {
    AlbumCardLayout(album, artwork, theme, onClick, soniq = true)
}
