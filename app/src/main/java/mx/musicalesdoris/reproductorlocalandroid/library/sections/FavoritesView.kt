package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song

@Composable
fun FavoritesView(songs: List<Song>, state: PlayerState, player: PlayerViewModel, theme: PlayerTheme) {
    var newestFirst by remember { mutableStateOf(true) }
    val favorites = songs.filter { it.favorite }.sortedBy { it.favoritedAt ?: it.importedAt }
        .let { if (newestFirst) it.reversed() else it }
    Column {
        if (theme.id == "soniq") {
            Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(164.dp).background(Brush.linearGradient(listOf(Color(0xFF14243C), Color(0xFF1964D2))),
                    RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Text("♥", color = Color(0xFF91E8FF), fontSize = 85.sp)
                }
                Spacer(Modifier.height(17.dp))
                Text("Favoritos", color = theme.foreground, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                val seconds = favorites.sumOf { it.durationMs ?: 0L } / 1000
                val duration = if (favorites.all { it.durationMs != null }) "${seconds / 60} min ${seconds % 60} s" else "—"
                Text("${favorites.size} canciones · $duration · En tu dispositivo", color = theme.muted, fontSize = 10.sp)
            }
            CollectionActions(favorites.isNotEmpty(), theme,
                onPlay = { player.playCollection(favorites) },
                onShuffle = { player.playCollection(favorites, shuffled = true) })
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tus favoritos", color = theme.foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { newestFirst = !newestFirst }) {
                    Text(if (newestFirst) "Recientes ↓" else "Recientes ↑", color = theme.muted, fontSize = 12.sp)
                }
            }
        } else {
            Text("Tus favoritos", color = theme.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
        }
        SongCollection(favorites, state, player, theme, "Aún no tienes favoritos")
    }
}

@Composable
fun CollectionActions(enabled: Boolean, theme: PlayerTheme, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(44.dp).background(Brush.horizontalGradient(listOf(theme.accent,
            theme.accent.copy(alpha = .75f))), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            TextButton(onClick = onPlay, enabled = enabled) { Text("▶ Reproducir", color = Color.White) }
        }
        Box(Modifier.weight(1f).height(44.dp).background(theme.surface, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center) {
            TextButton(onClick = onShuffle, enabled = enabled) { Text("Aleatorio", color = theme.foreground) }
        }
    }
}
