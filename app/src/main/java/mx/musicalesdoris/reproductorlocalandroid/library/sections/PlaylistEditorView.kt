package mx.musicalesdoris.reproductorlocalandroid.library.sections

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.Playlist
import mx.musicalesdoris.reproductorlocalandroid.SongLibrary
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkBitmapLoader
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun PlaylistEditorView(existing: Playlist?, library: SongLibrary, theme: PlayerTheme, onClose: () -> Unit,
                       modifier: Modifier = Modifier) {
    var name by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var coverUri by remember(existing?.id) { mutableStateOf<Uri?>(null) }
    var removeCover by remember(existing?.id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val previewState = produceState<ImageBitmap?>(null, coverUri) {
        value = coverUri?.let { ArtworkBitmapLoader.decodeUriImage(context.contentResolver, it, 300) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            coverUri = uri
            removeCover = false
            saveError = null
        }
    }
    BackHandler(onBack = onClose)
    Column(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.topGlow,
        theme.background, theme.bottomGlow)))) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 26.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "Nueva lista" else "Editar lista", color = theme.foreground,
                    fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("✕", color = theme.foreground, fontSize = 20.sp) }
            }
            Text("Un lugar para las canciones que quieres guardar.", color = theme.muted,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(86.dp).clip(RoundedCornerShape(if (theme.id == "soniq") 7.dp else 10.dp))) {
                    if (previewState.value != null) Image(previewState.value!!, "Previsualización de portada",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else ArtworkView(if (removeCover) null else existing?.let(library::coverFor), theme, 86.dp, existing?.id ?: name)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Portada de la lista", color = theme.foreground,
                        fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                        Text(if (existing?.coverFileName != null || coverUri != null) "Cambiar imagen" else "Añadir imagen",
                            color = theme.accent)
                    }
                    if (existing?.coverFileName != null || coverUri != null) TextButton(onClick = {
                        coverUri = null; removeCover = true
                    }) { Text("Quitar imagen", color = theme.muted, fontSize = 11.sp) }
                }
            }
            saveError?.let { Text(it, color = theme.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Nombre de la lista", color = theme.foreground,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(name, { name = it.take(60) }, placeholder = { Text("Ej. Órbita nocturna") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = if (theme.id == "soniq") OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SoniqPalette.ink, unfocusedTextColor = SoniqPalette.ink,
                    focusedBorderColor = SoniqPalette.electricBlue, unfocusedBorderColor = SoniqPalette.border,
                    focusedContainerColor = SoniqPalette.paper, unfocusedContainerColor = SoniqPalette.paper,
                    focusedPlaceholderColor = SoniqPalette.mutedInk, unfocusedPlaceholderColor = SoniqPalette.mutedInk
                ) else OutlinedTextFieldDefaults.colors())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Obligatorio", color = theme.muted, fontSize = 11.sp)
                Text("${name.length} / 60", color = theme.muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Descripción", color = theme.foreground,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(description, { description = it }, placeholder = { Text("¿Qué te gustaría escuchar aquí?") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
                colors = if (theme.id == "soniq") OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SoniqPalette.ink, unfocusedTextColor = SoniqPalette.ink,
                    focusedBorderColor = SoniqPalette.electricBlue, unfocusedBorderColor = SoniqPalette.border,
                    focusedContainerColor = SoniqPalette.paper, unfocusedContainerColor = SoniqPalette.paper,
                    focusedPlaceholderColor = SoniqPalette.mutedInk, unfocusedPlaceholderColor = SoniqPalette.mutedInk
                ) else OutlinedTextFieldDefaults.colors())
            Spacer(Modifier.height(18.dp))
            Text("Se guardará en tu dispositivo.\nPodrás añadir canciones después.",
                color = theme.muted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onClose, enabled = !saving, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.surface, contentColor = theme.foreground),
                shape = RoundedCornerShape(11.dp)) { Text("Cancelar") }
            Button(onClick = {
                saving = true
                saveError = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        val playlist = if (existing == null) library.createPlaylist(name, description)
                            else { library.renamePlaylist(existing.id, name, description); existing }
                        when {
                            playlist == null -> "Escribe un nombre para la lista."
                            removeCover -> { library.removePlaylistCover(playlist.id); null }
                            coverUri != null && !library.setPlaylistCover(playlist.id, coverUri!!) ->
                                library.error.value.also { if (existing == null) library.deletePlaylist(playlist.id) }
                            else -> null
                        }
                    }
                    saving = false
                    if (result == null) onClose() else saveError = result
                }
            }, enabled = name.isNotBlank() && !saving, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(11.dp)) {
                Text(if (existing == null) "Crear lista" else "Guardar cambios")
            }
        }
    }
}
