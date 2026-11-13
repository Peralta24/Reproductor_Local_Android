package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun MiniPlayer(song: Song?, state: PlayerState, variantId: String, theme: PlayerTheme,
               onPrevious: () -> Unit, onTogglePlayback: () -> Unit, onNext: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraMiniPlayer(song, state, theme, onPrevious, onTogglePlayback, onNext)
        else -> NeonMiniPlayer(song, state, theme, onPrevious, onTogglePlayback, onNext)
    }
}
