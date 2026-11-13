package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song

@Composable
fun SoniqMiniPlayer(song: Song?, state: PlayerState, theme: PlayerTheme,
                    player: PlayerViewModel, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFD4E8FB), Color(0xFF94C7F1))),
            RoundedCornerShape(24.dp)).border(1.dp, Color(0xFF7BAED2), RoundedCornerShape(24.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).background(Brush.verticalGradient(listOf(Color.White, Color(0xFF83BDEA))), CircleShape)
                    .border(1.dp, Color(0xFF7BAED2), CircleShape)
                    .clickable(enabled = song != null, onClick = player::togglePlayback), contentAlignment = Alignment.Center) {
                    Text(if (state.isPlaying) "Ⅱ" else "▶", color = Color(0xFF304F69), fontSize = 23.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text("MP3", color = theme.secondaryAccent, fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                    modifier = Modifier.background(Color(0xFF0A293F), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f).height(77.dp).background(Brush.verticalGradient(
                listOf(Color(0xFF07182C), Color(0xFF031020))), RoundedCornerShape(7.dp))
                .border(1.dp, Color(0xFF4F80AA), RoundedCornerShape(7.dp))
                .clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Text("DIGITAL MP3 PLAYER", color = Color(0xFF6B8FB5), fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp, letterSpacing = 1.3.sp)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E4D82)))
                Text(song?.title ?: "Sin canción", color = Color(0xFF7DD6FA),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (song == null) "Importa una canción" else "Archivo local", color = Color(0xFF7DA4C2),
                    fontFamily = FontFamily.Monospace, fontSize = 8.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time(state.currentMs), color = Color(0xFF7DA4C2), fontFamily = FontFamily.Monospace, fontSize = 7.sp)
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.weight(1f).height(4.dp).background(Color(0xFF0D304F), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(if (state.durationMs > 0)
                            (state.currentMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f)
                            .height(4.dp).background(Color(0xFF50C9FA), RoundedCornerShape(2.dp)))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(time(state.durationMs), color = Color(0xFF7DA4C2), fontFamily = FontFamily.Monospace, fontSize = 7.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.width(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(42.dp).background(Brush.verticalGradient(listOf(Color.White, Color(0xFF83BDEA))), CircleShape)
                    .border(1.dp, Color(0xFF7BAED2), CircleShape).clickable { player.nextSong() },
                    contentAlignment = Alignment.Center) { Text("⏭", color = Color(0xFF304F69), fontSize = 18.sp) }
                Text("♫", color = theme.muted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MP3/WMA/REC", color = theme.muted.copy(alpha = .7f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("★ ★ ★", color = theme.accent, fontSize = 8.sp)
            Text("USB ♫", color = theme.muted.copy(alpha = .7f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun time(ms: Int): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
