package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun MiniPlayer(song: Song?, state: PlayerState, variantId: String, theme: PlayerTheme,
               onTogglePlayback: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraMiniPlayer(song, state, theme, onTogglePlayback)
        else -> NeonMiniPlayer(song, state, theme, onTogglePlayback)
    }
}
