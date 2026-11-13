package mx.musicalesdoris.reproductorlocalandroid.player

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoniqPlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState,
                      player: PlayerViewModel, onClose: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val queue = player.queueSongs()
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.topGlow, theme.background,
        theme.bottomGlow))).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChromeButton("⌄", onClose)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(theme.accent, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center) { Text("≋", color = Color.White, fontSize = 19.sp) }
                    Spacer(Modifier.width(7.dp))
                    Text("SONIQ", color = theme.foreground, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text("REPRODUCIENDO", color = theme.muted, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            Spacer(Modifier.weight(1f))
            Box {
                ChromeButton("⋯") { menuExpanded = true }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Cola de reproducción") }, onClick = { menuExpanded = false; showQueue = true })
                    DropdownMenuItem(text = { Text("Cambiar favorito") }, onClick = { menuExpanded = false; player.toggleFavorite() })
                }
            }
        }
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TU BIBLIOTECA / ARCHIVO LOCAL", color = theme.muted, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Text("● EN EL DISPOSITIVO", color = theme.accent, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(19.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = (maxWidth - 22.dp).coerceAtMost(440.dp)
            SoniqPlayerArtwork(song, player, side)
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Sin canción", color = theme.foreground, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (song == null) "Importa una canción para empezar" else "Archivo local",
                    color = theme.muted, fontSize = 15.sp)
            }
            Box(Modifier.size(49.dp).background(Color(0xFFCFF5FF), CircleShape)
                .border(1.dp, Color(0xFF8BD5EC), CircleShape)
                .clickable(enabled = song != null, onClick = player::toggleFavorite), contentAlignment = Alignment.Center) {
                Text(if (song?.favorite == true) "♥" else "♡", color = Color(0xFF1C9BE3), fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(19.dp))
        Slider(value = state.currentMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { player.seek(it.toInt()) }, valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.durationMs > 0, colors = SliderDefaults.colors(thumbColor = Color(0xFF1A9BE8),
                activeTrackColor = Color(0xFF1A9BE8)))
        Row(Modifier.fillMaxWidth()) {
            Text(time(state.currentMs), color = theme.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text(time(state.durationMs), color = theme.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("⤨", color = if (state.shuffle) theme.accent else theme.foreground,
                fontSize = 20.sp, modifier = Modifier.clickable(enabled = song != null, onClick = player::toggleShuffle).padding(8.dp))
            TransportButton("⏮", 57.dp, theme, player::previousSong)
            TransportButton(if (state.isPlaying) "Ⅱ" else "▶", 78.dp, theme, player::togglePlayback)
            TransportButton("⏭", 57.dp, theme, player::nextSong)
            Text(if (state.repeat) "↻¹" else "↻", color = if (state.repeat) theme.accent else theme.foreground,
                fontSize = 20.sp, modifier = Modifier.clickable(enabled = song != null, onClick = player::toggleRepeat).padding(8.dp))
        }
        Spacer(Modifier.height(27.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◀", color = theme.muted, fontSize = 15.sp)
            Slider(value = state.volume, onValueChange = player::setVolume, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = theme.foreground, activeTrackColor = theme.foreground))
            Text("${(state.volume * 100).toInt()} %", color = theme.muted, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(27.dp))
        Row(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF0A1A30), RoundedCornerShape(18.dp))
            .clickable { showQueue = true }.padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☷", color = Color(0xFF5AC5F5), fontSize = 25.sp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Cola de reproducción", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${queue.size} canciones · Desde tu dispositivo", color = Color(0xFF8AA3BF),
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Text("›", color = Color.White, fontSize = 20.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
    if (showQueue) ModalBottomSheet(onDismissRequest = { showQueue = false }) {
        Text("Cola de reproducción", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(Modifier.heightIn(max = 500.dp)) {
            items(queue, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth().clickable {
                    player.select(item, preserveQueue = true)
                    showQueue = false
                }.padding(horizontal = 20.dp, vertical = 13.dp)) {
                    Text(item.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.id == song?.id) Text("≋", color = theme.accent)
                }
            }
        }
    }
}

@Composable
private fun ChromeButton(label: String, action: () -> Unit) {
    Box(Modifier.size(48.dp).background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD1DEEE))), CircleShape)
        .border(1.dp, Color(0xFFA1B8CE), CircleShape).clickable(onClick = action), contentAlignment = Alignment.Center) {
        Text(label, color = Color(0xFF1A293B), fontSize = 23.sp)
    }
}

@Composable
private fun TransportButton(label: String, size: Dp, theme: PlayerTheme, action: () -> Unit) {
    Box(Modifier.size(size).background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD1DEEE))), CircleShape)
        .border(1.dp, Color(0xFF99B3CC), CircleShape).clickable(onClick = action), contentAlignment = Alignment.Center) {
        Text(label, color = theme.foreground, fontSize = if (size > 60.dp) 28.sp else 21.sp)
    }
}

@Composable
private fun SoniqPlayerArtwork(song: Song?, player: PlayerViewModel, side: Dp) {
    val file = song?.let(player.library::artworkFor)
    val bitmap = remember(file?.absolutePath) { file?.let { BitmapFactory.decodeFile(it.absolutePath) } }
    Box(Modifier.size(side).clip(RoundedCornerShape(7.dp)).background(Color(0xFF292F3D)),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), "Carátula", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else {
            Box(Modifier.size(side * .91f).background(Brush.radialGradient(listOf(
                Color(0xFF5A94E8), Color(0xFF263D7A), Color(0xFF141C30))), CircleShape),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(side * .62f, side * .29f).rotate(-37f)
                    .border((side.value * .045f).dp, Color.White.copy(alpha = .7f), CircleShape))
                Box(Modifier.size(side * .62f, side * .27f).rotate(42f)
                    .border((side.value * .035f).dp, Color(0xFF91BDF6), CircleShape))
                Box(Modifier.size(side * .31f).background(Brush.radialGradient(listOf(
                    Color(0xFF434D73), Color(0xFF1F243D))), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(side * .065f).background(Color(0xFF050A12), CircleShape))
                }
            }
        }
        Text(song?.fileName?.substringAfterLast('.', "AUDIO")?.uppercase() ?: "AUDIO", color = Color.White,
            fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                .background(Color(0xFF09121F).copy(alpha = .75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

private fun time(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
