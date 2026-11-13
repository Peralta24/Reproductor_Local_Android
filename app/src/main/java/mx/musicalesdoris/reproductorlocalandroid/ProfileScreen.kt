package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

/** Visual local profile summary. Account-only actions shown in Figma are not fabricated. */
@Composable
fun ProfileScreen(songs: List<Song>, playlistCount: Int, onThemes: () -> Unit) {
    val totalMinutes = songs.sumOf { it.durationMs ?: 0L } / 60_000
    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle, SoniqPalette.pageBottom)))
        .padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mi perfil", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.layout.Box(Modifier.size(92.dp).background(SoniqPalette.paper, CircleShape)
            .border(1.dp, SoniqPalette.border, CircleShape), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(34.dp)) {
                drawCircle(SoniqPalette.electricBlue, radius = size.minDimension * .19f,
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * .28f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                drawArc(SoniqPalette.electricBlue, 205f, 130f, false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .53f),
                    size = androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Mi música", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Perfil local · En tu dispositivo", color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileMetric(songs.size.toString(), "canciones", Modifier.weight(1f))
            ProfileMetric(playlistCount.toString(), "listas", Modifier.weight(1f))
            ProfileMetric("${totalMinutes / 60} h", "de música", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        Text("Tu espacio", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().background(SoniqPalette.paper, RoundedCornerShape(12.dp))
            .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp))) {
            ProfileOption("◉", "Temas", "Y2K · Azul hielo", onThemes)
            ProfileOption("≋", "Biblioteca local", "${songs.size} canciones en este dispositivo", null)
            ProfileOption("▱", "Carpetas de música", "Ubicaciones de la biblioteca", null)
        }
        Spacer(Modifier.height(14.dp))
        ProfileOption("ⓘ", "Acerca de SONIQ", "Versión 1.0", null, framed = false)
        Spacer(Modifier.weight(1f))
        Text("Tu música permanece en este dispositivo.", color = SoniqPalette.secondaryText,
            fontFamily = SoniqMonoFont, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(SoniqPalette.paper, RoundedCornerShape(12.dp))
        .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = SoniqPalette.ink, fontFamily = SoniqHeadingFont, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 10.sp)
    }
}

@Composable
private fun ProfileOption(icon: String, title: String, subtitle: String, action: (() -> Unit)?, framed: Boolean = true) {
    Row(Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
        .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = SoniqPalette.electricBlue, fontSize = 19.sp, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 10.sp)
        }
        if (action != null) Text("›", color = SoniqPalette.electricBlue, fontSize = 21.sp)
    }
}
