package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.rememberArtworkRevisionToken
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow
import kotlinx.coroutines.delay

@Composable
fun SongCollection(songs: List<Song>, selectedId: String?, player: PlayerViewModel,
                   theme: PlayerTheme, emptyTitle: String = "No hay canciones",
                   playlistId: String? = null, modifier: Modifier = Modifier,
                   listState: LazyListState? = null,
                   headerContent: (@Composable () -> Unit)? = null,
                   playlistSongIds: List<String>? = null,
                   artworkRevision: Any? = songs) {
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    var addedToast by remember { mutableStateOf<String?>(null) }
    val playlistIds = playlistSongIds.orEmpty()
    val playlistPositions = remember(playlistId, playlistSongIds) {
        if (playlistId == null) emptyMap() else firstPlaylistSongPositions(playlistIds)
    }
    val effectiveListState = listState ?: rememberLazyListState()
    val artworkRevisionToken = rememberArtworkRevisionToken(artworkRevision, player.library)
    LaunchedEffect(addedToast) {
        if (addedToast != null) {
            delay(2600)
            addedToast = null
        }
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = effectiveListState) {
            headerContent?.let { content -> item(key = "collection-header") { content() } }
            if (songs.isEmpty()) item(key = "collection-empty-state") {
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                    Text(emptyTitle, color = theme.muted)
                }
            } else itemsIndexed(songs, key = { _, song -> song.id }) { itemIndex, song ->
                Column(Modifier.fillMaxWidth()) {
                    if (itemIndex > 0) Spacer(Modifier.height(if (theme.id == "soniq") 2.dp else 8.dp))
                    val ids = playlistIds
                    val playlistIndex = playlistPositions[song.id] ?: -1
                    SongRow(song, song.id == selectedId, player.library.artworkFor(song), theme,
                        onSelect = { player.playCollection(songs, startingWith = song) },
                        onFavorite = { player.library.toggleFavorite(song.id) },
                        onDelete = {
                            if (playlistId != null) player.library.setPlaylistSongs(playlistId, ids - song.id)
                            else pendingDelete = song
                        }, destructiveTitle = if (playlistId != null) "Quitar de la lista" else if (song.contentUri != null) "Quitar de la biblioteca" else "Eliminar archivo",
                        onMoveUp = if (playlistId != null && playlistIndex > 0) ({
                            player.library.setPlaylistSongs(playlistId, ids.toMutableList().apply { add(playlistIndex - 1, removeAt(playlistIndex)) })
                        }) else null,
                        onMoveDown = if (playlistId != null && playlistIndex >= 0 && playlistIndex < ids.lastIndex) ({
                            player.library.setPlaylistSongs(playlistId, ids.toMutableList().apply { add(playlistIndex + 1, removeAt(playlistIndex)) })
                        }) else null,
                        onAddToPlaylist = { songToAdd = song }, artworkRevision = artworkRevisionToken)
                }
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
            text = { Text(if (song.contentUri != null) "¿Quitar ${song.title} de la biblioteca? El archivo permanecerá en el dispositivo."
                else "¿Eliminar ${song.title} del dispositivo?", color = if (theme.id == "soniq") SoniqPalette.secondaryText else theme.muted) },
            confirmButton = { TextButton(onClick = { player.delete(song); pendingDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
            containerColor = if (theme.id == "soniq") Color(0xFFEAF2FB) else theme.background,
            shape = RoundedCornerShape(18.dp))
    }
    songToAdd?.let { song ->
        AddSongToPlaylistDialog(song, player, theme,
            onDismiss = { songToAdd = null }, onAdded = { addedToast = it })
    }
}

@Composable
private fun AddSongToPlaylistDialog(song: Song, player: PlayerViewModel, theme: PlayerTheme,
                                   onDismiss: () -> Unit, onAdded: (String) -> Unit) {
    val playlists by player.library.playlists.collectAsState()
    if (theme.id == "soniq") PlaylistDestinationSheet(song, playlists, player, theme,
        onDismiss = onDismiss, onAdded = onAdded)
    else AlertDialog(onDismissRequest = onDismiss,
            title = { Text("Agregar a una lista", color = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground) },
            text = {
                if (playlists.isEmpty()) Text("Todavía no tienes listas creadas.")
                else LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        val alreadyIncluded = song.id in playlist.songIds
                        Row(Modifier.fillMaxWidth().clickable(enabled = !alreadyIncluded) {
                            player.library.setPlaylistSongs(playlist.id, playlist.songIds + song.id)
                            onDismiss()
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
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
            containerColor = if (theme.id == "soniq") Color(0xFFEAF2FB) else theme.background,
            shape = RoundedCornerShape(18.dp))
}
