package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow

@Composable
fun SongCollection(songs: List<Song>, state: PlayerState, player: PlayerViewModel,
                   theme: PlayerTheme, emptyTitle: String = "No hay canciones",
                   playlistId: String? = null, modifier: Modifier = Modifier) {
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    if (songs.isEmpty()) {
        Box(modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
            Text(emptyTitle, color = theme.muted)
        }
    } else LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(if (theme.id == "soniq") 3.dp else 8.dp)) {
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
                }) else null)
        }
    }
    pendingDelete?.let { song ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar canción") }, text = { Text("¿Eliminar ${song.title} del dispositivo?") },
            confirmButton = { TextButton(onClick = { player.delete(song); pendingDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } })
    }
}
