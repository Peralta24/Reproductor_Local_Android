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
fun AuroraMiniPlayer(song: Song?, state: PlayerState, theme: PlayerTheme,
                     onPrevious: () -> Unit, onTogglePlayback: () -> Unit, onNext: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .background(Color.White.copy(alpha = .09f), RoundedCornerShape(30.dp))
        .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(30.dp))
        .padding(16.dp)) {
        MiniPlayerContent(song, state, theme, onPrevious, onTogglePlayback, onNext)
    }
}
