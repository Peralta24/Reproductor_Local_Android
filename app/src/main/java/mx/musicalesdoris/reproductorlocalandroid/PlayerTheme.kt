package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerTheme(
    val id: String, val name: String, val description: String,
    val background: Color, val topGlow: Color, val bottomGlow: Color,
    val accent: Color, val secondaryAccent: Color, val corner: Int,
    val componentDefaults: Map<LibraryComponent, String>
) {
    companion object {
        val all = listOf(
            PlayerTheme("neon", "Neón", "Rosa y violeta sobre un atardecer",
                Color(0xFF090E26), Color(0xFF8C33E8), Color(0xFFFA45AD),
                Color(0xFFFA45AD), Color(0xFF8C33E8), 28,
                LibraryComponent.entries.associateWith { "neon" }),
            PlayerTheme("aurora", "Aurora", "Verde y azul con estilo sereno",
                Color(0xFF061C26), Color(0xFF24B89E), Color(0xFF2E66C7),
                Color(0xFF4DE0BF), Color(0xFF458FEB), 36,
                LibraryComponent.entries.associateWith { "aurora" })
        )
    }
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("themes", 0)
    private val _selectedId = MutableStateFlow(preferences.getString("selectedPlayerTheme", "neon") ?: "neon")
    val selectedId = _selectedId.asStateFlow()
    private val _componentOverrides = MutableStateFlow(
        LibraryComponent.entries.mapNotNull { component ->
            preferences.getString("libraryComponentVariant.${component.key}", null)
                ?.takeIf { value -> component.availableVariants.any { it.id == value } }
                ?.let { component to it }
        }.toMap()
    )
    val componentOverrides = _componentOverrides.asStateFlow()

    fun variantFor(component: LibraryComponent, theme: PlayerTheme): String =
        _componentOverrides.value[component]
            ?: theme.componentDefaults[component]
                ?.takeIf { value -> component.availableVariants.any { it.id == value } }
            ?: component.availableVariants.first().id

    fun setVariant(component: LibraryComponent, variantId: String?) {
        if (variantId != null && component.availableVariants.none { it.id == variantId }) return
        _componentOverrides.value = _componentOverrides.value.toMutableMap().apply {
            if (variantId == null) remove(component) else put(component, variantId)
        }
        preferences.edit().apply {
            val key = "libraryComponentVariant.${component.key}"
            if (variantId == null) remove(key) else putString(key, variantId)
        }.apply()
    }
    fun select(id: String) {
        if (PlayerTheme.all.none { it.id == id }) return
        _selectedId.value = id
        preferences.edit().putString("selectedPlayerTheme", id).apply()
    }
}
