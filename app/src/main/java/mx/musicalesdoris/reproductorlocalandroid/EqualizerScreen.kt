package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

/** Visual layout based on ReproV1; controls stay disabled because Android has no EQ engine. */
@Composable
fun EqualizerScreen() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ecualizador", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        SettingCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Activar ecualizador", color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Procesamiento de audio no disponible", color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 10.sp)
                }
                Switch(checked = false, onCheckedChange = {}, enabled = false)
            }
        }
        SettingCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Preajuste", color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont, fontSize = 13.sp)
                Text("Plano  ⌄", color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFFE7F0FB), RoundedCornerShape(13.dp))
            .border(1.dp, SoniqPalette.border, RoundedCornerShape(13.dp))) {
            Row(Modifier.fillMaxWidth().background(SoniqPalette.lcdTop, RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("5 BAND GRAPHIC EQUALIZER", color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont, fontSize = 8.sp)
                Text("● AUDIO LOCAL", color = SoniqPalette.cyan, fontFamily = SoniqMonoFont, fontSize = 8.sp)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("60 Hz", "250 Hz", "1 kHz", "4 kHz", "16 kHz").forEach { label ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0 dB", color = SoniqPalette.secondaryText, fontFamily = SoniqMonoFont, fontSize = 8.sp)
                        androidx.compose.foundation.Canvas(Modifier.size(width = 28.dp, height = 130.dp)) {
                            val centerX = size.width / 2
                            drawLine(SoniqPalette.lcdTop, Offset(centerX, size.height * .12f),
                                Offset(centerX, size.height * .5f), strokeWidth = 5.dp.toPx())
                            drawLine(SoniqPalette.cyan, Offset(centerX, size.height * .5f),
                                Offset(centerX, size.height * .88f), strokeWidth = 5.dp.toPx())
                            drawRoundRect(Color.White, Offset(centerX - 13.dp.toPx(), size.height * .5f - 7.dp.toPx()),
                                Size(26.dp.toPx(), 14.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                            drawRoundRect(SoniqPalette.border, Offset(centerX - 13.dp.toPx(), size.height * .5f - 7.dp.toPx()),
                                Size(26.dp.toPx(), 14.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                                style = Stroke(1.dp.toPx()))
                        }
                        Text(label, color = SoniqPalette.ink, fontFamily = SoniqMonoFont, fontSize = 8.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Preamplificación", color = SoniqPalette.ink, fontFamily = SoniqBodyFont, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Slider(value = .5f, onValueChange = {}, enabled = false,
                    colors = SliderDefaults.colors(disabledActiveTrackColor = SoniqPalette.electricBlue))
            }
            Text("0 dB", color = SoniqPalette.secondaryText, fontFamily = SoniqMonoFont, fontSize = 9.sp)
        }
        TextButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)), RoundedCornerShape(11.dp))
            .border(1.dp, SoniqPalette.border, RoundedCornerShape(11.dp))) {
            Text("Restablecer", color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont)
        }
        Text("Los controles se activarán cuando el motor de audio admita filtros.", color = SoniqPalette.secondaryText,
            fontFamily = SoniqBodyFont, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF3F5F8), RoundedCornerShape(12.dp))
        .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) { content() }
}
