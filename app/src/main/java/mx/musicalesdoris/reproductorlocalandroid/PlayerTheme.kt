package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerTheme(
    val id: String, val name: String, val description: String,
    val background: Color, val topGlow: Color, val bottomGlow: Color,
    val accent: Color, val secondaryAccent: Color, val corner: Int
) {
    companion object {
        val all = listOf(
            PlayerTheme("neon", "Neón", "Rosa y violeta sobre un atardecer",
                Color(0xFF090E26), Color(0xFF8C33E8), Color(0xFFFA45AD),
                Color(0xFFFA45AD), Color(0xFF8C33E8), 28),
            PlayerTheme("aurora", "Aurora", "Verde y azul con estilo sereno",
                Color(0xFF061C26), Color(0xFF24B89E), Color(0xFF2E66C7),
                Color(0xFF4DE0BF), Color(0xFF458FEB), 36)
        )
    }
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("themes", 0)
    private val _selectedId = MutableStateFlow(preferences.getString("selectedPlayerTheme", "neon") ?: "neon")
    val selectedId = _selectedId.asStateFlow()
    fun select(id: String) {
        if (PlayerTheme.all.none { it.id == id }) return
        _selectedId.value = id
        preferences.edit().putString("selectedPlayerTheme", id).apply()
    }
}
