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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.SongLibrary
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.albumKey
import mx.musicalesdoris.reproductorlocalandroid.key
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.rememberArtworkRevisionToken
import mx.musicalesdoris.reproductorlocalandroid.library.components.artist.ArtistRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.playlist.PlaylistRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.genre.GenreRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.folder.FolderRow
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun PlaylistsView(library: SongLibrary, songs: List<Song>, playlists: List<Playlist>, theme: PlayerTheme,
                  onCreate: () -> Unit, onOpen: (Playlist) -> Unit,
                  onEdit: (Playlist) -> Unit, onDelete: (Playlist) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val sorted = remember(playlists, newestFirst) {
        playlists.sortedBy { it.updatedAt }.let { if (newestFirst) it.reversed() else it }
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(top = if (theme.id == "soniq") 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tus listas", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = if (theme.id == "soniq") 29.sp else 28.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onCreate) { Text("＋ Nueva lista", color = if (theme.id == "soniq") Color.White else theme.accent,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 12.sp, modifier = if (theme.id == "soniq") Modifier.background(
                    Brush.verticalGradient(listOf(SoniqPalette.electricBlue, Color(0xFF367BF5))), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp) else Modifier) }
        }
        CollectionMeta(playlists.size, "lista", "listas", newestFirst, theme) { newestFirst = !newestFirst }
        if (sorted.isEmpty()) EmptyCollection("Aún no hay listas", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(if (theme.id == "soniq") 4.dp else 8.dp)) {
            items(sorted, key = { it.id }) { playlist ->
                val summary = remember(playlist, songs) { library.playlistSummary(playlist) }
                PlaylistRow(summary, library.coverFor(playlist), theme,
                    onOpen = { onOpen(playlist) }, onRename = { onEdit(playlist) }, onDelete = { onDelete(playlist) })
            }
        }
    }
}

@Composable
fun AlbumsView(library: SongLibrary, theme: PlayerTheme, onOpen: (AlbumSummary) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val catalog by library.songs.collectAsState()
    val artworkRevisionToken = rememberArtworkRevisionToken(catalog, library)
    val sortedAlbums = remember(catalog) { library.albums().sortedByDescending { it.latestImportedAt } }
    val albums = remember(sortedAlbums, newestFirst) { if (newestFirst) sortedAlbums else sortedAlbums.asReversed() }
    val artworkSongs = remember(catalog) { catalog.artworkRepresentatives { it.albumKey() } }
    Column {
        Text("Tus álbumes", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (theme.id == "soniq") 12.dp else 0.dp))
        CollectionMeta(albums.size, "álbum", "álbumes", newestFirst, theme) { newestFirst = !newestFirst }
        if (albums.isEmpty()) EmptyCollection("Aún no hay álbumes", theme)
        else LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp)) {
            items(albums, key = { it.id }) { album ->
                val art = artworkSongs[album.id]?.let(library::artworkFor)
                AlbumCard(album, art, theme, artworkRevision = artworkRevisionToken) { onOpen(album) }
            }
        }
    }
}

@Composable
fun ArtistsView(library: SongLibrary, theme: PlayerTheme, onOpen: (ArtistSummary) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val catalog by library.songs.collectAsState()
    val artworkRevisionToken = rememberArtworkRevisionToken(catalog, library)
    val sortedArtists = remember(catalog) { library.artists().sortedByDescending { it.latestImportedAt } }
    val artists = remember(sortedArtists, newestFirst) { if (newestFirst) sortedArtists else sortedArtists.asReversed() }
    val artworkSongs = remember(catalog) { catalog.artworkRepresentatives { it.artist.key() } }
    Column {
        Text("Tus artistas", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (theme.id == "soniq") 12.dp else 0.dp))
        CollectionMeta(artists.size, "artista", "artistas", newestFirst, theme) { newestFirst = !newestFirst }
        if (artists.isEmpty()) EmptyCollection("Aún no hay artistas", theme)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(artists, key = { it.id }) { artist ->
                val art = artworkSongs[artist.id]?.let(library::artworkFor)
                ArtistRow(artist, art, theme, artworkRevision = artworkRevisionToken) { onOpen(artist) }
            }
        }
    }
}

private inline fun List<Song>.artworkRepresentatives(keyFor: (Song) -> String?): Map<String, Song> {
    val result = HashMap<String, Song>()
    for (song in this) {
        val key = keyFor(song) ?: continue
        if (key !in result || (result[key]?.artworkFileName == null && song.artworkFileName != null))
            result[key] = song
    }
    return result
}

@Composable
fun GenresView(library: SongLibrary, theme: PlayerTheme, onOpen: (String) -> Unit) {
    var newestFirst by remember { mutableStateOf(true) }
    val catalog by library.songs.collectAsState()
    val genres = remember(catalog, newestFirst) {
        library.genres().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    }
    Column {
        Text("Tus géneros", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (theme.id == "soniq") 12.dp else 0.dp))
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
    val catalog by library.songs.collectAsState()
    val folders = remember(catalog, newestFirst) {
        library.folders().sortedBy { it.latestImportedAt }.let { if (newestFirst) it.reversed() else it }
    }
    Column {
        Text("Tus carpetas", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (theme.id == "soniq") 25.sp else 28.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (theme.id == "soniq") 12.dp else 0.dp))
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
            fontFamily = if (theme.id == "soniq") SoniqMonoFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (theme.id == "soniq") 9.sp else 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onSort) { Text(if (theme.id == "soniq") "Recientes ⇵" else if (newestFirst) "Recientes ↓" else "Recientes ↑",
            color = theme.muted, fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default, fontSize = 11.sp) }
    }
}

@Composable
private fun EmptyCollection(title: String, theme: PlayerTheme) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, color = theme.muted)
    }
}
