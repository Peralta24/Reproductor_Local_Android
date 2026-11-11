package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun SongRowActions(song: Song, theme: PlayerTheme, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Row {
        TextButton(onClick = onFavorite) {
            Text(if (song.favorite) "♥" else "♡", color = theme.accent, fontSize = 24.sp)
        }
        TextButton(onClick = onDelete) {
            Text("×", color = Color(0xFFFF817A), fontSize = 24.sp)
        }
    }
}
