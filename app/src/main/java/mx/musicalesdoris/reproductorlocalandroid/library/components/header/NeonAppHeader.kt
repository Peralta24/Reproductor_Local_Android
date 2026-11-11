package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeonAppHeader(theme: PlayerTheme, onImport: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(theme.accent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) { Text("≋", color = Color.White, fontSize = 24.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SONIQ", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("AUDIO LOCAL", color = Color.White.copy(alpha = .65f), fontSize = 10.sp, letterSpacing = 2.sp)
        }
        TextButton(onClick = onImport) { Text("+", color = Color.White, fontSize = 28.sp) }
    }
}
