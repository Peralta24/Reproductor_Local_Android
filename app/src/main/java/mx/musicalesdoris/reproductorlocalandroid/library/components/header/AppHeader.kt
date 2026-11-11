package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun AppHeader(variantId: String, theme: PlayerTheme, onImport: () -> Unit) {
    when (variantId) {
        "aurora" -> AuroraAppHeader(theme, onImport)
        else -> NeonAppHeader(theme, onImport)
    }
}
