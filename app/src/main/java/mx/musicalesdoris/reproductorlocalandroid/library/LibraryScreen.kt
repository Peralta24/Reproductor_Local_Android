package mx.musicalesdoris.reproductorlocalandroid.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

/** Matches the iOS composition: one header, one navigation bar and one mini player. */
@Composable
fun LibraryScreen(theme: PlayerTheme, songs: List<Song>, state: PlayerState, player: PlayerViewModel,
                  importAudio: () -> Unit, onPlayer: () -> Unit, onThemes: () -> Unit,
                  onProfile: () -> Unit, onEqualizer: () -> Unit, chooseCover: (String) -> Unit,
                  section: LibrarySection, onSectionChange: (LibrarySection) -> Unit,
                  detailStack: SnapshotStateList<Pair<String, String>>, systemBackEnabled: Boolean) {
    val playlists by player.library.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var importing by remember { mutableStateOf(false) }
    var choosingSongs by remember { mutableStateOf<String?>(null) }
    var deletePlaylist by remember { mutableStateOf<Playlist?>(null) }
    val selected = songs.firstOrNull { it.id == state.selectedId }

    BackHandler(enabled = systemBackEnabled && detailStack.isNotEmpty() && !creating && editing == null && !importing &&
        choosingSongs == null && deletePlaylist == null) {
        detailStack.removeAt(detailStack.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            if (theme.id == "soniq") SoniqPalette.pageTop else theme.topGlow,
            if (theme.id == "soniq") SoniqPalette.pageMiddle else theme.background,
            if (theme.id == "soniq") SoniqPalette.pageBottom else theme.bottomGlow
        )))) {
        if (theme.id == "soniq") Spacer(Modifier.height(20.dp))
        when {
            creating || editing != null -> PlaylistEditorView(editing, player.library, theme,
                onClose = { creating = false; editing = null }, modifier = Modifier.weight(1f))
            importing -> ImportScreen(importAudio, onClose = { importing = false }, modifier = Modifier.weight(1f))
            else -> {
                if (detailStack.isEmpty()) {
                    AppHeader(theme, { importing = true }, onPlayer, onThemes, onProfile, onEqualizer)
                    LibrarySectionNavigation(section, theme.variantFor(LibraryComponent.NAVIGATION), theme) { selectedSection ->
                        onSectionChange(selectedSection)
                        detailStack.clear()
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    val detail = detailStack.lastOrNull()
                    if (detail != null) {
                        DetailView(detail.first, detail.second, state, player, theme,
                            onBack = { detailStack.removeAt(detailStack.lastIndex) },
                            onEdit = { editing = it }, onAdd = { choosingSongs = it.id },
                            onCover = chooseCover,
                            onOpenAlbum = { detailStack.add("album" to it) },
                            onImport = { importing = true }, onPlayer = onPlayer,
                            onThemes = onThemes, onProfile = onProfile, onEqualizer = onEqualizer)
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
            }
        }
        MiniPlayer(selected, state, theme, player, onPlayer)
        Spacer(Modifier.height(10.dp))
    }

    choosingSongs?.let { id ->
        val playlist = playlists.firstOrNull { it.id == id }
        if (playlist != null) PlaylistSongPicker(playlist, songs, player, theme) { choosingSongs = null }
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
