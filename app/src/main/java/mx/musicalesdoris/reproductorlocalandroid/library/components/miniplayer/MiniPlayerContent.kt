package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
fun MiniPlayerContent(song: Song?, state: PlayerState, progress: PlaybackProgress, theme: PlayerTheme, onTogglePlayback: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(theme.background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) { Text("♫", color = theme.accent, fontSize = 22.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song?.title ?: "Sin canción", color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress =
                if (progress.durationMs > 0) (progress.currentMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) else 0f,
                modifier = Modifier.fillMaxWidth(), color = theme.accent,
                trackColor = Color.White.copy(alpha = .15f))
        }
        Spacer(Modifier.width(12.dp))
        Text(if (state.isPlaying) "Ⅱ" else "▶", color = Color.White, fontSize = 24.sp,
            modifier = Modifier.clickable(enabled = song != null, onClick = onTogglePlayback).padding(8.dp))
    }
}
