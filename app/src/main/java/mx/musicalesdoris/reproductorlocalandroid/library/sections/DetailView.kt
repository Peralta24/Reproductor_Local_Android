package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.albumKey
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView

@Composable
fun DetailView(kind: String, key: String, state: PlayerState, player: PlayerViewModel,
               theme: PlayerTheme, onBack: () -> Unit, onEdit: (Playlist) -> Unit,
               onAdd: (Playlist) -> Unit, onCover: (String) -> Unit,
               onOpenAlbum: (String) -> Unit) {
    val library = player.library
    val playlist = library.playlists.value.firstOrNull { it.id == key }
    val members = when (kind) {
        "album" -> library.songsInAlbum(key)
        "artist" -> library.songsByArtist(key)
        "genre" -> library.songsInGenre(key)
        "folder" -> library.songsInFolder(key)
        else -> playlist?.let(library::songsIn).orEmpty()
    }
    val title = when (kind) {
        "album" -> members.firstOrNull()?.album ?: "Álbum"
        "artist" -> members.firstOrNull()?.artist ?: "Artista"
        "genre" -> members.firstOrNull()?.genre ?: "Género"
        "folder" -> key.substringAfterLast('/').ifBlank { key }
        else -> playlist?.title ?: "Lista"
    }
    var descending by remember(kind, key) { mutableStateOf(false) }
    val ordered = if (descending) members.reversed() else members
    val duration = if (members.all { it.durationMs != null }) {
        val minutes = members.sumOf { it.durationMs ?: 0L } / 60000
        if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
    } else "—"
    Column(Modifier.fillMaxSize().padding(horizontal = if (theme.id == "soniq") 20.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ ${when (kind) {
                "album" -> "Álbumes"; "artist" -> "Artistas"; "genre" -> "Géneros";
                "folder" -> "Carpetas"; else -> "Listas" }}", color = theme.accent) }
            Spacer(Modifier.weight(1f))
            if (playlist != null) {
                TextButton(onClick = { onEdit(playlist) }) { Text("Editar", color = theme.accent) }
                TextButton(onClick = { onAdd(playlist) }) { Text("Añadir", color = theme.accent) }
            }
        }
        if (theme.id == "soniq") {
            val art = when (kind) {
                "playlist" -> playlist?.let(library::coverFor)
                else -> members.firstOrNull()?.let(library::artworkFor)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ArtworkView(art, theme, if (kind == "album") 215.dp else 174.dp,
                    key, circular = kind == "artist")
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(title, color = theme.foreground, fontSize = if (theme.id == "soniq") 29.sp else 26.sp,
            fontWeight = FontWeight.Bold, textAlign = if (theme.id == "soniq") TextAlign.Center else TextAlign.Start,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(5.dp))
        Text("${members.size} canciones · $duration", color = theme.muted, fontSize = 11.sp,
            textAlign = if (theme.id == "soniq") TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth())
        if (kind == "album") Text(members.firstOrNull()?.artist ?: "Artista desconocido",
            color = theme.muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (kind == "folder") Text(key, color = theme.muted, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2)
        if (playlist?.description?.isNotBlank() == true) Text(playlist.description, color = theme.muted,
            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        if (playlist != null && theme.id == "soniq") TextButton(onClick = { onCover(playlist.id) },
            modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cambiar portada", color = theme.accent) }
        Spacer(Modifier.height(15.dp))
        CollectionActions(ordered.isNotEmpty(), theme,
            onPlay = { player.playCollection(ordered) },
            onShuffle = { player.playCollection(ordered, shuffled = true) })
        Spacer(Modifier.height(18.dp))
        if (kind == "artist") {
            val albums = library.albums().filter { album -> members.any { it.albumKey() == album.id } }
            Text("Álbumes", color = theme.foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album, library.songsInAlbum(album.id).firstOrNull()?.let(library::artworkFor), theme) {
                        onOpenAlbum(album.id)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (kind == "playlist") "Canciones de la lista" else "Canciones", color = theme.foreground,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { descending = !descending }) {
                    Text(if (descending) "Orden ↑" else "Orden ↓", color = theme.muted, fontSize = 12.sp)
                }
            }
            SongCollection(ordered, state, player, theme,
                emptyTitle = if (playlist != null) "Esta lista está vacía" else "No hay canciones",
                playlistId = playlist?.id)
        }
    }
}
