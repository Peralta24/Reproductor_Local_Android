package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

/** Category-first selector presentation; updates continue using the existing playlist song operation. */
@Composable
fun PlaylistSongPicker(playlist: Playlist, songs: List<Song>, player: PlayerViewModel,
                       theme: PlayerTheme, onClose: () -> Unit) {
    var category by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    val categories = listOf("Artistas", "Álbumes", "Canciones", "Carpetas")
    val grouped = when (category) {
        "Artistas" -> songs.groupBy { it.artist?.takeIf(String::isNotBlank) ?: "Artista desconocido" }
        "Álbumes" -> songs.groupBy { it.album?.takeIf(String::isNotBlank) ?: "Álbum desconocido" }
        "Carpetas" -> songs.groupBy { player.library.fileFor(it).parentFile?.name ?: "Carpeta local" }
        else -> emptyMap()
    }
    val candidates = when {
        category == "Canciones" -> songs
        filter != null -> grouped[filter].orEmpty()
        else -> emptyList()
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            SoniqPalette.pageTop, SoniqPalette.pageMiddle, SoniqPalette.pageBottom
        ))).padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (category != null) Text("‹", color = SoniqPalette.ink, fontSize = 25.sp,
                    modifier = Modifier.clickable {
                        if (filter != null) filter = null else category = null
                    }.padding(horizontal = 8.dp))
                Text(if (category == null) "Agregar canciones" else filter ?: category.orEmpty(),
                    color = SoniqPalette.ink, fontFamily = SoniqHeadingFont, fontWeight = FontWeight.Bold,
                    fontSize = 23.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = onClose) { Text("Listo", color = SoniqPalette.electricBlue, fontFamily = SoniqBodyFont) }
            }
            Text(if (category == null) "Elige una categoría para explorar tu biblioteca."
                else if (filter == null && category != "Canciones") "Selecciona una opción para ver sus canciones."
                else "${candidates.size} canciones · ${playlist.songIds.size} en la lista",
                color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 14.dp))
            when {
                category == null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(categories) { label ->
                        PickerCategory(label, when (label) {
                            "Artistas" -> songs.mapNotNull { it.artist?.takeIf(String::isNotBlank) }.distinct().size
                            "Álbumes" -> songs.mapNotNull { it.album?.takeIf(String::isNotBlank) }.distinct().size
                            "Carpetas" -> songs.map { player.library.fileFor(it).parentFile?.name ?: "Carpeta local" }.distinct().size
                            else -> songs.size
                        }) { category = label }
                    }
                }
                category != "Canciones" && filter == null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(grouped.keys.sorted()) { name ->
                        PickerCategory(name, grouped[name].orEmpty().size) { filter = name }
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(candidates, key = { it.id }) { song ->
                        val included = song.id in playlist.songIds
                        Row(Modifier.fillMaxWidth().height(58.dp)
                            .background(if (included) Color.White.copy(alpha = .48f) else Color.Transparent,
                                RoundedCornerShape(11.dp))
                            .then(if (included) Modifier.border(1.dp, SoniqPalette.border, RoundedCornerShape(11.dp)) else Modifier)
                            .clickable {
                                val ids = playlist.songIds.toMutableSet()
                                if (included) ids.remove(song.id) else ids.add(song.id)
                                player.library.setPlaylistSongs(playlist.id, ids.toList())
                            }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ArtworkView(player.library.artworkFor(song), theme, 42.dp, song.id)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = if (included) SoniqPalette.electricBlue else SoniqPalette.ink,
                                    fontFamily = SoniqBodyFont, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.subtitle, color = SoniqPalette.secondaryText,
                                    fontFamily = SoniqBodyFont, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(if (included) "✓" else "+", color = SoniqPalette.electricBlue,
                                fontSize = 19.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCategory(label: String, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(65.dp).background(Color.White.copy(alpha = .68f), RoundedCornerShape(12.dp))
        .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("$count ${if (label == "Canciones") "canciones" else "elementos"}",
                color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 10.sp)
        }
        Text("›", color = SoniqPalette.electricBlue, fontSize = 22.sp)
    }
}
