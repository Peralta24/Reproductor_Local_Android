package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.Song
import java.io.File

@Composable
fun NeonSongRow(song: Song, selected: Boolean, artwork: File?, theme: PlayerTheme,
                    onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit,
                    destructiveTitle: String, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?,
                    onAddToPlaylist: (() -> Unit)? = null) {
    SongRowLayout(song, selected, artwork, theme, onSelect, onFavorite, onDelete,
        destructiveTitle, onMoveUp, onMoveDown, "neon", onAddToPlaylist)
}
