package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.SongLibrary
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artist.ArtistRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.playlist.PlaylistRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.genre.GenreRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.folder.FolderRow

@Composable
fun PlaylistsView(library: SongLibrary, playlists: List<Playlist>, theme: PlayerTheme,
                  onCreate: () -> Unit, onOpen: (Playlist) -> Unit,
                  onEdit: (Playlist) -> Unit, onDelete: (Playlist) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val sorted = playlists.sortedBy { it.updatedAt }.let { if (newestFirst) it.reversed() else it }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tus listas", color = theme.foreground, fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onCreate) { Text("+ Nueva lista", color = theme.accent) }
        }
        CollectionMeta(playlists.size, "lista", "listas", newestFirst, theme) { newestFirst = !newestFirst }
        if (sorted.isEmpty()) EmptyCollection("Aún no hay listas", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sorted, key = { it.id }) { playlist ->
                PlaylistRow(library.playlistSummary(playlist), library.coverFor(playlist), theme,
                    onOpen = { onOpen(playlist) }, onRename = { onEdit(playlist) }, onDelete = { onDelete(playlist) })
            }
        }
    }
}

@Composable
fun AlbumsView(library: SongLibrary, theme: PlayerTheme, onOpen: (AlbumSummary) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val albums = library.albums().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    Column {
        Text("Tus álbumes", color = theme.foreground, fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold)
        CollectionMeta(albums.size, "álbum", "álbumes", newestFirst, theme) { newestFirst = !newestFirst }
        if (albums.isEmpty()) EmptyCollection("Aún no hay álbumes", theme)
        else LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp)) {
            items(albums, key = { it.id }) { album ->
                val art = library.songsInAlbum(album.id).firstOrNull()?.let(library::artworkFor)
                AlbumCard(album, art, theme) { onOpen(album) }
            }
        }
    }
}

@Composable
fun ArtistsView(library: SongLibrary, theme: PlayerTheme, onOpen: (ArtistSummary) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val artists = library.artists().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    Column {
        Text("Tus artistas", color = theme.foreground, fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold)
        CollectionMeta(artists.size, "artista", "artistas", newestFirst, theme) { newestFirst = !newestFirst }
        if (artists.isEmpty()) EmptyCollection("Aún no hay artistas", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(artists, key = { it.id }) { artist ->
                val art = library.songsByArtist(artist.id).firstOrNull()?.let(library::artworkFor)
                ArtistRow(artist, art, theme) { onOpen(artist) }
            }
        }
    }
}

@Composable
fun GenresView(library: SongLibrary, theme: PlayerTheme, onOpen: (String) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val genres = library.genres().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    Column {
        Text("Tus géneros", color = theme.foreground, fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold)
        CollectionMeta(genres.size, "género", "géneros", newestFirst, theme) { newestFirst = !newestFirst }
        if (genres.isEmpty()) EmptyCollection("Aún no hay géneros", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(genres, key = { it.id }) { genre -> GenreRow(genre, theme) { onOpen(genre.id) } }
        }
    }
}

@Composable
fun FoldersView(library: SongLibrary, theme: PlayerTheme, onOpen: (String) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val folders = library.folders().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    Column {
        Text("Tus carpetas", color = theme.foreground, fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold)
        CollectionMeta(folders.size, "carpeta", "carpetas", newestFirst, theme) { newestFirst = !newestFirst }
        if (folders.isEmpty()) EmptyCollection("Aún no hay carpetas", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders, key = { it.id }) { folder -> FolderRow(folder, theme) { onOpen(folder.id) } }
        }
    }
}

@Composable
private fun CollectionMeta(count: Int, singular: String, plural: String, newestFirst: Boolean,
                           theme: PlayerTheme, onSort: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$count ${if (count == 1) singular else plural} · En tu dispositivo", color = theme.muted,
            fontSize = if (theme.id == "soniq") 10.sp else 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onSort) { Text(if (newestFirst) "Recientes ↓" else "Recientes ↑",
            color = theme.muted, fontSize = 11.sp) }
    }
}

@Composable
private fun EmptyCollection(title: String, theme: PlayerTheme) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, color = theme.muted)
    }
}
