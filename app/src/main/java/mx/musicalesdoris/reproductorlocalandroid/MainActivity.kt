package mx.musicalesdoris.reproductorlocalandroid

import mx.musicalesdoris.reproductorlocalandroid.library.LibraryScreen
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.LocalArtwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.ReproductorLocalAndroidTheme

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels()
    private val themes: ThemeViewModel by viewModels()
    private val importer = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) player.importSongs(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            val selectedId by themes.selectedId.collectAsState()
            val theme = PlayerTheme.all.firstOrNull { it.id == selectedId } ?: PlayerTheme.all.first()
            ReproductorLocalAndroidTheme(darkTheme = true, dynamicColor = false) {
                AppScreen(player, themes, theme) { importer.launch(arrayOf("audio/*")) }
            }
        }
    }
}

@Composable
private fun AppScreen(player: PlayerViewModel, themes: ThemeViewModel, theme: PlayerTheme, importAudio: () -> Unit) {
    val songs by player.library.songs.collectAsState()
    val state by player.state.collectAsState()
    val libraryError by player.library.error.collectAsState()
    val catalogNeedsRecovery by player.library.catalogNeedsRecovery.collectAsState()
    val catalogLoading by player.library.catalogLoading.collectAsState()
    val catalogRecoveryInProgress by player.catalogRecoveryInProgress.collectAsState()
    val recoveryMessage by player.library.recoveryMessage.collectAsState()
    val selectedId by themes.selectedId.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showingQueue by remember { mutableStateOf(false) }
    var recoveryPromptDismissed by remember { mutableStateOf(false) }
    val selected = songs.firstOrNull { it.id == state.selectedId }
    val error = if (catalogNeedsRecovery) {
        libraryError ?: state.error
    } else {
        state.error ?: libraryError ?: recoveryMessage
    }

    LaunchedEffect(error) { recoveryPromptDismissed = false }
    BackHandler(enabled = showingQueue) { showingQueue = false }

    if (error != null && (!catalogNeedsRecovery || !recoveryPromptDismissed)) AlertDialog(
        onDismissRequest = {
            if (catalogNeedsRecovery) recoveryPromptDismissed = true else player.clearError()
        },
        title = {
            Text(when {
                catalogNeedsRecovery -> "Catálogo ilegible"
                error == recoveryMessage -> "Recuperación completada"
                else -> "Error"
            })
        },
        text = { Text(error) },
        confirmButton = {
            if (catalogNeedsRecovery) {
                TextButton(
                    onClick = player::recoverCatalog,
                    enabled = !catalogRecoveryInProgress
                ) { Text(if (catalogRecoveryInProgress) "Guardando respaldo…" else "Respaldar y reconstruir") }
            } else {
                TextButton(onClick = player::clearError) { Text("Aceptar") }
            }
        },
        dismissButton = if (catalogNeedsRecovery) {
            { TextButton(onClick = { recoveryPromptDismissed = true }) { Text("Cerrar") } }
        } else null
    )

    Scaffold(
        containerColor = theme.background,
        bottomBar = {
            NavigationBar(containerColor = theme.background) {
                listOf("Reproductor" to "▶", "Biblioteca" to "♫", "Temas" to "◈").forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index, onClick = { tab = index; if (index != 0) showingQueue = false },
                        icon = { Text(item.second, fontSize = 22.sp) }, label = { Text(item.first) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                            indicatorColor = theme.accent.copy(alpha = .14f),
                            unselectedIconColor = Color.White.copy(alpha = .65f),
                            unselectedTextColor = Color.White.copy(alpha = .65f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(theme.topGlow.copy(alpha = .42f), theme.background, theme.bottomGlow.copy(alpha = .20f)))
        )) {
            if (catalogLoading && tab != 2) {
                CircularProgressIndicator(color = theme.accent, modifier = Modifier.align(Alignment.Center))
            } else when (tab) {
                0 -> if (showingQueue) QueueScreen(
                    theme = theme, songs = songs, state = state, player = player,
                    onBack = { showingQueue = false }
                ) else PlayerScreen(theme, selected, state, player, importAudio, onOpenQueue = { showingQueue = true })
                1 -> LibraryScreen(theme, songs, state, player, themes, importAudio)
                else -> ThemesScreen(theme, selectedId, themes)
            }
        }
    }
}

@Composable
private fun Artwork(theme: PlayerTheme, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(theme.corner.dp)
    Box(modifier.clip(shape).background(
        Brush.linearGradient(listOf(theme.topGlow, theme.background, theme.bottomGlow))
    ), contentAlignment = Alignment.Center) {
        if (theme.id == "neon") {
            Image(painterResource(R.drawable.neon_sunset), "Carátula con un atardecer neón sobre el mar",
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("≋", fontSize = 116.sp, color = Color.White.copy(alpha = .75f))
        }
    }
}

@Composable
private fun PlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState, player: PlayerViewModel,
                         importAudio: () -> Unit, onOpenQueue: () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp.dp
    val artworkSize = (width - if (theme.id == "neon") 52.dp else 80.dp)
        .coerceAtMost(if (theme.id == "neon") 490.dp else 360.dp)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("≋", fontSize = 26.sp, color = Color.White, modifier = Modifier.width(44.dp)
                .clickable(onClick = onOpenQueue)
                .semantics { contentDescription = "Abrir cola de reproducción"; role = Role.Button })
            Text("Reproduciendo", Modifier.weight(1f), color = Color.White,
                fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = importAudio, enabled = !state.isImporting,
                modifier = Modifier.width(44.dp), contentPadding = PaddingValues(0.dp)) {
                Text(if (state.isImporting) "…" else "+", fontSize = 30.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(28.dp))
        val artworkShape = RoundedCornerShape(theme.corner.dp)
        LocalArtwork(
            file = song?.let(player.library::artworkFileFor),
            contentDescription = song?.let { "Portada de ${it.title}" } ?: "Portada de SONIQ",
            modifier = Modifier.size(artworkSize)
                .shadow(28.dp, artworkShape)
                .background(Brush.linearGradient(listOf(theme.topGlow, theme.background, theme.bottomGlow)), artworkShape),
            shape = artworkShape
        ) { Artwork(theme, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Sin canción", color = Color.White, fontSize = 29.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                song?.let { currentSong ->
                    val metadata = listOfNotNull(currentSong.artist ?: currentSong.albumArtist, currentSong.album)
                        .joinToString(" · ")
                    if (metadata.isNotEmpty()) Text(metadata, color = Color.White.copy(alpha = .82f),
                        fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(when {
                    song == null -> "Importa una canción para empezar"
                    state.isPreparing -> "Preparando reproducción…"
                    !state.selectedFileAvailable -> "Archivo no disponible"
                    else -> "Archivo local"
                },
                    color = Color.White.copy(alpha = .68f), fontSize = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = player::toggleFavorite, enabled = song != null,
                modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = .09f), RoundedCornerShape(17.dp)),
                contentPadding = PaddingValues(0.dp)) {
                Text(if (song?.favorite == true) "♥" else "♡", color = theme.accent, fontSize = 30.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Slider(value = state.currentMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { player.seek(it.toInt()) }, valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.canSeek && state.durationMs > 0 && state.selectedFileAvailable && !state.isPreparing,
            colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(state.currentMs), color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(state.durationMs), color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        val canTransport = song != null && state.selectedFileAvailable && !state.isPreparing
        val canTogglePlayback = state.isPlaying || (song != null && state.selectedFileAvailable && !state.isPreparing)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Control("⏮", "Canción anterior", song != null && state.canSkipPrevious, Color.White,
                Modifier.weight(1f).aspectRatio(1f), player::previous)
            Control("↶15", "Retroceder 15 segundos", canTransport, Color.White,
                Modifier.weight(1f).aspectRatio(1f)) { player.skip(-15_000) }
            Box(Modifier.weight(1.4f).aspectRatio(1f).padding(5.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(theme.accent, theme.secondaryAccent)))
                .clickable(enabled = canTogglePlayback, onClick = player::togglePlayback)
                .semantics { contentDescription = if (state.isPlaying) "Pausar" else "Reproducir"; role = Role.Button },
                contentAlignment = Alignment.Center) {
                Text(if (state.isPreparing) "…" else if (state.isPlaying) "Ⅱ" else "▶", color = Color.White, fontSize = 32.sp)
            }
            Control("15↷", "Avanzar 15 segundos", canTransport, Color.White,
                Modifier.weight(1f).aspectRatio(1f)) { player.skip(15_000) }
            Control("⏭", "Siguiente canción", song != null && state.canSkipNext, Color.White,
                Modifier.weight(1f).aspectRatio(1f), player::next)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            val repeatLabel = when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> "↻"
                Player.REPEAT_MODE_ONE -> "↻¹"
                else -> "↻"
            }
            val repeatDescription = when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> "Repetir toda la cola. Cambiar modo de repetición"
                Player.REPEAT_MODE_ONE -> "Repetir canción. Cambiar modo de repetición"
                else -> "Repetición desactivada. Cambiar modo de repetición"
            }
            Control(repeatLabel, repeatDescription, song != null,
                if (state.repeatMode != Player.REPEAT_MODE_OFF) theme.accent else Color.White,
                Modifier.size(48.dp), player::toggleRepeat)
        }
        Spacer(Modifier.height(34.dp))
        Row(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = .09f)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(22.dp))
            .clickable(enabled = !state.isImporting, onClick = importAudio)
            .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("↓", color = Color.White, fontSize = 25.sp)
            Spacer(Modifier.width(14.dp))
            Text(if (state.isImporting) "Importando canciones…" else "Importar canción",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("›", color = Color.White, fontSize = 26.sp)
        }
    }
}

@Composable
private fun Control(
    label: String,
    description: String,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier.size(55.dp),
    onClick: () -> Unit
) {
    Box(modifier.clickable(enabled = enabled, onClick = onClick)
        .semantics { contentDescription = description; role = Role.Button }, contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) color else color.copy(alpha = .4f), fontSize = 21.sp, maxLines = 1)
    }
}

private fun formatTime(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
