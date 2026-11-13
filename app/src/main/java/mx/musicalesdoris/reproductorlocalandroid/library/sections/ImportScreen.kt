package mx.musicalesdoris.reproductorlocalandroid.library.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun ImportScreen(onSelectAudio: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onClose)
    Column(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        SoniqPalette.pageTop, SoniqPalette.pageMiddle, SoniqPalette.pageBottom
    ))).padding(horizontal = 20.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = SoniqPalette.ink, fontSize = 25.sp,
                modifier = Modifier.clickable(onClick = onClose).padding(horizontal = 8.dp))
            Text("Importar", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
                fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.background(Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)),
            RoundedCornerShape(14.dp)).border(1.dp, SoniqPalette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("↓", color = SoniqPalette.electricBlue, fontSize = 33.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Importa tu música", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Elige archivos de audio para añadirlos a tu biblioteca local.",
            color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont,
            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.height(20.dp))
        ImportAction("Seleccionar archivos", enabled = true, onClick = onSelectAudio, primary = true)
        Spacer(Modifier.height(10.dp))
        ImportAction("Elegir carpeta · No disponible", enabled = false, onClick = {}, primary = false)
        Spacer(Modifier.height(10.dp))
        ImportAction("Importar por cable · No disponible", enabled = false, onClick = {}, primary = false)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().background(SoniqPalette.paper, RoundedCornerShape(12.dp))
            .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp)).padding(14.dp)) {
            Text("Importación local", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Los archivos seleccionados permanecen en tu dispositivo. La importación por carpeta y cable aún no está implementada.",
                color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ImportAction(label: String, enabled: Boolean, onClick: () -> Unit, primary: Boolean) {
    val shape = RoundedCornerShape(11.dp)
    Box(Modifier.fillMaxWidth().height(48.dp).background(
        if (primary) Brush.verticalGradient(listOf(SoniqPalette.electricBlue, Color(0xFF397CF0)))
        else Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)), shape)
        .border(1.dp, if (primary) SoniqPalette.electricBlue else SoniqPalette.border, shape)
        .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Text(label, color = if (primary) Color.White else SoniqPalette.mutedInk.copy(alpha = .6f),
            fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
