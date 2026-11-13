package mx.musicalesdoris.reproductorlocalandroid.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.header.AppHeader
import mx.musicalesdoris.reproductorlocalandroid.library.components.navigation.LibrarySectionNavigation
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer
import mx.musicalesdoris.reproductorlocalandroid.library.sections.*

/** Matches the iOS composition: one header, one navigation bar and one mini player. */
@Composable
fun LibraryScreen(theme: PlayerTheme, songs: List<Song>, state: PlayerState, player: PlayerViewModel,
                  importAudio: () -> Unit, onPlayer: () -> Unit, onThemes: () -> Unit,
                  onProfile: () -> Unit, onEqualizer: () -> Unit, chooseCover: (String) -> Unit) {
    val playlists by player.library.playlists.collectAsState()
    var section by remember { mutableStateOf(LibrarySection.SONGS) }
    val detailStack = remember { mutableStateListOf<Pair<String, String>>() }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var choosingSongs by remember { mutableStateOf<String?>(null) }
    var deletePlaylist by remember { mutableStateOf<Playlist?>(null) }
    val selected = songs.firstOrNull { it.id == state.selectedId }

    if (creating || editing != null) {
        PlaylistEditorView(editing, player.library, theme) { creating = false; editing = null }
    } else {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.topGlow.copy(alpha = .5f),
            theme.background, theme.bottomGlow.copy(alpha = .35f))))) {
            if (detailStack.isEmpty()) {
                AppHeader(theme, importAudio, onPlayer, onThemes, onProfile, onEqualizer)
                LibrarySectionNavigation(section, theme.variantFor(LibraryComponent.NAVIGATION), theme) { selectedSection ->
                    section = selectedSection
                    detailStack.clear()
                }
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = if (theme.id == "soniq") 14.dp else 20.dp)) {
                val detail = detailStack.lastOrNull()
                if (detail != null) {
                    DetailView(detail.first, detail.second, state, player, theme,
                        onBack = { detailStack.removeAt(detailStack.lastIndex) },
                        onEdit = { editing = it }, onAdd = { choosingSongs = it.id },
                        onCover = chooseCover,
                        onOpenAlbum = { detailStack.add("album" to it) })
                } else when (section) {
                    LibrarySection.SONGS -> SongsView(songs, state, player, theme)
                    LibrarySection.FAVORITES -> FavoritesView(songs, state, player, theme)
                    LibrarySection.PLAYLISTS -> PlaylistsView(player.library, playlists, theme,
                        onCreate = { creating = true },
                        onOpen = { detailStack.add("playlist" to it.id) },
                        onEdit = { editing = it }, onDelete = { deletePlaylist = it })
                    LibrarySection.ALBUMS -> AlbumsView(player.library, theme) { detailStack.add("album" to it.id) }
                    LibrarySection.ARTISTS -> ArtistsView(player.library, theme) { detailStack.add("artist" to it.id) }
                    LibrarySection.GENRES -> GenresView(player.library, theme) { detailStack.add("genre" to it) }
                    LibrarySection.FOLDERS -> FoldersView(player.library, theme) { detailStack.add("folder" to it) }
                }
            }
            MiniPlayer(selected, state, theme, player, onPlayer)
            Spacer(Modifier.height(10.dp))
        }
    }

    choosingSongs?.let { id ->
        val playlist = playlists.firstOrNull { it.id == id }
        if (playlist != null) AlertDialog(onDismissRequest = { choosingSongs = null },
            title = { Text("Añadir canciones") },
            text = { LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(songs, key = { it.id }) { song ->
                    val included = song.id in playlist.songIds
                    Row(Modifier.fillMaxWidth().clickable {
                        player.library.setPlaylistSongs(id,
                            if (included) playlist.songIds - song.id else playlist.songIds + song.id)
                    }.padding(10.dp)) {
                        Text(if (included) "☑" else "□", color = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text(song.title, maxLines = 1)
                    }
                }
            } }, confirmButton = { TextButton(onClick = { choosingSongs = null }) { Text("Listo") } })
        else choosingSongs = null
    }
    deletePlaylist?.let { playlist ->
        AlertDialog(onDismissRequest = { deletePlaylist = null }, title = { Text("Eliminar lista") },
            text = { Text("¿Eliminar ${playlist.title}?") },
            confirmButton = { TextButton(onClick = {
                player.library.deletePlaylist(playlist.id)
                deletePlaylist = null
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deletePlaylist = null }) { Text("Cancelar") } })
    }
}
