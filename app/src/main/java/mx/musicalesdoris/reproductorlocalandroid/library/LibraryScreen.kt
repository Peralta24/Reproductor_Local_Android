package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.components.header.AppHeader
import mx.musicalesdoris.reproductorlocalandroid.library.components.navigation.LibrarySectionNavigation
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artist.ArtistRow

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(
    theme: PlayerTheme,
    songs: List<Song>,
    state: PlayerState,
    player: PlayerViewModel,
    themes: ThemeViewModel,
    importAudio: () -> Unit
) {
    // La colección se observa para reconstruir la pieza cuando el usuario cambia su diseño.
    val overrides by themes.componentOverrides.collectAsState()
    var section by remember { mutableStateOf(LibrarySection.SONGS) }
    var query by remember { mutableStateOf("") }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    val selected = songs.firstOrNull { it.id == state.selectedId }
    val albums = remember(songs) { LibraryGrouping.albums(songs) }
    val artists = remember(songs) { LibraryGrouping.artists(songs) }
    val selectedAlbum = albums.firstOrNull { it.id == selectedAlbumId }
    val selectedArtist = artists.firstOrNull { it.id == selectedArtistId }

    Column(Modifier.fillMaxSize().background(theme.background)) {
        AppHeader(themes.variantFor(LibraryComponent.HEADER, theme), theme, importAudio)
        LibrarySectionNavigation(section, themes.variantFor(LibraryComponent.NAVIGATION, theme), theme) {
            section = it
            selectedAlbumId = null
            selectedArtistId = null
        }
        if (state.isImporting) {
            Text(
                "Importando canciones…",
                color = Color.White.copy(alpha = .75f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            if (selectedAlbum != null || selectedArtist != null) {
                CollectionBackButton(
                    destination = if (selectedAlbum != null) "Álbumes" else "Artistas",
                    onClick = {
                        selectedAlbumId = null
                        selectedArtistId = null
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(selectedAlbum?.title ?: selectedArtist?.name ?: section.heading,
                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
            when {
                selectedAlbum != null -> {
                    val collectionSongs = selectedAlbum.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                    SongRows(collectionSongs, state, player, themes, theme, queue = collectionSongs)
                }
                selectedArtist != null -> {
                    val collectionSongs = selectedArtist.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                    SongRows(collectionSongs, state, player, themes, theme, queue = collectionSongs)
                }
                section == LibrarySection.SONGS || section == LibrarySection.FAVORITES -> {
                    OutlinedTextField(query, { query = it }, label = { Text("Buscar canciones") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = theme.accent, focusedLabelColor = theme.accent,
                            unfocusedLabelColor = Color.White.copy(alpha = .65f)))
                    Spacer(Modifier.height(12.dp))
                    val visible = songs.filter { song ->
                        (section != LibrarySection.FAVORITES || song.favorite) &&
                            song.title.contains(query, ignoreCase = true)
                    }
                    if (visible.isEmpty()) {
                        EmptyLibraryState(if (section == LibrarySection.FAVORITES) "Aún no tienes favoritos" else "No hay canciones", theme)
                    } else {
                        SongRows(visible, state, player, themes, theme, queue = visible)
                    }
                }
                section == LibrarySection.ALBUMS -> {
                    if (albums.isEmpty()) EmptyLibraryState("Aún no hay álbumes", theme)
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(albums, key = { it.id }) { album ->
                            AlbumCard(album, player.library.artworkFileForPath(album.artworkPath),
                                themes.variantFor(LibraryComponent.ALBUM, theme), theme,
                                onClick = { selectedAlbumId = album.id })
                        }
                    }
                }
                section == LibrarySection.ARTISTS -> {
                    if (artists.isEmpty()) EmptyLibraryState("Aún no hay artistas", theme)
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(artists, key = { it.id }) { artist ->
                            ArtistRow(artist, themes.variantFor(LibraryComponent.ARTIST, theme), theme,
                                onClick = { selectedArtistId = artist.id })
                        }
                    }
                }
                section == LibrarySection.PLAYLISTS -> EmptyLibraryState("Aún no hay listas", theme)
                else -> EmptyLibraryState("No hay contenido", theme)
            }
        }
        MiniPlayer(selected, state, themes.variantFor(LibraryComponent.MINI_PLAYER, theme), theme,
            onTogglePlayback = player::togglePlayback)
        Spacer(Modifier.height(10.dp))
    }
}

/** Matches the official detail-header control (Figma node 8:3172, "Volver"). */
@Composable
private fun CollectionBackButton(destination: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(90.dp).height(36.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFFA0BEDD)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(
                    Color.White, Color(0xFFD6E8F8), Color(0xFFBFDBF7), Color(0xFFEAF2FB)
                )))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("‹", color = Color(0xFF182231), fontSize = 22.sp, lineHeight = 16.sp)
            Text(destination, color = Color(0xFF182231), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SongRows(
    songs: List<Song>,
    state: PlayerState,
    player: PlayerViewModel,
    themes: ThemeViewModel,
    theme: PlayerTheme,
    queue: List<Song> = songs
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(songs, key = { it.id }) { song ->
            SongRow(song, player.library.artworkFileFor(song), song.id == state.selectedId,
                player.library.isAvailable(song), themes.variantFor(LibraryComponent.SONG, theme), theme,
                onSelect = { player.playCollection(queue, song) },
                onFavorite = { player.toggleFavorite(song.id) },
                onDelete = { player.delete(song) })
        }
    }
}

@Composable
private fun EmptyLibraryState(title: String, theme: PlayerTheme) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♫", color = theme.accent, fontSize = 48.sp)
            Text(title, color = Color.White, fontSize = 20.sp)
            Text("Importa canciones para comenzar.", color = Color.White.copy(alpha = .65f))
        }
    }
}
