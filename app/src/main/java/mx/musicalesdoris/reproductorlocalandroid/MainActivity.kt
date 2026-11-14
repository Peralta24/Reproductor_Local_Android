package mx.musicalesdoris.reproductorlocalandroid

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.library.LibraryScreen
import mx.musicalesdoris.reproductorlocalandroid.library.LibrarySection
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer
import mx.musicalesdoris.reproductorlocalandroid.player.ThemedPlayerScreen
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.ReproductorLocalAndroidTheme
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels()
    private var requestedScreen by mutableStateOf("library")
    private var screenRequestId by mutableStateOf(0)
    private val themes: ThemeViewModel by viewModels()
    private val importer = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) player.importSongs(uris)
    }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanDeviceMusic()
    }
    private fun requiredAudioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    private fun scanDeviceMusic() {
        val library = player.library
        lifecycleScope.launch(Dispatchers.IO) { library.scanDeviceMusic() }
    }
    private var coverPlaylistId: String? = null
    private val coverImporter = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = coverPlaylistId
        if (uri != null && id != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            lifecycleScope.launch(Dispatchers.IO) { player.library.setPlaylistCover(id, uri) }
        }
        coverPlaylistId = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        handleAction(intent)
        if (ContextCompat.checkSelfPermission(this, requiredAudioPermission()) != PackageManager.PERMISSION_GRANTED)
            audioPermission.launch(requiredAudioPermission())
        setContent {
            val selectedId by themes.selectedId.collectAsState()
            val theme = PlayerTheme.all.firstOrNull { it.id == selectedId } ?: PlayerTheme.all.first()
            SideEffect {
                val insets = WindowCompat.getInsetsController(window, window.decorView)
                insets.isAppearanceLightStatusBars = theme.id == "soniq"
                insets.isAppearanceLightNavigationBars = theme.id == "soniq"
            }
            ReproductorLocalAndroidTheme(
                darkTheme = theme.id != "soniq",
                dynamicColor = false,
                soniq = theme.id == "soniq"
            ) {
                AppScreen(player, themes, theme, requestedScreen, screenRequestId, { importer.launch(arrayOf("audio/*")) }) { id ->
                    coverPlaylistId = id
                    coverImporter.launch(arrayOf("image/*"))
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED)
            scanDeviceMusic()
        else player.library.markDeviceMediaUnavailable()
    }
    override fun onStop() {
        player.persistCurrentPlaybackState()
        super.onStop()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }
    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            "mx.musicalesdoris.OPEN_PLAYER" -> {
                requestedScreen = "player"
                screenRequestId++
            }
            "mx.musicalesdoris.PREVIOUS" -> player.previousSong()
            "mx.musicalesdoris.TOGGLE" -> player.togglePlayback()
            "mx.musicalesdoris.NEXT" -> player.nextSong()
        }
    }
}
@Composable
private fun AppScreen(player: PlayerViewModel, themes: ThemeViewModel, theme: PlayerTheme,
                      requestedScreen: String, screenRequestId: Int,
                      importAudio: () -> Unit, chooseCover: (String) -> Unit) {
    val debugLoggingEnabled = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val songs by player.library.songs.collectAsState()
    val state by player.state.collectAsState()
    val libraryError by player.library.error.collectAsState()
    val pendingRelinks by player.library.pendingRelinks.collectAsState()
    val selectedId by themes.selectedId.collectAsState()
    var screen by remember { mutableStateOf(requestedScreen) }
    val screenHistory = remember { mutableStateListOf<String>() }
    val librarySection = remember { mutableStateOf(LibrarySection.SONGS) }
    val libraryDetails = remember { mutableStateListOf<Pair<String, String>>() }
    val navigateTo: (String) -> Unit = { destination ->
        if (destination != screen) {
            screenHistory.add(screen)
            screen = destination
        }
    }
    val navigateBack: () -> Unit = {
        val previousScreen = screen
        val destination = if (screenHistory.isNotEmpty()) screenHistory.removeAt(screenHistory.lastIndex) else "library"
        screen = destination
        if (debugLoggingEnabled) Log.d("SONIQ_SWIPE_DEBUG", "navigation_updated from=$previousScreen to=$destination")
    }
    LaunchedEffect(requestedScreen, screenRequestId) {
        if (requestedScreen != screen) navigateTo(requestedScreen)
    }
    val error = state.error ?: libraryError
    val selectedSong = songs.firstOrNull { it.id == state.selectedId }
    val contentScreen = if (screen == "player") screenHistory.lastOrNull { it != "player" } ?: "library" else screen
    BackHandler(enabled = screen != "library" && error == null) { navigateBack() }
    if (error != null) AlertDialog(onDismissRequest = player::clearError,
        title = { Text("Error") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = player::clearError) { Text("Aceptar") } })
    else pendingRelinks.firstOrNull()?.let { pending ->
        val textColor = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground
        val mutedColor = if (theme.id == "soniq") SoniqPalette.secondaryText else theme.muted
        AlertDialog(
            onDismissRequest = {
                player.library.resolvePendingRelink(pending.librarySong.id, pending.importedSong.id, relink = false)
            },
            title = { Text("Coincidencia de biblioteca", color = textColor) },
            text = { Text("El audio importado coincide exactamente con ${pending.librarySong.title}. ¿Quieres volver a vincularlo para recuperar sus favoritos y playlists?",
                color = mutedColor) },
            confirmButton = {
                TextButton(onClick = {
                    player.library.resolvePendingRelink(pending.librarySong.id, pending.importedSong.id, relink = true)
                }) { Text("Vincular", color = if (theme.id == "soniq") SoniqPalette.electricBlue else theme.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    player.library.resolvePendingRelink(pending.librarySong.id, pending.importedSong.id, relink = false)
                }) { Text("Importar aparte", color = if (theme.id == "soniq") SoniqPalette.electricBlue else theme.accent) }
            },
            containerColor = if (theme.id == "soniq") Color(0xFFEAF2FB) else theme.background,
            shape = RoundedCornerShape(18.dp)
        )
    }
    Box(Modifier.fillMaxSize().background(theme.background).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxSize().then(
            if (screen == "player") Modifier.clearAndSetSemantics {} else Modifier
        )) {
            if (contentScreen == "library") {
                LibraryScreen(theme, songs, state, player, importAudio,
                    section = librarySection.value, onSectionChange = { librarySection.value = it },
                    detailStack = libraryDetails,
                    systemBackEnabled = screen == "library" && error == null,
                    onPlayer = { navigateTo("player") }, onThemes = { navigateTo("themes") },
                    onProfile = { navigateTo("profile") }, onEqualizer = { navigateTo("equalizer") }, chooseCover = chooseCover)
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f)) {
                        when (contentScreen) {
                            "themes" -> Column {
                                ScreenHeader("Temas", theme, navigateBack)
                                ThemesScreen(theme, selectedId, themes)
                            }
                            "profile" -> Column {
                                ScreenHeader("Perfil", theme, navigateBack)
                                ProfileScreen(songs, player.library.playlists.value.size) { navigateTo("themes") }
                            }
                            "equalizer" -> Column(Modifier.fillMaxSize()) {
                                ScreenHeader("Ecualizador", theme, navigateBack)
                                EqualizerScreen()
                            }
                        }
                    }
                    MiniPlayer(selectedSong, state, theme, player) { navigateTo("player") }
                }
            }
        }
        if (screen == "player") {
            ThemedPlayerScreen(theme, selectedSong, state, player,
                onClose = navigateBack, importAudio = importAudio)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, theme: PlayerTheme, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Text("‹", color = if (theme.id == "soniq") SoniqPalette.ink else theme.accent, fontSize = 25.sp)
        }
        Text(title, color = if (theme.id == "soniq") SoniqPalette.ink else theme.foreground,
            fontFamily = if (theme.id == "soniq") SoniqHeadingFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
