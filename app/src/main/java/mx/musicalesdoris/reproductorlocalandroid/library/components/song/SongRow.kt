package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.Song
import java.io.File

@Composable
fun SongRow(song: Song, selected: Boolean, artwork: File?, theme: PlayerTheme,
            onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit,
            destructiveTitle: String = "Eliminar archivo", onMoveUp: (() -> Unit)? = null,
            onMoveDown: (() -> Unit)? = null) {
    when (theme.variantFor(LibraryComponent.SONG)) {
        "soniq" -> SoniqSongRow(song, selected, artwork, theme, onSelect, onFavorite,
            onDelete, destructiveTitle, onMoveUp, onMoveDown)
        "aurora" -> AuroraSongRow(song, selected, artwork, theme, onSelect, onFavorite,
            onDelete, destructiveTitle, onMoveUp, onMoveDown)
        else -> NeonSongRow(song, selected, artwork, theme, onSelect, onFavorite,
            onDelete, destructiveTitle, onMoveUp, onMoveDown)
    }
}
