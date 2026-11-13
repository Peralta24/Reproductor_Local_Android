package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song
import android.text.format.Formatter

@Composable
fun SongsView(songs: List<Song>, state: PlayerState, player: PlayerViewModel, theme: PlayerTheme) {
    var query by remember { mutableStateOf("") }
    val visible = songs.filter { it.title.contains(query, true) || it.artist?.contains(query, true) == true ||
        it.album?.contains(query, true) == true }
    Column {
        if (theme.id == "soniq") SoniqStatsCard(songs, player, theme)
        else Text("Tus canciones", color = theme.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))
        OutlinedTextField(query, { query = it }, label = { Text(if (theme.id == "soniq") "Buscar en tu música" else "Buscar canciones") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(if (theme.id == "soniq") 14.dp else 10.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.foreground,
                unfocusedTextColor = theme.foreground, focusedBorderColor = theme.accent,
                focusedLabelColor = theme.accent, unfocusedLabelColor = theme.muted,
                focusedContainerColor = if (theme.id == "soniq") Color(0xFFF5F8FC) else Color.Transparent,
                unfocusedContainerColor = if (theme.id == "soniq") Color(0xFFF5F8FC) else Color.Transparent))
        Spacer(Modifier.height(if (theme.id == "soniq") 20.dp else 12.dp))
        if (theme.id == "soniq") Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Colección completa", color = theme.foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${visible.size} canciones", color = theme.accent, fontSize = 12.sp)
        }
        SongCollection(visible, state, player, theme,
            emptyTitle = if (query.isEmpty()) "No hay canciones" else "No se encontraron canciones")
    }
}

@Composable
private fun SoniqStatsCard(songs: List<Song>, player: PlayerViewModel, theme: PlayerTheme) {
    val context = LocalContext.current
    val bytes = remember(songs) { songs.sumOf { player.library.fileFor(it).length() } }
    val size = Formatter.formatShortFileSize(context, bytes)
    val known = songs.all { it.durationMs != null }
    val totalMinutes = songs.sumOf { it.durationMs ?: 0L } / 60000
    val duration = if (!known) "—" else if (totalMinutes >= 60) "${totalMinutes / 60} h ${totalMinutes % 60} min"
        else "$totalMinutes min"
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp)
        .background(Color(0xFF1A293D), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TU MÚSICA, CONTIGO", color = Color(0xFF97A9C2), fontSize = 10.sp, letterSpacing = 2.sp)
            Text("● SIN CONEXIÓN", color = Color(0xFF5AC7F7), fontSize = 10.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Text("${songs.size}", color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.SemiBold)
                Text(" canciones", color = Color(0xFFA6B7CD), fontSize = 14.sp, modifier = Modifier.padding(top = 18.dp))
            }
            Text("$size · $duration", color = Color(0xFFA6B7CD), fontSize = 10.sp,
                modifier = Modifier.padding(top = 20.dp))
        }
    }
}
