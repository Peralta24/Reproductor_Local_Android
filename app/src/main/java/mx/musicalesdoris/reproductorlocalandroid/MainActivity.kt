package mx.musicalesdoris.reproductorlocalandroid

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.library.LibraryScreen
import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.MiniPlayer
import mx.musicalesdoris.reproductorlocalandroid.player.ThemedPlayerScreen
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.ReproductorLocalAndroidTheme
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels()
    private var requestedScreen by mutableStateOf("library")
    private val themes: ThemeViewModel by viewModels()
    private val importer = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) player.importSongs(uris)
    }
    private var coverPlaylistId: String? = null
    private val coverImporter = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = coverPlaylistId
        if (uri != null && id != null) player.library.setPlaylistCover(id, uri)
        coverPlaylistId = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        handleAction(intent)
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
                AppScreen(player, themes, theme, requestedScreen, { importer.launch(arrayOf("audio/*")) }) { id ->
                    coverPlaylistId = id
                    coverImporter.launch(arrayOf("image/*"))
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }
    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            "mx.musicalesdoris.OPEN_PLAYER" -> requestedScreen = "player"
            "mx.musicalesdoris.PREVIOUS" -> player.previousSong()
            "mx.musicalesdoris.TOGGLE" -> player.togglePlayback()
            "mx.musicalesdoris.NEXT" -> player.nextSong()
        }
    }
}
@Composable
private fun AppScreen(player: PlayerViewModel, themes: ThemeViewModel, theme: PlayerTheme,
                      requestedScreen: String, importAudio: () -> Unit, chooseCover: (String) -> Unit) {
    val songs by player.library.songs.collectAsState()
    val state by player.state.collectAsState()
    val libraryError by player.library.error.collectAsState()
    val selectedId by themes.selectedId.collectAsState()
    var screen by remember { mutableStateOf(requestedScreen) }
    LaunchedEffect(requestedScreen) { screen = requestedScreen }
    val error = state.error ?: libraryError
    val selectedSong = songs.firstOrNull { it.id == state.selectedId }
    BackHandler(screen != "library") { screen = "library" }
    if (error != null) AlertDialog(onDismissRequest = player::clearError,
        title = { Text("Error") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = player::clearError) { Text("Aceptar") } })
    Box(Modifier.fillMaxSize().background(theme.background).statusBarsPadding().navigationBarsPadding()) {
        if (screen == "library") {
            LibraryScreen(theme, songs, state, player, importAudio,
                onPlayer = { screen = "player" }, onThemes = { screen = "themes" },
                onProfile = { screen = "profile" }, onEqualizer = { screen = "equalizer" }, chooseCover = chooseCover)
        } else if (screen == "player") {
            ThemedPlayerScreen(theme, selectedSong, state, player,
                onClose = { screen = "library" }, importAudio = importAudio)
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)) {
                    when (screen) {
                        "themes" -> Column {
                            ScreenHeader("Temas", theme) { screen = "library" }
                            ThemesScreen(theme, selectedId, themes)
                        }
                        "profile" -> Column {
                            ScreenHeader("Perfil", theme) { screen = "library" }
                            ProfileScreen(songs, player.library.playlists.value.size) { screen = "themes" }
                        }
                        "equalizer" -> Column(Modifier.fillMaxSize()) {
                            ScreenHeader("Ecualizador", theme) { screen = "library" }
                            EqualizerScreen()
                        }
                    }
                }
                MiniPlayer(selectedSong, state, theme, player) { screen = "player" }
            }
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
