package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme

@Composable
fun AuroraAppHeader(theme: PlayerTheme, onImport: () -> Unit, onPlayer: () -> Unit, onThemes: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(theme.secondaryAccent, CircleShape),
            contentAlignment = Alignment.Center) { Text("≋", color = Color.White, fontSize = 25.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SONIQ", color = theme.foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("AUDIO LOCAL", color = theme.muted, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        HeaderMenu(theme, false, onImport, onPlayer, onThemes)
    }
}
