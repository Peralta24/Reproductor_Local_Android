package mx.musicalesdoris.reproductorlocalandroid.library.components.playlist

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.LocalArtwork
import java.io.File

@Composable
fun NeonPlaylistRow(playlist: PlaylistSummary, theme: PlayerTheme, artwork: File? = null) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        LocalArtwork(artwork, "Portada de ${playlist.title}", Modifier.size(54.dp), RoundedCornerShape(9.dp)) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .09f)), contentAlignment = Alignment.Center) {
                Text("♫", color = theme.accent, fontSize = 25.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(playlist.title, color = Color.White, fontWeight = FontWeight.SemiBold)
            playlist.description?.let { Text(it, color = Color.White.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1) }
            Text("${playlist.songCount} canciones", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
        }
    }
}
