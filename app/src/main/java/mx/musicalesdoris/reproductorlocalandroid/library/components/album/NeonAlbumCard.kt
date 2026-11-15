package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import java.io.File

@Composable
fun NeonAlbumCard(album: AlbumSummary, artwork: File?, theme: PlayerTheme,
                  artworkRevision: Any? = null, onClick: () -> Unit) {
    AlbumCardLayout(album, artwork, theme, artworkRevision, onClick, soniq = false)
}
