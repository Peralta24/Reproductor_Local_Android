package mx.musicalesdoris.reproductorlocalandroid.library.components.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.library.PlaylistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import java.io.File

@Composable
fun PlaylistRow(playlist: PlaylistSummary, artwork: File?, theme: PlayerTheme,
                onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    when (theme.variantFor(LibraryComponent.PLAYLIST)) {
        "soniq" -> SoniqPlaylistRow(playlist, artwork, theme, onOpen, onRename, onDelete)
        "aurora" -> AuroraPlaylistRow(playlist, artwork, theme, onOpen, onRename, onDelete)
        else -> NeonPlaylistRow(playlist, artwork, theme, onOpen, onRename, onDelete)
    }
}

@Composable
internal fun PlaylistRowLayout(playlist: PlaylistSummary, artwork: File?, theme: PlayerTheme,
                                onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
                                soniq: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = if (soniq) 6.dp else 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            ArtworkView(artwork, theme, if (soniq) 60.dp else 58.dp, playlist.id)
            Spacer(Modifier.width(if (soniq) 14.dp else 13.dp))
            Column {
                Text(playlist.title, color = theme.foreground, fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val duration = playlist.durationMs?.let { "${it / 60000} min" } ?: "—"
                Text("${playlist.songCount} canciones · $duration", color = theme.muted,
                    fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = if (soniq) 11.sp else 12.sp)
            }
        }
        Box {
            Text("⋮", color = theme.muted, fontSize = 20.sp,
                modifier = Modifier.clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 8.dp))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Editar lista") }, onClick = { expanded = false; onRename() })
                DropdownMenuItem(text = { Text("Eliminar lista") }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}
