package mx.musicalesdoris.reproductorlocalandroid.library.components.genre

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.GenreSummary

@Composable
fun GenreRow(genre: GenreSummary, theme: PlayerTheme, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).height(80.dp).padding(start = 4.dp),
        verticalArrangement = Arrangement.Center) {
        Text(genre.name, color = theme.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("${genre.songCount} canciones", color = theme.muted, fontSize = 12.sp)
    }
}
