package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable
import java.io.File

@Composable
fun SongRow(song: Song, artworkFile: File?, selected: Boolean, available: Boolean, variantId: String, theme: PlayerTheme,
            onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraSongRow(song, artworkFile, selected, available, theme, onSelect, onFavorite, onDelete)
        else -> NeonSongRow(song, artworkFile, selected, available, theme, onSelect, onFavorite, onDelete)
    }
}

internal fun songMetadataLine(song: Song): String =
    listOfNotNull(song.artist ?: song.albumArtist, song.album).joinToString(" · ")
