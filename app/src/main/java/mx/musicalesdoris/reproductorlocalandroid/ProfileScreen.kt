package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle, SoniqPalette.pageBottom)))
        .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(Modifier.size(96.dp).shadow(5.dp, CircleShape).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight,
                Color(0xFFBFDBF7), SoniqPalette.pageTop)))
            .border(1.dp, SoniqPalette.border, CircleShape), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Box(Modifier.size(80.dp).clip(CircleShape)
                .background(SoniqPalette.paper), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                drawCircle(SoniqPalette.electricBlue, radius = size.minDimension * .19f,
                    center = Offset(size.width / 2, size.height * .28f), style = Stroke(width = 2.dp.toPx()))
                drawArc(SoniqPalette.electricBlue, 205f, 130f, false,
                    topLeft = Offset(size.width * .16f, size.height * .53f),
                    size = androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .6f),
                    style = Stroke(width = 2.dp.toPx()))
            }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Mi perfil", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text("Perfil local · En tu dispositivo", color = SoniqPalette.secondaryText,
            fontFamily = SoniqBodyFont, fontSize = 11.sp, lineHeight = 16.sp,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileMetric(songs.size.toString(), "canciones", Modifier.weight(1f))
            ProfileMetric(playlistCount.toString(), "listas", Modifier.weight(1f))
            ProfileMetric("${totalMinutes / 60} h", "de música", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Tu espacio", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val optionShape = RoundedCornerShape(12.dp)
        Column(Modifier.fillMaxWidth().clip(optionShape).background(SoniqPalette.paper, optionShape)
            .border(1.dp, SoniqPalette.border, optionShape)) {
            ProfileOption("◉", "Temas", "Y2K · Azul hielo", onThemes)
            ProfileDivider()
            ProfileOption("≋", "Biblioteca local", "${songs.size} canciones en este dispositivo", null)
            ProfileDivider()
            ProfileOption("▱", "Carpetas de música", "Ubicaciones de la biblioteca", null)
        }
        Spacer(Modifier.height(10.dp))
        ProfileOption("ⓘ", "Acerca de SONIQ", "Versión 1.0", null, framed = false)
        Spacer(Modifier.height(16.dp))
        Text("Tu música permanece en este dispositivo.", color = SoniqPalette.secondaryText,
            fontFamily = SoniqMonoFont, fontSize = 9.sp, lineHeight = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.heightIn(min = 72.dp)
        .shadow(3.dp, shape).background(Brush.verticalGradient(listOf(Color.White,
            SoniqPalette.metalLight, Color(0xFFBFDBF7), SoniqPalette.pageTop)), shape)
        .border(1.dp, SoniqPalette.border, shape).padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = SoniqPalette.ink, fontFamily = SoniqHeadingFont, fontSize = 26.sp,
            lineHeight = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(label, color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 11.sp,
            lineHeight = 16.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileOption(icon: String, title: String, subtitle: String, action: (() -> Unit)?, framed: Boolean = true) {
    Row(Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
        .heightIn(min = 56.dp).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Text(icon, color = SoniqPalette.electricBlue, fontSize = 19.sp, textAlign = TextAlign.Center,
                maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont,
                fontSize = 11.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (action != null) androidx.compose.foundation.layout.Box(Modifier.size(24.dp),
            contentAlignment = Alignment.Center) {
            Text("›", color = SoniqPalette.electricBlue, fontSize = 21.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProfileDivider() {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        .height(1.dp).background(SoniqPalette.border.copy(alpha = .55f)))
}
