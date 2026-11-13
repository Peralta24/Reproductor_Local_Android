package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont

@Composable
fun FavoritesView(songs: List<Song>, state: PlayerState, player: PlayerViewModel, theme: PlayerTheme) {
    var newestFirst by remember { mutableStateOf(true) }
    val favorites = songs.filter { it.favorite }.sortedBy { it.favoritedAt ?: it.importedAt }
        .let { if (newestFirst) it.reversed() else it }
    Column {
        if (theme.id == "soniq") {
            Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val art = favorites.firstOrNull()?.let { player.library.artworkFor(it) }
                ArtworkView(art, theme, 172.dp, "soniq-favorites")
                Spacer(Modifier.height(17.dp))
                Text("Favoritos", color = theme.foreground, fontFamily = SoniqHeadingFont,
                    fontSize = 29.sp, fontWeight = FontWeight.Bold)
                val seconds = favorites.sumOf { it.durationMs ?: 0L } / 1000
                val duration = if (favorites.all { it.durationMs != null }) "${seconds / 60} min ${seconds % 60} s" else "—"
                Text("${favorites.size} canciones · $duration · En tu dispositivo", color = theme.muted,
                    fontFamily = SoniqMonoFont, fontSize = 9.sp)
            }
            CollectionActions(favorites.isNotEmpty(), theme,
                onPlay = { player.playCollection(favorites) },
                onShuffle = { player.playCollection(favorites, shuffled = true) })
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tus canciones", color = theme.foreground, fontFamily = SoniqHeadingFont, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { newestFirst = !newestFirst }) {
                    Text(if (newestFirst) "Añadidas ⇵" else "Añadidas ⇵", color = theme.muted,
                        fontFamily = SoniqBodyFont, fontSize = 10.sp)
                }
            }
        } else {
            Text("Tus favoritos", color = theme.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
        }
        SongCollection(favorites, state, player, theme, "Aún no tienes favoritos")
    }
}

@Composable
fun CollectionActions(enabled: Boolean, theme: PlayerTheme, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(44.dp).background(Brush.verticalGradient(listOf(theme.accent,
            Color(0xFF2877F5))), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            TextButton(onClick = onPlay, enabled = enabled) { Text("▷  Reproducir", color = Color.White,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }
        Box(Modifier.weight(1f).height(44.dp).background(
            if (theme.id == "soniq") Brush.verticalGradient(listOf(Color.White, Color(0xFFD5E2EF)))
            else Brush.verticalGradient(listOf(theme.surface, theme.surface)), RoundedCornerShape(11.dp))
            .then(if (theme.id == "soniq") Modifier else Modifier),
            contentAlignment = Alignment.Center) {
            TextButton(onClick = onShuffle, enabled = enabled) { Text("⤨  Aleatorio", color = theme.foreground,
                fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }
    }
}
