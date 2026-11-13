package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayerContent(
    song: Song?,
    state: PlayerState,
    theme: PlayerTheme,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(theme.background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) { Text("♫", color = theme.accent, fontSize = 22.sp) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(song?.title ?: "Sin canción", color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song?.artist?.takeIf(String::isNotBlank) ?: "Artista desconocido",
                color = Color.White.copy(alpha = .68f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = {
                if (state.durationMs > 0) (state.currentMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
            },
                modifier = Modifier.fillMaxWidth(), color = theme.accent,
                trackColor = Color.White.copy(alpha = .15f))
        }
        IconButton(
            enabled = song != null && state.canSkipPrevious,
            onClick = onPrevious,
            modifier = Modifier.semantics { contentDescription = "Canción anterior" }
        ) { Text("⏮", color = Color.White, fontSize = 16.sp) }
        IconButton(
            enabled = song != null && (state.isPlaying ||
                (state.selectedFileAvailable && !state.isPreparing)),
            onClick = onTogglePlayback,
            modifier = Modifier.semantics {
                contentDescription = if (state.isPlaying) "Pausar reproducción" else "Reanudar reproducción"
            }
        ) { Text(if (state.isPlaying) "Ⅱ" else "▶", color = Color.White, fontSize = 20.sp) }
        IconButton(
            enabled = song != null && state.canSkipNext,
            onClick = onNext,
            modifier = Modifier.semantics { contentDescription = "Canción siguiente" }
        ) { Text("⏭", color = Color.White, fontSize = 16.sp) }
    }
}
