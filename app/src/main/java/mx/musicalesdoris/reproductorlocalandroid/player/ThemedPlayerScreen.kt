package mx.musicalesdoris.reproductorlocalandroid.player

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.Song

@Composable
fun ThemedPlayerScreen(theme: PlayerTheme, song: Song?, state: PlayerState,
                       player: PlayerViewModel, onClose: () -> Unit, importAudio: () -> Unit) {
    if (theme.variantFor(LibraryComponent.PLAYER_SCREEN) == "soniq") SoniqPlayerScreen(theme, song, state, player, onClose)
    else StandardPlayerScreen(theme, song, state, player, onClose, importAudio)
}
