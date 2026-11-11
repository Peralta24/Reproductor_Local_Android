package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.runtime.Composable

@Composable
fun LibrarySectionNavigation(section: LibrarySection, variantId: String, theme: PlayerTheme,
                             onSelect: (LibrarySection) -> Unit) {
    when (variantId) {
        "aurora" -> AuroraSectionNavigation(section, theme, onSelect)
        "segmented" -> SegmentedSectionNavigation(section, theme, onSelect)
        else -> NeonSectionNavigation(section, theme, onSelect)
    }
}
