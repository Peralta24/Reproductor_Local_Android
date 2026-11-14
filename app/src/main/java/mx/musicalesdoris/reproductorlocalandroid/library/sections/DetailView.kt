package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.albumKey
import mx.musicalesdoris.reproductorlocalandroid.library.components.album.AlbumCard
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.SoniqCdCase
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.PlaylistPlaceholderCover
import mx.musicalesdoris.reproductorlocalandroid.library.components.header.HeaderMenu
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.soniqClickable

@Composable
fun DetailView(kind: String, key: String, state: PlayerState, player: PlayerViewModel,
               theme: PlayerTheme, onBack: () -> Unit, onEdit: (Playlist) -> Unit,
               onAdd: (Playlist) -> Unit, onCover: (String) -> Unit,
               onOpenAlbum: (String) -> Unit, onImport: () -> Unit = {},
               onPlayer: () -> Unit = {}, onThemes: () -> Unit = {},
               onProfile: () -> Unit = {}, onEqualizer: () -> Unit = {}) {
    val library = player.library
    val playlist = library.playlists.value.firstOrNull { it.id == key }
    val members = when (kind) {
        "album" -> library.songsInAlbum(key)
        "artist" -> library.songsByArtist(key)
        "genre" -> library.songsInGenre(key)
        "folder" -> library.songsInFolder(key)
        else -> playlist?.let(library::songsIn).orEmpty()
    }
    val title = when (kind) {
        "album" -> members.firstOrNull()?.album ?: "Álbum"
        "artist" -> members.firstOrNull()?.artist ?: "Artista"
        "genre" -> members.firstOrNull()?.genre ?: "Género"
        "folder" -> key.substringAfterLast('/').ifBlank { key }
        else -> playlist?.title ?: "Lista"
    }
    var descending by remember(kind, key) { mutableStateOf(false) }
    val scrollState = remember(kind, key) { LazyListState() }
    val ordered = remember(members, descending) { if (descending) members.reversed() else members }
    val artistAlbums = remember(library, kind, members) {
        if (kind == "artist") library.albums().filter { album -> members.any { it.albumKey() == album.id } }
        else emptyList()
    }
    val artistAlbumRows = remember(artistAlbums) { artistAlbums.chunked(2) }
    val duration = if (members.all { it.durationMs != null }) {
        val minutes = members.sumOf { it.durationMs ?: 0L } / 60000
        if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
    } else "—"
    val collectionHeader: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            if (theme.id == "soniq") {
                val art = when (kind) {
                    "playlist" -> playlist?.let(library::coverFor)
                    else -> members.firstOrNull()?.let(library::artworkFor)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (kind == "album") SoniqCdCase(art, theme, 215.dp, key)
                    else if (kind == "playlist" && art == null) PlaylistPlaceholderCover(174.dp)
                    else ArtworkView(art, theme, 174.dp, key, circular = kind == "artist")
                }
                Spacer(Modifier.height(18.dp))
            }
            Text(title, color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = if (theme.id == "soniq") 29.sp else 26.sp,
                fontWeight = FontWeight.Bold, textAlign = if (theme.id == "soniq") TextAlign.Center else TextAlign.Start,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(5.dp))
            Text("${members.size} canciones · $duration", color = theme.muted,
                fontFamily = if (theme.id == "soniq") SoniqMonoFont else androidx.compose.ui.text.font.FontFamily.Default, fontSize = 10.sp,
                textAlign = if (theme.id == "soniq") TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth())
            if (kind == "album") Text(members.firstOrNull()?.artist ?: "Artista desconocido",
                color = theme.muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (kind == "folder") Text(key, color = theme.muted, fontSize = 11.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            if (playlist?.description?.isNotBlank() == true) Text(playlist.description, color = theme.muted,
                fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            if (playlist != null && theme.id == "soniq") TextButton(onClick = { onCover(playlist.id) },
                modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cambiar portada", color = theme.accent) }
            Spacer(Modifier.height(15.dp))
            CollectionActions(ordered.isNotEmpty(), theme,
                onPlay = { player.playCollection(ordered) },
                onShuffle = { player.playCollection(ordered, shuffled = true) })
            Spacer(Modifier.height(18.dp))
            if (kind == "artist") {
                Text("Álbumes", color = theme.foreground, fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (kind == "playlist") "Canciones de la lista" else "Canciones", color = theme.foreground,
                        fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { descending = !descending }) {
                        Text(if (descending) "Orden ↑" else "Orden ↓", color = theme.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = if (theme.id == "soniq") 20.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ ${when (kind) {
                "album" -> "Álbumes"; "artist" -> "Artistas"; "genre" -> "Géneros";
                "folder" -> "Carpetas"; else -> "Listas" }}", color = theme.accent) }
            Spacer(Modifier.weight(1f))
            if (playlist != null && !(theme.id == "soniq" && members.isEmpty())) {
                TextButton(onClick = { onEdit(playlist) }) { Text("Editar", color = theme.accent) }
                TextButton(onClick = { onAdd(playlist) }) { Text("Añadir", color = theme.accent) }
            }
            HeaderMenu(theme, theme.id == "soniq", onImport, onPlayer, onThemes, onProfile, onEqualizer)
        }
        if (theme.id == "soniq" && kind == "playlist" && members.isEmpty()) {
            val playlistCover = playlist?.let(library::coverFor)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scrollState) {
                item(key = "empty-playlist-content") {
                    Column(Modifier.fillParentMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.weight(.7f))
                        if (playlistCover != null) ArtworkView(playlistCover, theme, 140.dp, playlist?.id.orEmpty())
                        else PlaylistPlaceholderCover(140.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(title, color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
                            fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("0 canciones · En tu dispositivo", color = SoniqPalette.secondaryText,
                            fontFamily = SoniqMonoFont, fontSize = 9.sp)
                        Spacer(Modifier.weight(1f))
                        Text("Esta lista está vacía", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
                            fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Agrega canciones de tu biblioteca\npara empezar a darle forma.",
                            color = SoniqPalette.secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 7.dp))
                        Spacer(Modifier.height(20.dp))
                        Box(Modifier.fillMaxWidth(.68f).height(48.dp).clip(RoundedCornerShape(11.dp)).background(
                            Brush.verticalGradient(listOf(SoniqPalette.electricBlue, Color(0xFF367CF2))),
                            RoundedCornerShape(11.dp)).soniqClickable(RoundedCornerShape(11.dp), enabled = playlist != null) {
                                playlist?.let(onAdd)
                            }, contentAlignment = Alignment.Center) {
                            Text("Agregar canciones", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.weight(1.35f))
                    }
                }
            }
        } else if (kind == "artist") {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scrollState) {
                item(key = "artist-detail-header") { collectionHeader() }
                itemsIndexed(artistAlbumRows, key = { _, row -> row.first().id }) { rowIndex, row ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { album ->
                                Box(Modifier.weight(1f)) {
                                    AlbumCard(album, library.songsInAlbum(album.id).firstOrNull()?.let(library::artworkFor), theme) {
                                        onOpenAlbum(album.id)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        if (rowIndex < artistAlbumRows.lastIndex) Spacer(Modifier.height(20.dp))
                    }
                }
            }
        } else {
            SongCollection(ordered, state, player, theme,
                emptyTitle = if (playlist != null) "Esta lista está vacía" else "No hay canciones",
                playlistId = playlist?.id, modifier = Modifier.weight(1f),
                listState = scrollState, headerContent = collectionHeader)
        }
    }
}
