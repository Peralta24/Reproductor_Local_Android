package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeonSongRow(song: Song, selected: Boolean, theme: PlayerTheme,
                onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .05f), RoundedCornerShape(14.dp))
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = onSelect), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Color.White.copy(alpha = .09f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Text(if (selected) "≋" else "♫", color = theme.accent, fontSize = 23.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (selected) "Seleccionada" else "Archivo local",
                    color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
            }
        }
        SongRowActions(song, theme, onFavorite, onDelete)
    }
}
