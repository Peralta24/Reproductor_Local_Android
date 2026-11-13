package mx.musicalesdoris.reproductorlocalandroid.library.components.playlist

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.PlaylistSummary
import java.io.File

@Composable
fun NeonPlaylistRow(playlist: PlaylistSummary, artwork: File?, theme: PlayerTheme,
                   onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    PlaylistRowLayout(playlist, artwork, theme, onOpen, onRename, onDelete, false)
}
