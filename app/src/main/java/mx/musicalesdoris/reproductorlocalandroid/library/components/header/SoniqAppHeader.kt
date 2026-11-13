package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme

@Composable
fun SoniqAppHeader(theme: PlayerTheme, onImport: () -> Unit, onProfile: () -> Unit, onEqualizer: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(theme.topGlow, Color(0xFFDDEEFF))))
        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(theme.accent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) { Text("≋", color = Color.White, fontSize = 28.sp) }
        Spacer(Modifier.width(10.dp))
        Text("SONIQ", color = theme.foreground, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
        Spacer(Modifier.width(14.dp))
        Text("AUDIO\nLOCAL", color = theme.muted, fontSize = 9.sp, lineHeight = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.weight(1f))
        HeaderMenu(theme, true, onImport, onProfile = onProfile, onEqualizer = onEqualizer)
    }
}
