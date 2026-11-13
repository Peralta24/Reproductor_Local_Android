package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song

@Composable
fun MiniPlayer(song: Song?, state: PlayerState, theme: PlayerTheme, player: PlayerViewModel, onOpen: () -> Unit) {
    when (theme.variantFor(LibraryComponent.MINI_PLAYER)) {
        "soniq" -> SoniqMiniPlayer(song, state, theme, player, onOpen)
        "aurora" -> Box(Modifier.clickable(onClick = onOpen)) {
            AuroraMiniPlayer(song, state, theme, player::togglePlayback)
        }
        else -> Box(Modifier.clickable(onClick = onOpen)) {
            NeonMiniPlayer(song, state, theme, player::togglePlayback)
        }
    }
}
