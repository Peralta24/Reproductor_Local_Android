package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun ThemesScreen(theme: PlayerTheme, selectedId: String, themes: ThemeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Elige cómo se ve tu música.", color = if (theme.id == "soniq") SoniqPalette.secondaryText else theme.muted,
            fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        PlayerTheme.all.forEach { option ->
            val soniq = theme.id == "soniq"
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .background(if (soniq) SoniqPalette.paper else theme.surface, RoundedCornerShape(13.dp))
                .border(1.dp, if (selectedId == option.id && soniq) SoniqPalette.electricBlue
                    else if (selectedId == option.id) option.accent else SoniqPalette.border, RoundedCornerShape(13.dp))
                .clickable { themes.select(option.id) }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (soniq) "Y2K · Azul hielo" else option.name, color = if (soniq) SoniqPalette.ink else theme.foreground,
                        fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(if (selectedId == option.id) "ACTUAL" else "DISPONIBLE",
                        color = if (selectedId == option.id && soniq) androidx.compose.ui.graphics.Color.White else theme.accent,
                        fontFamily = if (soniq) SoniqMonoFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 8.sp, modifier = Modifier.background(if (selectedId == option.id && soniq) SoniqPalette.electricBlue
                            else SoniqPalette.pageTop, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth().height(66.dp).background(
                    Brush.horizontalGradient(listOf(option.topGlow, option.background, option.bottomGlow)), RoundedCornerShape(11.dp)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (soniq) {
                        Text("Ⅱ", color = SoniqPalette.controlInk, fontSize = 17.sp,
                            modifier = Modifier.background(SoniqPalette.metalDark, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f).padding(vertical = 8.dp).background(SoniqPalette.lcdTop, RoundedCornerShape(5.dp)).padding(8.dp)) {
                            Text("DIGITAL MP3 PLAYER", color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont, fontSize = 5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("♫  Tu biblioteca local", color = SoniqPalette.lcdActiveText, fontFamily = SoniqMonoFont, fontSize = 8.sp)
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.fillMaxWidth(.7f).height(2.dp).background(SoniqPalette.cyan))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("▷", color = SoniqPalette.controlInk, fontSize = 16.sp,
                            modifier = Modifier.background(SoniqPalette.metalDark, CircleShape).padding(horizontal = 8.dp, vertical = 5.dp))
                    } else {
                        Box(Modifier.fillMaxWidth(.82f).height(40.dp).background(theme.surface, RoundedCornerShape(8.dp)))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (soniq) "Incluido" else option.description, color = theme.muted,
                        fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(if (selectedId == option.id) "En uso" else "Aplicar", color = if (soniq) SoniqPalette.ink else theme.accent,
                        fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        modifier = Modifier.background(if (selectedId == option.id && soniq) SoniqPalette.metalLight else option.accent.copy(alpha = .15f),
                            RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
    }
}
