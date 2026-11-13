package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@Composable
fun PlaylistDestinationSheet(song: Song, playlists: List<Playlist>, player: PlayerViewModel,
                             theme: PlayerTheme, onDismiss: () -> Unit, onAdded: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.76f)
                .background(Brush.verticalGradient(listOf(Color(0xFFF3F7FC), SoniqPalette.pageTop, SoniqPalette.pageBottom)),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp).background(Color(0xFF9BA8B7), CircleShape))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (creating) "Nueva lista" else "Agregar a…", color = SoniqPalette.ink,
                        fontFamily = SoniqHeadingFont, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("×", color = SoniqPalette.ink, fontSize = 22.sp) }
                }
                if (!creating) {
                    Text("${song.title} · ${song.artist ?: "Artista desconocido"}", color = SoniqPalette.secondaryText,
                        fontFamily = SoniqBodyFont, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 9.dp))
                    if (playlists.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Todavía no tienes listas creadas.", color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont)
                        }
                    } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            val included = song.id in playlist.songIds
                            Row(Modifier.fillMaxWidth().height(62.dp)
                                .clickable(enabled = !included) {
                                    player.library.setPlaylistSongs(playlist.id, playlist.songIds + song.id)
                                    onAdded(playlist.title)
                                    onDismiss()
                                }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ArtworkView(player.library.coverFor(playlist), theme, 42.dp, playlist.id)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(playlist.title, color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (included) "Ya está en esta lista" else "${playlist.songIds.size} canciones",
                                        color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 10.sp)
                                }
                                Text(if (included) "✓" else "›", color = SoniqPalette.electricBlue, fontSize = 20.sp)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(48.dp).background(
                        Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)), RoundedCornerShape(11.dp))
                        .border(1.dp, SoniqPalette.border, RoundedCornerShape(11.dp))
                        .clickable { creating = true }, contentAlignment = Alignment.Center) {
                        Text("＋  Crear nueva lista", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Text("La canción se añadirá a la lista que elijas.", color = SoniqPalette.secondaryText,
                        fontFamily = SoniqBodyFont, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkView(player.library.artworkFor(song), theme, 86.dp, song.id)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Portada de la lista", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("La portada se puede cambiar después.", color = SoniqPalette.secondaryText,
                                fontFamily = SoniqBodyFont, fontSize = 10.sp)
                        }
                    }
                    Text("Nombre de la lista", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(name, { name = it.take(60) }, placeholder = { Text("Ej. Órbita nocturna") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SoniqPalette.ink,
                            unfocusedTextColor = SoniqPalette.ink, focusedBorderColor = SoniqPalette.electricBlue,
                            unfocusedBorderColor = SoniqPalette.border, focusedContainerColor = SoniqPalette.paper,
                            unfocusedContainerColor = SoniqPalette.paper))
                    Text("Descripción", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(description, { description = it }, placeholder = { Text("Opcional") },
                        minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SoniqPalette.ink,
                            unfocusedTextColor = SoniqPalette.ink, focusedBorderColor = SoniqPalette.electricBlue,
                            unfocusedBorderColor = SoniqPalette.border, focusedContainerColor = SoniqPalette.paper,
                            unfocusedContainerColor = SoniqPalette.paper))
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { creating = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = SoniqPalette.ink)
                        }
                        Button(onClick = {
                            player.library.createPlaylist(name, description)?.let { playlist ->
                                player.library.setPlaylistSongs(playlist.id, listOf(song.id))
                                onAdded(playlist.title)
                                onDismiss()
                            }
                        }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SoniqPalette.electricBlue)) {
                            Text("Crear y agregar", fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
