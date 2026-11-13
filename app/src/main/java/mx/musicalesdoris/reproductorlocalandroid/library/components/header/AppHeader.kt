package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import androidx.compose.runtime.Composable
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent

@Composable
fun AppHeader(theme: PlayerTheme, onImport: () -> Unit, onPlayer: () -> Unit,
              onThemes: () -> Unit, onProfile: () -> Unit, onEqualizer: () -> Unit) {
    when (theme.variantFor(LibraryComponent.HEADER)) {
        "aurora" -> AuroraAppHeader(theme, onImport, onPlayer, onThemes)
        "soniq" -> SoniqAppHeader(theme, onImport, onProfile, onEqualizer)
        else -> NeonAppHeader(theme, onImport, onPlayer, onThemes)
    }
}
