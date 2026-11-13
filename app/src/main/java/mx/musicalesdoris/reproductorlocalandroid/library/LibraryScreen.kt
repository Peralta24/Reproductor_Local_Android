package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.components.header.AppHeader
import mx.musicalesdoris.reproductorlocalandroid.library.components.navigation.LibrarySectionNavigation
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artist.ArtistRow
import mx.musicalesdoris.reproductorlocalandroid.library.playlists.PlaylistSectionContent

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
    var selectedGenreId by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    val selected = songs.firstOrNull { it.id == state.selectedId }
    val albums = remember(songs) { LibraryGrouping.albums(songs) }
    val artists = remember(songs) { LibraryGrouping.artists(songs) }
    val genres = remember(songs) { LibraryGrouping.genres(songs) }
    val folderGrouping by player.folderGrouping.collectAsState()
    val folders = folderGrouping.folders
    val playlistCatalog by player.playlistCatalog.collectAsState()
    val selectedAlbum = albums.firstOrNull { it.id == selectedAlbumId }
    val selectedArtist = artists.firstOrNull { it.id == selectedArtistId }
    val selectedGenre = genres.firstOrNull { it.id == selectedGenreId }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }

    Column(Modifier.fillMaxSize().background(theme.background)) {
        AppHeader(themes.variantFor(LibraryComponent.HEADER, theme), theme, importAudio)
        LibrarySectionNavigation(section, themes.variantFor(LibraryComponent.NAVIGATION, theme), theme) {
            section = it
            selectedAlbumId = null
            selectedArtistId = null
            selectedGenreId = null
            selectedFolderId = null
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
            if (selectedAlbum != null || selectedArtist != null || selectedGenre != null || selectedFolder != null) {
                CollectionBackButton(
                    destination = when {
                        selectedAlbum != null -> "Álbumes"
                        selectedArtist != null -> "Artistas"
                        selectedGenre != null -> "Géneros"
                        else -> "Carpetas"
                    },
                    theme = theme,
                    onClick = {
                        selectedAlbumId = null
                        selectedArtistId = null
                        selectedGenreId = null
                        selectedFolderId = null
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
            if (section != LibrarySection.PLAYLISTS) Text(selectedAlbum?.title ?: selectedArtist?.name
                ?: selectedGenre?.name ?: selectedFolder?.name ?: section.heading,
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
                selectedGenre != null -> {
                    val collectionSongs = selectedGenre.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                    SongRows(collectionSongs, state, player, themes, theme, queue = collectionSongs)
                }
                selectedFolder != null -> {
                    val collectionSongs = selectedFolder.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
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
                section == LibrarySection.GENRES -> {
                    if (genres.isEmpty()) EmptyLibraryState("No hay géneros en los metadatos locales", theme)
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(genres, key = { it.id }) { group ->
                            GroupedLibraryRow(group.name, "${group.songCount} canciones", theme) {
                                selectedGenreId = group.id
                            }
                        }
                    }
                }
                section == LibrarySection.FOLDERS -> {
                    if (folderGrouping.isLoading) Text("Cargando carpetas…", color = Color.White.copy(alpha = .7f))
                    else if (folders.isEmpty()) EmptyLibraryState("No hay archivos disponibles en carpetas locales", theme)
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(folders, key = { it.id }) { group ->
                            GroupedLibraryRow(group.name, "${group.songCount} canciones · Almacenamiento privado de SONIQ", theme) {
                                selectedFolderId = group.id
                            }
                        }
                    }
                }
                section == LibrarySection.PLAYLISTS -> PlaylistSectionContent(
                    songs = songs,
                    catalog = playlistCatalog,
                    player = player,
                    theme = theme,
                    variantId = themes.variantFor(LibraryComponent.PLAYLIST, theme)
                )
                else -> EmptyLibraryState("No hay contenido", theme)
            }
        }
        MiniPlayer(selected, state, themes.variantFor(LibraryComponent.MINI_PLAYER, theme), theme,
            onPrevious = player::previous,
            onTogglePlayback = player::togglePlayback,
            onNext = player::next)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun GroupedLibraryRow(title: String, subtitle: String, theme: PlayerTheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape((theme.corner / 2).coerceIn(12, 18).dp),
        color = Color.White.copy(alpha = .08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .12f))
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("♫", color = theme.accent, fontSize = 25.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
            }
            Text("›", color = theme.accent, fontSize = 24.sp)
        }
    }
}

/** Matches the official detail-header control (Figma node 8:3172, "Volver"). */
@Composable
internal fun CollectionBackButton(destination: String, theme: PlayerTheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(38.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape((theme.corner / 2).coerceIn(12, 18).dp),
        color = theme.background.copy(alpha = .78f),
        border = BorderStroke(1.dp, theme.accent.copy(alpha = .58f)),
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("‹", color = theme.accent, fontSize = 22.sp, lineHeight = 16.sp)
            Text(destination, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    val availability by player.library.availabilityBySongId.collectAsState()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(songs, key = { it.id }) { song ->
            SongRow(song, player.library.artworkFileFor(song), song.id == state.selectedId,
                availability[song.id] == true, themes.variantFor(LibraryComponent.SONG, theme), theme,
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
