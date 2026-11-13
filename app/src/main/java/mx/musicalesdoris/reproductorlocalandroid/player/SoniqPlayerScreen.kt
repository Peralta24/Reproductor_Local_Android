package mx.musicalesdoris.reproductorlocalandroid.player

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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.SoniqCdCase
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.animation.ValueAnimator
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.soniqClickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SoniqPlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState,
                      player: PlayerViewModel, onClose: () -> Unit) {
    val progress by player.progress.collectAsState()
    val marqueeEnabled = ValueAnimator.areAnimatorsEnabled()
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
                    Text("SONIQ", color = theme.foreground, fontFamily = SoniqHeadingFont,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                }
                Text("REPRODUCIENDO", color = theme.muted, fontSize = 8.sp,
                    fontFamily = SoniqMonoFont, letterSpacing = 1.5.sp)
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
            Text("TU BIBLIOTECA / ${song?.album?.uppercase() ?: "ARCHIVO LOCAL"}", color = theme.muted, fontSize = 9.sp,
                fontFamily = SoniqMonoFont, letterSpacing = .6.sp, maxLines = 1,
                overflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).then(if (marqueeEnabled)
                    Modifier.basicMarquee(initialDelayMillis = 1200, velocity = 24.dp) else Modifier))
            Text("● EN EL DISPOSITIVO", color = theme.accent, fontSize = 8.sp,
                fontFamily = SoniqMonoFont)
        }
        Spacer(Modifier.height(19.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = (maxWidth - 22.dp).coerceAtMost(440.dp)
            SoniqPlayerArtwork(song, player, side)
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Sin canción", color = theme.foreground, fontFamily = SoniqHeadingFont, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().then(if (marqueeEnabled)
                        Modifier.basicMarquee(initialDelayMillis = 1200, velocity = 24.dp) else Modifier))
                Text(if (song == null) "Importa una canción para empezar" else listOfNotNull(song.artist, song.album).filter(String::isNotBlank).joinToString(" · ").ifBlank { "Archivo local" },
                    color = theme.muted, fontFamily = SoniqBodyFont, fontSize = 14.sp, maxLines = 1,
                    overflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().then(if (marqueeEnabled)
                        Modifier.basicMarquee(initialDelayMillis = 1200, velocity = 22.dp) else Modifier))
            }
            Box(Modifier.size(49.dp).background(Color(0xFFCFF5FF), CircleShape)
                .border(1.dp, Color(0xFF8BD5EC), CircleShape)
                .soniqClickable(CircleShape, enabled = song != null, onClick = player::toggleFavorite), contentAlignment = Alignment.Center) {
                Text(if (song?.favorite == true) "♥" else "♡", color = Color(0xFF1C9BE3), fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(19.dp))
        Slider(value = progress.currentMs.toFloat().coerceIn(0f, progress.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { player.seek(it.toInt()) }, valueRange = 0f..progress.durationMs.coerceAtLeast(1).toFloat(),
            enabled = progress.durationMs > 0, colors = SliderDefaults.colors(thumbColor = Color(0xFF1A9BE8),
                activeTrackColor = Color(0xFF1A9BE8), inactiveTrackColor = Color(0xFF9DB4CC)))
        Row(Modifier.fillMaxWidth()) {
            Text(time(progress.currentMs), color = theme.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text(time(progress.durationMs), color = theme.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SoniqTransportIcon("shuffle", Modifier.padding(8.dp).soniqClickable(CircleShape,
                enabled = song != null, onClick = player::toggleShuffle),
                size = 36.dp, color = if (state.shuffle) theme.accent else theme.foreground)
            TransportButton("previous", 57.dp, theme, player::previousSong)
            TransportButton(if (state.isPlaying) "pause" else "play", 78.dp, theme, player::togglePlayback)
            TransportButton("next", 57.dp, theme, player::nextSong)
            SoniqTransportIcon("repeat", Modifier.padding(8.dp).soniqClickable(CircleShape,
                enabled = song != null, onClick = player::toggleRepeat),
                size = 36.dp, color = if (state.repeat) theme.accent else theme.foreground)
        }
        Spacer(Modifier.height(27.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SoniqTransportIcon("volume", size = 21.dp, color = theme.muted)
            Slider(value = state.volume, onValueChange = player::setVolume, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = theme.foreground, activeTrackColor = theme.foreground))
            Text("${(state.volume * 100).toInt()} %", color = theme.muted, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace)
            SoniqTransportIcon("volume", size = 21.dp, color = theme.muted)
        }
        Spacer(Modifier.height(27.dp))
        Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A1A30), RoundedCornerShape(18.dp))
            .soniqClickable(RoundedCornerShape(18.dp)) { showQueue = true }
            .padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            SoniqTransportIcon("queue", size = 25.dp, color = Color(0xFF5AC5F5))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Cola de reproducción", color = Color.White, fontFamily = SoniqBodyFont,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${queue.size} canciones · Desde tu dispositivo", color = Color(0xFF8AA3BF),
                    fontSize = 9.sp, fontFamily = SoniqMonoFont)
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
    Box(Modifier.size(48.dp).clip(CircleShape)
        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD1DEEE))), CircleShape)
        .border(1.dp, Color(0xFFA1B8CE), CircleShape).soniqClickable(CircleShape, onClick = action),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color(0xFF1A293B), fontSize = 23.sp)
    }
}

@Composable
private fun TransportButton(name: String, size: Dp, theme: PlayerTheme, action: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape)
        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD1DEEE))), CircleShape)
        .border(1.dp, Color(0xFF99B3CC), CircleShape).soniqClickable(CircleShape, onClick = action),
        contentAlignment = Alignment.Center) {
        SoniqTransportIcon(name, size = if (size > 60.dp) 31.dp else 24.dp, color = theme.foreground)
    }
}

@Composable
private fun SoniqPlayerArtwork(song: Song?, player: PlayerViewModel, side: Dp) {
    val file = song?.let(player.library::artworkFor)
    Box(Modifier.size(side),
        contentAlignment = Alignment.Center) {
        SoniqCdCase(file, PlayerTheme.all.first { it.id == "soniq" }, side, song?.id.orEmpty())
        Text(song?.fileName?.substringAfterLast('.', "AUDIO")?.uppercase() ?: "AUDIO", color = Color.White,
            fontSize = 9.sp, fontFamily = SoniqMonoFont,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                .background(Color(0xFF09121F).copy(alpha = .75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

private fun time(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
