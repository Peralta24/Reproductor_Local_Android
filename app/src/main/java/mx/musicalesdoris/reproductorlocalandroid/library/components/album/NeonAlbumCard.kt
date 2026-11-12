package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.io.File
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.LocalArtwork

@Composable
fun NeonAlbumCard(album: AlbumSummary, artworkFile: File?, theme: PlayerTheme, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        LocalArtwork(artworkFile, "Portada de ${album.title}",
            Modifier.fillMaxWidth().height(136.dp).background(Color.White.copy(alpha = .09f), RoundedCornerShape(10.dp)),
            RoundedCornerShape(10.dp)) { Text("◉", color = theme.accent, fontSize = 75.sp) }
        Spacer(Modifier.height(7.dp))
        Text(album.title, color = Color.White, fontWeight = FontWeight.SemiBold)
        album.artist?.let { Text(it, color = Color.White.copy(alpha = .65f)) }
        Text("${album.songCount} canciones", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
    }
}
