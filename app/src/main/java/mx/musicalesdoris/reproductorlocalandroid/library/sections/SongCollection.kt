package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow
import kotlinx.coroutines.delay

@Composable
fun SongCollection(songs: List<Song>, state: PlayerState, player: PlayerViewModel,
                   theme: PlayerTheme, emptyTitle: String = "No hay canciones",
                   playlistId: String? = null, modifier: Modifier = Modifier) {
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    var addedToast by remember { mutableStateOf<String?>(null) }
    val playlists by player.library.playlists.collectAsState()
    LaunchedEffect(addedToast) {
        if (addedToast != null) {
            delay(2600)
            addedToast = null
        }
    }
    Box(modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                Text(emptyTitle, color = theme.muted)
            }
        } else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (theme.id == "soniq") 2.dp else 8.dp)) {
            items(songs, key = { it.id }) { song ->
                val ids = playlistId?.let { id -> player.library.playlists.value.firstOrNull { it.id == id }?.songIds }.orEmpty()
                val index = ids.indexOf(song.id)
                SongRow(song, song.id == state.selectedId, player.library.artworkFor(song), theme,
                    onSelect = { player.playCollection(songs, startingWith = song) },
                    onFavorite = { player.library.toggleFavorite(song.id) },
                    onDelete = {
                        if (playlistId != null) player.library.setPlaylistSongs(playlistId, ids - song.id)
                        else pendingDelete = song
                    }, destructiveTitle = if (playlistId != null) "Quitar de la lista" else "Eliminar archivo",
                    onMoveUp = if (playlistId != null && index > 0) ({
                        player.library.setPlaylistSongs(playlistId, ids.toMutableList().apply { add(index - 1, removeAt(index)) })
                    }) else null,
                    onMoveDown = if (playlistId != null && index >= 0 && index < ids.lastIndex) ({
                        player.library.setPlaylistSongs(playlistId, ids.toMutableList().apply { add(index + 1, removeAt(index)) })
                    }) else null,
                    onAddToPlaylist = { songToAdd = song })
            }
        }
        addedToast?.let { title ->
            androidx.compose.material3.Snackbar(Modifier.align(Alignment.BottomCenter).padding(12.dp),
                containerColor = if (theme.id == "soniq") SoniqPalette.electricBlue else theme.accent) {
                Text("Canción agregada a $title", color = Color.White,
                    fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 12.sp)
            }
        }
    }
    pendingDelete?.let { song ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar canción", color = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground) },
            text = { Text("¿Eliminar ${song.title} del dispositivo?", color = if (theme.id == "soniq") SoniqPalette.secondaryText else theme.muted) },
            confirmButton = { TextButton(onClick = { player.delete(song); pendingDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
            containerColor = if (theme.id == "soniq") Color(0xFFEAF2FB) else theme.background,
            shape = RoundedCornerShape(18.dp))
    }
    songToAdd?.let { song ->
        if (theme.id == "soniq") PlaylistDestinationSheet(song, playlists, player, theme,
            onDismiss = { songToAdd = null }, onAdded = { addedToast = it })
        else AlertDialog(onDismissRequest = { songToAdd = null },
            title = { Text("Agregar a una lista", color = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground) },
            text = {
                if (playlists.isEmpty()) Text("Todavía no tienes listas creadas.")
                else LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        val alreadyIncluded = song.id in playlist.songIds
                        Row(Modifier.fillMaxWidth().clickable(enabled = !alreadyIncluded) {
                            player.library.setPlaylistSongs(playlist.id, playlist.songIds + song.id)
                            songToAdd = null
                        }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (alreadyIncluded) "✓" else "+", color = SoniqPalette.electricBlue,
                                fontFamily = SoniqBodyFont, modifier = Modifier.width(30.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.title, color = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground,
                                    fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default)
                                Text(if (alreadyIncluded) "Ya está en esta lista" else "${playlist.songIds.size} canciones",
                                    color = theme.muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { songToAdd = null }) { Text("Cerrar") } },
            containerColor = if (theme.id == "soniq") Color(0xFFEAF2FB) else theme.background,
            shape = RoundedCornerShape(18.dp))
    }
}
