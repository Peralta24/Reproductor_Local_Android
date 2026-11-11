package mx.musicalesdoris.reproductorlocalandroid.library.components.album

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

@Composable
fun AuroraAlbumCard(album: AlbumSummary, theme: PlayerTheme) {
    Column(Modifier.background(Color.White.copy(alpha = .09f), RoundedCornerShape(24.dp)).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(100.dp).background(theme.background, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center) { Text("◉", color = theme.accent, fontSize = 65.sp) }
        Spacer(Modifier.height(7.dp))
        Text(album.title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(album.artist, color = Color.White.copy(alpha = .65f))
        Text("${album.songCount} canciones", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
    }
}
