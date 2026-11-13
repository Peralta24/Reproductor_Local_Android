package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemesScreen(theme: PlayerTheme, selectedId: String, themes: ThemeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Elige un tema", color = theme.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        PlayerTheme.all.forEach { option ->
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp)
                .background(theme.surface, RoundedCornerShape(22.dp))
                .border(1.dp, if (selectedId == option.id) option.accent else theme.muted.copy(alpha = .3f), RoundedCornerShape(22.dp))
                .clickable { themes.select(option.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).background(
                    Brush.linearGradient(listOf(option.topGlow, option.background, option.bottomGlow)), RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(option.name, color = theme.foreground, fontWeight = FontWeight.SemiBold)
                    Text(option.description, color = theme.muted, fontSize = 14.sp)
                }
                if (selectedId == option.id) Text("✓", color = option.accent, fontSize = 24.sp)
            }
        }
    }
}
