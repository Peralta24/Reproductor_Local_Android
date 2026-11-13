package mx.musicalesdoris.reproductorlocalandroid.player

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song

@Composable
fun StandardPlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState, player: PlayerViewModel,
                         onClose: () -> Unit, importAudio: () -> Unit) {
    val art = remember(song?.id, song?.artworkFileName) { song?.let(player.library::artworkFor) }
    val bitmap = remember(art?.absolutePath) { art?.let { BitmapFactory.decodeFile(it.absolutePath) } }
    val queue = player.queueSongs()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .background(Brush.verticalGradient(listOf(theme.topGlow.copy(alpha = .4f), theme.background, theme.bottomGlow.copy(alpha = .25f))))
        .padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("⌄", color = theme.foreground, fontSize = 27.sp) }
            Text("Reproduciendo", color = theme.foreground, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton(onClick = importAudio) { Text("+", color = theme.accent, fontSize = 26.sp) }
        }
        Spacer(Modifier.height(18.dp))
        if (theme.id == "soniq") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TU BIBLIOTECA / ARCHIVO LOCAL", color = theme.muted, fontSize = 10.sp)
                Text("● EN EL DISPOSITIVO", color = theme.accent, fontSize = 10.sp)
            }
            Spacer(Modifier.height(18.dp))
        }
        Box(Modifier.size(280.dp).clip(RoundedCornerShape(theme.corner.dp))
            .background(Brush.linearGradient(listOf(theme.topGlow, theme.accent, theme.bottomGlow))),
            contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), "Carátula", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text("♫", color = theme.foreground, fontSize = 100.sp)
        }
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Sin canción", color = theme.foreground, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song?.subtitle ?: "Importa una canción", color = theme.muted, fontSize = 16.sp)
            }
            TextButton(onClick = player::toggleFavorite, enabled = song != null) {
                Text(if (song?.favorite == true) "♥" else "♡", color = theme.accent, fontSize = 30.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Slider(value = state.currentMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { player.seek(it.toInt()) }, valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.durationMs > 0, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(state.currentMs), color = theme.muted)
            Spacer(Modifier.weight(1f))
            Text(formatTime(state.durationMs), color = theme.muted)
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            PlayerButton(if (state.shuffle) "⤨" else "⇄", theme, state.shuffle, player::toggleShuffle)
            PlayerButton("⏮", theme, false, player::previousSong)
            Box(Modifier.size(72.dp).clip(CircleShape).background(theme.accent)
                .clickable(enabled = song != null, onClick = player::togglePlayback), contentAlignment = Alignment.Center) {
                Text(if (state.isPlaying) "Ⅱ" else "▶", color = androidx.compose.ui.graphics.Color.White, fontSize = 28.sp)
            }
            PlayerButton("⏭", theme, false, player::nextSong)
            PlayerButton(if (state.repeat) "↻¹" else "↻", theme, state.repeat, player::toggleRepeat)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { player.skip(-15_000) }) { Text("↶ 15", color = theme.accent) }
            Spacer(Modifier.width(20.dp))
            TextButton(onClick = { player.skip(15_000) }) { Text("15 ↷", color = theme.accent) }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◀", color = theme.muted)
            Slider(value = state.volume, onValueChange = player::setVolume,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("${(state.volume * 100).toInt()} %", color = theme.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Cola · ${queue.size} canciones", color = theme.foreground, fontSize = 18.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        queue.forEach { item ->
            Text(item.title, color = if (item.id == song?.id) theme.accent else theme.foreground,
                modifier = Modifier.fillMaxWidth().clickable { player.select(item, preserveQueue = true) }
                    .padding(vertical = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlayerButton(label: String, theme: PlayerTheme, active: Boolean, onClick: () -> Unit) {
    Text(label, color = if (active) theme.accent else theme.foreground, fontSize = 22.sp,
        modifier = Modifier.size(43.dp).clickable(onClick = onClick).wrapContentSize())
}

private fun formatTime(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
