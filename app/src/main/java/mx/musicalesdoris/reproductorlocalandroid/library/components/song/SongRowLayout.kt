package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import java.io.File

@Composable
internal fun SongRowLayout(song: Song, selected: Boolean, artwork: File?, theme: PlayerTheme,
                           onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit,
                           destructiveTitle: String, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?,
                           variant: String) {
    var expanded by remember { mutableStateOf(false) }
    val soniq = variant == "soniq"
    val shape = RoundedCornerShape(if (variant == "aurora") 24.dp else if (soniq) 13.dp else 14.dp)
    val background = when (variant) {
        "soniq" -> if (selected) Brush.verticalGradient(listOf(Color.White.copy(alpha = .92f), Color(0xFFD7EBFF)))
            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        "aurora" -> Brush.verticalGradient(listOf(theme.surface, theme.surface))
        else -> Brush.verticalGradient(listOf(theme.surface.copy(alpha = .45f), theme.surface.copy(alpha = .45f)))
    }
    Row(Modifier.fillMaxWidth().background(background, shape)
        .padding(horizontal = if (soniq) 8.dp else if (variant == "aurora") 14.dp else 10.dp,
            vertical = if (soniq) 5.dp else if (variant == "aurora") 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (soniq) 6.dp else 12.dp)) {
        Row(Modifier.weight(1f).clickable(onClick = onSelect), verticalAlignment = Alignment.CenterVertically) {
            ArtworkView(artwork, theme, 48.dp, song.id, circular = variant == "aurora")
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = if (selected && soniq) theme.accent else theme.foreground,
                    fontSize = if (soniq) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(song.subtitle, color = theme.muted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(song.displayDuration(), color = theme.muted, fontSize = if (soniq) 10.sp else 12.sp,
                fontFamily = FontFamily.Monospace)
        }
        if (!soniq) Text(if (song.favorite) "♥" else "♡", color = theme.accent, fontSize = 20.sp,
            modifier = Modifier.clickable(onClick = onFavorite).padding(3.dp))
        Box {
            Text("⋮", color = theme.muted, fontSize = 18.sp,
                modifier = Modifier.clickable { expanded = true }.padding(horizontal = 4.dp, vertical = 8.dp))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(if (song.favorite) "Quitar de favoritos" else "Agregar a favoritos") },
                    onClick = { expanded = false; onFavorite() })
                if (onMoveUp != null) DropdownMenuItem(text = { Text("Mover arriba") }, onClick = { expanded = false; onMoveUp() })
                if (onMoveDown != null) DropdownMenuItem(text = { Text("Mover abajo") }, onClick = { expanded = false; onMoveDown() })
                DropdownMenuItem(text = { Text(destructiveTitle) }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}

private fun Song.displayDuration(): String {
    val ms = durationMs ?: return "—"
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
