package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.LocalArtwork

@Composable
internal fun QueueScreen(
    theme: PlayerTheme,
    songs: List<Song>,
    state: PlayerState,
    player: PlayerViewModel,
    onBack: () -> Unit
) {
    val byId = remember(songs) { songs.associateBy(Song::id) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.semantics {
                contentDescription = "Volver al reproductor"; role = Role.Button
            }) { Text("‹ Volver", color = theme.accent) }
            Text("Cola de reproducción", Modifier.weight(1f), color = Color.White,
                fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.shuffle, onClick = player::toggleShuffle, label = { Text("Aleatorio") })
            FilterChip(selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                onClick = player::toggleRepeat,
                label = { Text(when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "Repetir canción"
                    Player.REPEAT_MODE_ALL -> "Repetir cola"
                    else -> "Repetición desactivada"
                }) })
        }
        Spacer(Modifier.height(8.dp))
        if (state.queueSongIds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("La cola está vacía", color = Color.White.copy(alpha = .72f), fontSize = 16.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.queueSongIds, key = { _, id -> id }) { index, id ->
                    val song = byId[id]
                    val current = id == state.selectedId
        val played = id in state.playedSongIds || (!state.shuffle && index < state.queueIndex)
        val upcoming = id in state.upcomingSongIds
                    Row(Modifier.fillMaxWidth()
                        .background(if (current) theme.accent.copy(alpha = .18f) else Color.White.copy(alpha = .07f),
                            RoundedCornerShape(14.dp))
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LocalArtwork(song?.let(player.library::artworkFileFor),
                            song?.let { "Portada de ${it.title}" } ?: "Canción no disponible",
                            Modifier.size(48.dp), RoundedCornerShape(9.dp)) {
                            Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
                                Text("♫", color = theme.accent)
                            }
                        }
                        Column(Modifier.weight(1f).clickable(onClick = { player.playQueueItem(index) }).padding(horizontal = 10.dp)) {
                            Text(song?.title ?: "Canción no disponible", color = Color.White,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(when {
                                current -> "Reproduciendo"
                                played -> "Reproducida anteriormente"
                                upcoming -> listOfNotNull(song?.artist ?: song?.albumArtist, song?.album)
                                    .joinToString(" · ").ifBlank { "Siguiente en la cola" }
                                else -> "En la cola"
                            }, color = if (current) theme.accent else Color.White.copy(alpha = .67f),
                                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                error = if (index == 0 || !player.moveQueueItem(index, index - 1)) "No se pudo reordenar la cola" else null
                            }, enabled = index > 0, modifier = Modifier.semantics { contentDescription = "Mover antes" }) {
                                Text("↑", color = Color.White, fontSize = 18.sp)
                            }
                            IconButton(onClick = {
                                error = if (index == state.queueSongIds.lastIndex || !player.moveQueueItem(index, index + 1)) "No se pudo reordenar la cola" else null
                            }, enabled = index < state.queueSongIds.lastIndex, modifier = Modifier.semantics { contentDescription = "Mover después" }) {
                                Text("↓", color = Color.White, fontSize = 18.sp)
                            }
                        }
                        IconButton(onClick = {
                            error = if (player.removeQueueItem(index)) null else "No se pudo quitar la canción de la cola"
                        }, modifier = Modifier.semantics { contentDescription = "Quitar de la cola" }) {
                            Text("×", color = Color.White, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
        error?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp)) }
    }
}
