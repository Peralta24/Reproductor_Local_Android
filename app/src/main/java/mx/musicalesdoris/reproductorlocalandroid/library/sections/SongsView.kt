package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.R
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import android.text.format.Formatter
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SongsView(songs: List<Song>, state: PlayerState, player: PlayerViewModel, theme: PlayerTheme) {
    var query by remember { mutableStateOf("") }
    val visible = songs.filter { it.title.contains(query, true) || it.artist?.contains(query, true) == true ||
        it.album?.contains(query, true) == true }
    Column(Modifier.fillMaxSize()) {
        if (theme.id == "soniq") {
            SoniqStatsCard(songs, player, theme, Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(12.dp))
            SoniqSearchField(query, { query = it })
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Colección completa", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${NumberFormat.getIntegerInstance(Locale("es", "MX")).format(visible.size)} canciones",
                    color = SoniqPalette.electricBlue, fontFamily = SoniqBodyFont, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Text("Tus canciones", color = theme.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
            androidx.compose.material3.OutlinedTextField(query, { query = it }, label = { Text("Buscar canciones") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(12.dp))
        }
        SongCollection(visible, state, player, theme,
            emptyTitle = if (query.isEmpty()) "No hay canciones" else "No se encontraron canciones",
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SoniqSearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().height(44.dp).background(SoniqPalette.paper, shape)
        .border(1.dp, SoniqPalette.metalMid, shape).padding(horizontal = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Image(painterResource(R.drawable.soniq_search), null, Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Buscar en tu música", color = SoniqPalette.mutedInk,
                fontFamily = SoniqBodyFont, fontSize = 13.sp)
            BasicTextField(query, onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
                cursorBrush = SolidColor(SoniqPalette.electricBlue),
                textStyle = TextStyle(color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontSize = 13.sp))
        }
    }
}

@Composable
private fun SoniqStatsCard(songs: List<Song>, player: PlayerViewModel, theme: PlayerTheme, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bytes = remember(songs) { songs.sumOf { player.library.fileFor(it).length() } }
    val size = Formatter.formatShortFileSize(context, bytes)
    val known = songs.all { it.durationMs != null }
    val totalMinutes = songs.sumOf { it.durationMs ?: 0L } / 60000
    val duration = if (!known) "—" else if (totalMinutes >= 60) "${totalMinutes / 60} h ${totalMinutes % 60} min"
        else "$totalMinutes min"
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().height(96.dp).background(SoniqPalette.librarySurface, shape)
        .border(2.dp, SoniqPalette.libraryBorder, shape).padding(horizontal = 15.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 13.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("TU MÚSICA, CONTIGO", color = Color(0xFFA2B4C5), fontFamily = SoniqMonoFont,
                fontSize = 10.sp, letterSpacing = 1.3.sp)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(SoniqPalette.cyan, RoundedCornerShape(50)))
                Text("SIN CONEXIÓN", color = SoniqPalette.cyan, fontFamily = SoniqMonoFont,
                    fontSize = 9.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(NumberFormat.getIntegerInstance(Locale("es", "MX")).format(songs.size),
                    color = Color(0xFFEDF4FB), fontFamily = SoniqHeadingFont, fontSize = 34.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp, lineHeight = 38.sp)
                Text(" canciones", color = Color(0xFFA2B4C5), fontFamily = SoniqBodyFont,
                    fontSize = 12.sp, modifier = Modifier.padding(start = 7.dp, bottom = 4.dp))
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("$size · $duration", color = Color(0xFFA2B4C5), fontFamily = SoniqMonoFont,
                    fontSize = 8.sp, letterSpacing = .3.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(14) { index ->
                        Box(Modifier.size(5.dp).background(
                            if (songs.isNotEmpty() && index < 10) SoniqPalette.cyan else Color(0xFF697C90),
                            RoundedCornerShape(0.5.dp)))
                    }
                }
            }
        }
    }
}
