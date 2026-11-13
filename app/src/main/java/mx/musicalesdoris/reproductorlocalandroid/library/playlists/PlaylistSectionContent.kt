package mx.musicalesdoris.reproductorlocalandroid.library.playlists

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.*
import mx.musicalesdoris.reproductorlocalandroid.library.CollectionBackButton
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.LocalArtwork
import mx.musicalesdoris.reproductorlocalandroid.library.components.playlist.PlaylistRow

@Composable
fun PlaylistSectionContent(
    songs: List<Song>,
    catalog: PlaylistCatalogState,
    player: PlayerViewModel,
    theme: PlayerTheme,
    variantId: String
) {
    val scope = rememberCoroutineScope()
    val playerState by player.state.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var selectingSongs by remember { mutableStateOf(false) }
    var recovering by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val selected = catalog.playlists.firstOrNull { it.id == selectedId }
    val enabled = !catalog.isLoading && catalog.isReadable

    Column(Modifier.fillMaxSize()) {
        if (selected != null) {
            CollectionBackButton("Listas", theme) { selectedId = null }
            Spacer(Modifier.height(12.dp))
            LocalArtwork(player.playlistArtworkFile(selected.artworkPath), "Portada de ${selected.title}",
                Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(16.dp)) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .07f)), contentAlignment = Alignment.Center) {
                    Text("♫", color = theme.accent, fontSize = 38.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(selected.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    selected.description?.let { Text(it, color = Color.White.copy(alpha = .7f), fontSize = 13.sp, maxLines = 2) }
                    Text("${selected.songIds.size} canciones", color = Color.White.copy(alpha = .68f), fontSize = 13.sp)
                }
                TextButton(enabled = selected.songsIn(songs).any(player.library::isAvailable), onClick = {
                    player.playPlaylist(selected, shuffled = false)
                }) { Text("▶") }
                TextButton(enabled = enabled, onClick = { editing = selected }) { Text("Editar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = enabled, onClick = { selectingSongs = true }) { Text("＋ Añadir canciones") }
                TextButton(
                    enabled = selected.songsIn(songs).any(player.library::isAvailable),
                    onClick = { player.togglePlaylistShuffle(selected) },
                    modifier = Modifier.semantics {
                        stateDescription = if (playerState.shuffle) "Aleatorio activado" else "Aleatorio desactivado"
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (playerState.shuffle) theme.accent else Color.White.copy(alpha = .78f)
                    )
                ) { Text(if (playerState.shuffle) "⇄ Aleatorio ✓" else "⇄ Aleatorio") }
            }
            Spacer(Modifier.height(8.dp))
            if (selected.songIds.isEmpty()) EmptyPlaylistMessage("Esta lista aún no tiene canciones", theme)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selected.songIds, key = { "${selected.id}:$it" }) { songId ->
                    val song = songs.firstOrNull { it.id == songId }
                    PlaylistTrackRow(song, songId == playerState.selectedId,
                        song?.let(player.library::isAvailable) == true,
                        song?.let(player.library::artworkFileFor), theme,
                        onPlay = { if (song != null) player.playPlaylist(selected, startingSongId = song.id) },
                        onRemove = { scope.launch { if (!player.removeSongFromPlaylist(selected.id, songId)) message = player.playlistCatalog.value.error } })
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tus listas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Button(enabled = enabled, onClick = { creating = true }) { Text("＋ Nueva lista") }
            }
            Spacer(Modifier.height(10.dp))
            when {
                catalog.isLoading -> Text("Cargando listas…", color = Color.White.copy(alpha = .7f))
                !catalog.isReadable -> Column {
                    Text(catalog.error ?: "No se pudo leer el catálogo de listas", color = Color(0xFFFF8A80))
                    TextButton(onClick = player::refreshPlaylists) { Text("Reintentar lectura") }
                    if (catalog.needsRecovery) TextButton(onClick = { recovering = true }) { Text("Respaldar y reconstruir") }
                }
                catalog.playlists.isEmpty() -> EmptyPlaylistMessage("Crea tu primera lista", theme)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(catalog.playlists, key = Playlist::id) { playlist ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).clickable { selectedId = playlist.id }) {
                            PlaylistRow(
                                PlaylistSummary(playlist.id, playlist.title, playlist.songIds.size,
                                    playlist.description, playlist.artworkPath),
                                variantId, theme, player.playlistArtworkFile(playlist.artworkPath)
                            )
                            }
                            var menu by remember(playlist.id) { mutableStateOf(false) }
                            Box {
                                TextButton(enabled = enabled, onClick = { menu = true }) { Text("⋮", fontSize = 23.sp) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { menu = false; editing = playlist })
                                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; deleting = playlist })
                                }
                            }
                        }
                    }
                }
            }
        }
        catalog.error?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp)) }
        catalog.recoveryMessage?.let { Text(it, color = theme.accent, modifier = Modifier.padding(top = 8.dp)) }
        message?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp)) }
    }

    if (creating || editing != null) PlaylistEditorDialog(
        initial = editing,
        existingArtwork = editing?.let { player.playlistArtworkFile(it.artworkPath) },
        onDismiss = { creating = false; editing = null },
        onSave = { title, description, artworkUri, removeArtwork ->
            scope.launch {
                val current = editing
                val ok = current?.let {
                    player.updatePlaylist(it.id, title, description, artworkUri, removeArtwork)
                } ?: player.createPlaylist(title, description, artworkUri)
                if (ok) { creating = false; editing = null; message = null }
                else message = player.playlistCatalog.value.error ?: "No se pudo guardar el nombre"
            }
        }
    )
    if (recovering) AlertDialog(
        onDismissRequest = { recovering = false },
        title = { Text("Recuperar listas") },
        text = { Text("El catálogo ilegible se conservará en una copia de respaldo. SONIQ iniciará un catálogo vacío para que puedas continuar.") },
        confirmButton = { TextButton(onClick = { scope.launch {
            if (player.recoverPlaylists()) recovering = false
            else message = player.playlistCatalog.value.error
        } }) { Text("Crear respaldo y continuar") } },
        dismissButton = { TextButton(onClick = { recovering = false }) { Text("Cancelar") } }
    )
    deleting?.let { target -> AlertDialog(
        onDismissRequest = { deleting = null },
        title = { Text("Eliminar lista") },
        text = { Text("¿Quieres eliminar “${target.title}”? Las canciones seguirán en tu biblioteca.") },
        confirmButton = { TextButton(onClick = { scope.launch {
            if (player.deletePlaylist(target.id)) { deleting = null; selectedId = null }
            else message = player.playlistCatalog.value.error ?: "No se pudo eliminar la lista"
        } }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
    ) }
    if (selectingSongs && selected != null) AddSongsDialog(
        songs = songs.filter(player.library::isAvailable),
        existingIds = selected.songIds.toSet(),
        theme = theme,
        onDismiss = { selectingSongs = false },
        onAdd = { ids -> scope.launch {
            if (player.addSongsToPlaylist(selected.id, ids)) selectingSongs = false
            else message = player.playlistCatalog.value.error ?: "No se pudieron añadir las canciones"
        } }
    )
}

@Composable
private fun PlaylistEditorDialog(
    initial: Playlist?,
    existingArtwork: java.io.File?,
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?, Boolean) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var artworkUri by remember(initial?.id) { mutableStateOf<Uri?>(null) }
    var removeArtwork by remember(initial?.id) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { artworkUri = uri; removeArtwork = false }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva lista" else "Editar lista") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaylistCoverPreview(artworkUri, if (removeArtwork) null else existingArtwork)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("image/*") }) {
                        Text(if (artworkUri != null || (existingArtwork != null && !removeArtwork)) "Cambiar portada" else "Añadir portada")
                    }
                    if (artworkUri != null || (existingArtwork != null && !removeArtwork)) {
                        TextButton(onClick = { artworkUri = null; removeArtwork = true }) { Text("Quitar") }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, singleLine = true,
                    label = { Text("Nombre") }, supportingText = { Text("Obligatorio · ${title.length}/60") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                OutlinedTextField(value = description, onValueChange = { description = it.take(500) },
                    label = { Text("Descripción · Opcional") }, minLines = 2, maxLines = 4,
                    supportingText = { Text("${description.length}/500") })
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
            onSave(title.trim(), description.trim(), artworkUri, removeArtwork)
        }) { Text(if (initial == null) "Crear" else "Guardar cambios") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun PlaylistCoverPreview(uri: Uri?, existing: java.io.File?) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = uri?.let { selected -> withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(selected)?.use(BitmapFactory::decodeStream) }.getOrNull()
        } }
    }
    Box(Modifier.fillMaxWidth().height(130.dp).background(Color.White.copy(alpha = .07f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap!!.asImageBitmap(), "Vista previa de portada", Modifier.fillMaxSize())
            uri == null && existing != null -> LocalArtwork(existing, "Portada actual", Modifier.fillMaxSize(), RoundedCornerShape(16.dp)) {
                Text("♫", color = Color.White, fontSize = 34.sp)
            }
            else -> Text("♫", color = Color.White.copy(alpha = .7f), fontSize = 34.sp)
        }
    }
}

@Composable
private fun AddSongsDialog(songs: List<Song>, existingIds: Set<String>, theme: PlayerTheme,
                           onDismiss: () -> Unit, onAdd: (List<String>) -> Unit) {
    val chosen = remember { mutableStateListOf<String>() }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Añadir canciones") },
        text = {
            if (songs.isEmpty()) Text("No hay canciones disponibles para añadir.")
            else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(songs, key = Song::id) { song ->
                    val alreadyAdded = song.id in existingIds
                    Row(Modifier.fillMaxWidth().clickable(enabled = !alreadyAdded) {
                        if (song.id in chosen) chosen.remove(song.id) else chosen.add(song.id)
                    }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = alreadyAdded || song.id in chosen, enabled = !alreadyAdded,
                            onCheckedChange = { checked -> if (checked) chosen.add(song.id) else chosen.remove(song.id) })
                        Column {
                            Text(song.title, color = Color.White)
                            Text(song.artist ?: "Artista desconocido", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = chosen.isNotEmpty(), onClick = { onAdd(songs.map(Song::id).filter(chosen::contains)) }) { Text("Añadir (${chosen.size})") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun PlaylistTrackRow(song: Song?, selected: Boolean, available: Boolean, artwork: java.io.File?, theme: PlayerTheme,
                             onPlay: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .07f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        LocalArtwork(artwork, song?.title ?: "Canción no disponible", Modifier.size(46.dp), RoundedCornerShape(10.dp)) {
            Text("♫", color = theme.accent, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f).clickable(enabled = available, onClick = onPlay).padding(horizontal = 10.dp)) {
            Text(song?.title ?: "Canción no disponible", color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (available) listOfNotNull(song?.artist, song?.album).joinToString(" · ").ifBlank { "Archivo local" }
                else "Archivo no disponible", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
        }
        TextButton(onClick = onRemove) { Text("Quitar") }
    }
}

@Composable
private fun EmptyPlaylistMessage(text: String, theme: PlayerTheme) {
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = theme.accent, fontSize = 16.sp)
    }
}
