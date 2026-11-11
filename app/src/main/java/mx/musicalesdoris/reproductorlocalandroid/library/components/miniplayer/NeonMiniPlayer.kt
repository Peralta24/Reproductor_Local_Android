package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NeonMiniPlayer(song: Song?, state: PlayerState, theme: PlayerTheme, onTogglePlayback: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .background(Color.White.copy(alpha = .09f), RoundedCornerShape(18.dp))
        .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
        .padding(12.dp)) {
        MiniPlayerContent(song, state, theme, onTogglePlayback)
    }
}
