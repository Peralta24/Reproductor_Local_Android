package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.components.header.AppHeader
import mx.musicalesdoris.reproductorlocalandroid.library.components.navigation.LibrarySectionNavigation
import mx.musicalesdoris.reproductorlocalandroid.library.components.song.SongRow
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val selected = songs.firstOrNull { it.id == state.selectedId }

    Column(Modifier.fillMaxSize().background(theme.background)) {
        AppHeader(themes.variantFor(LibraryComponent.HEADER, theme), theme, importAudio)
        LibrarySectionNavigation(section, themes.variantFor(LibraryComponent.NAVIGATION, theme), theme) {
            section = it
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Text(section.heading, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
            when (section) {
                LibrarySection.SONGS, LibrarySection.FAVORITES -> {
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
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(visible, key = { it.id }) { song ->
                                SongRow(song, song.id == state.selectedId,
                                    themes.variantFor(LibraryComponent.SONG, theme), theme,
                                    onSelect = { player.select(song) },
                                    onFavorite = { player.library.toggleFavorite(song.id) },
                                    onDelete = { player.delete(song) })
                            }
                        }
                    }
                }
                LibrarySection.PLAYLISTS -> EmptyLibraryState("Aún no hay listas", theme)
                LibrarySection.ALBUMS -> EmptyLibraryState("Aún no hay álbumes", theme)
                LibrarySection.ARTISTS -> EmptyLibraryState("Aún no hay artistas", theme)
            }
        }
        MiniPlayer(selected, state, themes.variantFor(LibraryComponent.MINI_PLAYER, theme), theme,
            onTogglePlayback = player::togglePlayback)
        Spacer(Modifier.height(10.dp))
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
