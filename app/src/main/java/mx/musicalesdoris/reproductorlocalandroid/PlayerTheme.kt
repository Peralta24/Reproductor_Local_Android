package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

data class PlayerTheme(
    val id: String, val name: String, val description: String,
    val background: Color, val topGlow: Color, val bottomGlow: Color,
    val accent: Color, val secondaryAccent: Color, val corner: Int,
    val componentDefaults: Map<LibraryComponent, String>,
    val foreground: Color = Color.White, val muted: Color = Color.White.copy(alpha = .65f),
    val surface: Color = Color.White.copy(alpha = .09f)
) {
    fun variantFor(component: LibraryComponent): String = componentDefaults[component] ?: id

    companion object {
        val all = listOf(
            PlayerTheme("neon", "Neón", "Rosa y violeta sobre un atardecer",
                Color(0xFF090E26), Color(0xFF8C33E8), Color(0xFFFA45AD),
                Color(0xFFFA45AD), Color(0xFF8C33E8), 28,
                LibraryComponent.entries.associateWith { "neon" }),
            PlayerTheme("aurora", "Aurora", "Verde y azul con estilo sereno",
                Color(0xFF061C26), Color(0xFF24B89E), Color(0xFF2E66C7),
                Color(0xFF4DE0BF), Color(0xFF458FEB), 36,
                LibraryComponent.entries.associateWith { "aurora" }),
            PlayerTheme("soniq", "Soniq", "Azul claro con reproductor digital",
                SoniqPalette.blueIce, SoniqPalette.blueIceHighlight, SoniqPalette.blueIceShadow,
                SoniqPalette.electricBlue, SoniqPalette.cyan, 24,
                LibraryComponent.entries.associateWith { "soniq" },
                SoniqPalette.ink, SoniqPalette.mutedInk, Color.White.copy(alpha = .66f))
        )
    }
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("themes", 0)
    private val _selectedId = MutableStateFlow(preferences.getString("selectedPlayerTheme", "soniq") ?: "soniq")
    val selectedId = _selectedId.asStateFlow()
    init {
        preferences.edit().apply {
            preferences.all.keys.filter { it.startsWith("libraryComponentVariant.") }.forEach(::remove)
        }.apply()
    }
    fun variantFor(component: LibraryComponent, theme: PlayerTheme): String =
        theme.variantFor(component)
    fun select(id: String) {
        if (PlayerTheme.all.none { it.id == id }) return
        _selectedId.value = id
        preferences.edit().putString("selectedPlayerTheme", id).apply()
    }
}
