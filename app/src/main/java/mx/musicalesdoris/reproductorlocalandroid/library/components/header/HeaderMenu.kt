package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.R
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.soniqClickable

@Composable
internal fun HeaderMenu(theme: PlayerTheme, soniq: Boolean, onImport: () -> Unit,
                        onPlayer: () -> Unit = {}, onThemes: () -> Unit = {},
                        onProfile: () -> Unit = {}, onEqualizer: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.size(42.dp).then(if (soniq) Modifier.clip(CircleShape) else Modifier)
            .background(if (soniq) Brush.verticalGradient(listOf(Color.White, Color(0xFFDDE1E7),
                Color(0xFFB7BFCB), Color(0xFFF3F5F8)))
                else Brush.verticalGradient(listOf(theme.surface, theme.surface)), CircleShape)
            .border(1.dp, if (soniq) Color(0xFFB7BFCB) else Color.Transparent, CircleShape)
            .then(if (soniq) Modifier.soniqClickable(CircleShape) { expanded = true }
                else Modifier.clickable { expanded = true }), contentAlignment = Alignment.Center) {
            if (soniq) Image(painterResource(R.drawable.soniq_settings_gear), null, Modifier.size(20.dp))
            else Text("☷", color = theme.foreground, fontSize = 24.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = if (soniq) Modifier.background(Brush.verticalGradient(
                listOf(Color.White, Color(0xFFD1E8FC), Color(0xFFE8F5FF)))) else Modifier) {
            if (soniq) {
                Text("CONFIGURACIÓN", color = SoniqPalette.mutedInk, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                HeaderOption("♙", "Perfil", theme) { expanded = false; onProfile() }
                HeaderOption("☷", "Ecualizador", theme) { expanded = false; onEqualizer() }
                HeaderOption("↓", "Importar", theme) { expanded = false; onImport() }
            } else {
                DropdownMenuItem(text = { Text("Reproductor") }, onClick = { expanded = false; onPlayer() })
                DropdownMenuItem(text = { Text("Temas") }, onClick = { expanded = false; onThemes() })
                DropdownMenuItem(text = { Text("Importar") }, onClick = { expanded = false; onImport() })
            }
        }
    }
}

@Composable
private fun HeaderOption(icon: String, title: String, theme: PlayerTheme, action: () -> Unit) {
    Row(Modifier.width(240.dp).clickable(onClick = action).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD5E9FA))),
            RoundedCornerShape(9.dp)).border(1.dp, Color(0xFF9AB2CC), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center) { Text(icon, color = theme.accent, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Text(title, color = theme.foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
