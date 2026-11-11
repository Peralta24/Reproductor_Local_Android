package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun SongRow(song: Song, selected: Boolean, variantId: String, theme: PlayerTheme,
            onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraSongRow(song, selected, theme, onSelect, onFavorite, onDelete)
        else -> NeonSongRow(song, selected, theme, onSelect, onFavorite, onDelete)
    }
}
