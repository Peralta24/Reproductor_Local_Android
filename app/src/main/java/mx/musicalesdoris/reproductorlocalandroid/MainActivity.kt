package mx.musicalesdoris.reproductorlocalandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val selectedId by themes.selectedId.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val selected = songs.firstOrNull { it.id == state.selectedId }
    val error = state.error ?: libraryError

    if (error != null) AlertDialog(
        onDismissRequest = player::clearError,
        title = { Text("Error") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = player::clearError) { Text("Aceptar") } }
    )

    Scaffold(
        containerColor = theme.background,
        bottomBar = {
            NavigationBar(containerColor = theme.background) {
                listOf("Reproductor" to "▶", "Biblioteca" to "♫", "Temas" to "◈").forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index, onClick = { tab = index },
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
            when (tab) {
                0 -> PlayerScreen(theme, selected, state, player, importAudio)
                1 -> LibraryScreen(theme, songs, state.selectedId, player, importAudio)
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
private fun PlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState, player: PlayerViewModel, importAudio: () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp.dp
    val artworkSize = (width - if (theme.id == "neon") 52.dp else 80.dp)
        .coerceAtMost(if (theme.id == "neon") 490.dp else 360.dp)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("≋", fontSize = 26.sp, color = Color.White, modifier = Modifier.width(44.dp))
            Text("Reproduciendo", Modifier.weight(1f), color = Color.White,
                fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = importAudio, modifier = Modifier.width(44.dp), contentPadding = PaddingValues(0.dp)) {
                Text("+", fontSize = 30.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(28.dp))
        Artwork(theme, Modifier.size(artworkSize).shadow(28.dp, RoundedCornerShape(theme.corner.dp)))
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Sin canción", color = Color.White, fontSize = 29.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(if (song == null) "Importa una canción para empezar" else "Archivo local",
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
            enabled = state.durationMs > 0, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(state.currentMs), color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(state.durationMs), color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Control("↶15", "Retroceder 15 segundos", song != null, Color.White) { player.skip(-15_000) }
            Box(Modifier.size(82.dp).clip(CircleShape).background(Brush.linearGradient(listOf(theme.accent, theme.secondaryAccent)))
                .clickable(enabled = song != null, onClick = player::togglePlayback), contentAlignment = Alignment.Center) {
                Text(if (state.isPlaying) "Ⅱ" else "▶", color = Color.White, fontSize = 32.sp)
            }
            Control("15↷", "Adelantar 15 segundos", song != null, Color.White) { player.skip(15_000) }
            Control(if (state.repeat) "↻¹" else "↻", "Repetir canción", song != null,
                if (state.repeat) theme.accent else Color.White, player::toggleRepeat)
        }
        Spacer(Modifier.height(34.dp))
        Row(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = .09f)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(22.dp))
            .clickable(onClick = importAudio).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("↓", color = Color.White, fontSize = 25.sp)
            Spacer(Modifier.width(14.dp))
            Text("Importar canción", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("›", color = Color.White, fontSize = 26.sp)
        }
    }
}

@Composable
private fun Control(label: String, description: String, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Text(label, modifier = Modifier.size(55.dp).clickable(enabled = enabled, onClick = onClick)
        .wrapContentSize(), color = if (enabled) color else color.copy(alpha = .4f), fontSize = 24.sp)
}

@Composable
private fun LibraryScreen(theme: PlayerTheme, songs: List<Song>, selectedId: String?, player: PlayerViewModel, importAudio: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Biblioteca", Modifier.weight(1f), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = importAudio) { Text("+", color = theme.accent, fontSize = 30.sp) }
        }
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("♫", color = theme.accent, fontSize = 54.sp)
                    Text("Tu biblioteca está vacía", color = Color.White, fontSize = 21.sp)
                    Text("Importa archivos de audio para empezar a escuchar.", color = Color.White.copy(alpha = .65f))
                }
            }
        } else {
            OutlinedTextField(query, { query = it }, label = { Text("Buscar canciones") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = theme.accent,
                    focusedLabelColor = theme.accent, unfocusedLabelColor = Color.White.copy(alpha = .65f)))
            Spacer(Modifier.height(12.dp))
            val visible = songs.filter { it.title.contains(query, ignoreCase = true) }
            if (visible.isEmpty()) Text("No se encontraron canciones", color = Color.White.copy(alpha = .7f))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { song ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .08f))
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable { player.select(song) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (song.id == selectedId) "≋" else "♫", color = theme.accent, fontSize = 23.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (song.id == selectedId) "Seleccionada" else "Archivo local",
                                    color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = { player.library.toggleFavorite(song.id) }) {
                            Text(if (song.favorite) "♥" else "♡", color = theme.accent, fontSize = 24.sp)
                        }
                        TextButton(onClick = { player.delete(song) }) { Text("×", color = Color(0xFFFF817A), fontSize = 24.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemesScreen(theme: PlayerTheme, selectedId: String, themes: ThemeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Temas", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        PlayerTheme.all.forEach { option ->
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp).clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = .08f))
                .border(1.dp, if (selectedId == option.id) option.accent else Color.White.copy(alpha = .14f), RoundedCornerShape(22.dp))
                .clickable { themes.select(option.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(option, Modifier.size(80.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(option.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(option.description, color = Color.White.copy(alpha = .65f), fontSize = 14.sp)
                }
                if (selectedId == option.id) Text("✓", color = option.accent, fontSize = 24.sp)
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
