package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige cómo se ve tu música.", color = if (theme.id == "soniq") SoniqPalette.secondaryText else theme.muted,
            fontFamily = if (theme.id == "soniq") SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        PlayerTheme.all.forEach { option ->
            val soniq = theme.id == "soniq"
            val cardShape = RoundedCornerShape(12.dp)
            Column(Modifier.fillMaxWidth().shadow(if (selectedId == option.id) 2.dp else 1.dp, cardShape)
                .clip(cardShape).background(if (soniq) SoniqPalette.paper else theme.surface, cardShape)
                .border(1.dp, if (selectedId == option.id && soniq) SoniqPalette.electricBlue
                    else if (selectedId == option.id) option.accent else SoniqPalette.border, cardShape)
                .clickable { themes.select(option.id) }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (soniq) "Y2K · Azul hielo" else option.name, color = if (soniq) SoniqPalette.ink else theme.foreground,
                        fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                    androidx.compose.foundation.layout.Box(Modifier.widthIn(min = 62.dp).heightIn(min = 22.dp)
                        .background(if (selectedId == option.id && soniq) SoniqPalette.electricBlue
                            else SoniqPalette.pageTop, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
                        Text(if (selectedId == option.id) "ACTUAL" else "DISPONIBLE",
                            color = if (selectedId == option.id && soniq) androidx.compose.ui.graphics.Color.White else theme.accent,
                            fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(9.dp))
                val previewShape = RoundedCornerShape(11.dp)
                Row(Modifier.fillMaxWidth().height(70.dp).clip(previewShape).background(
                    Brush.horizontalGradient(listOf(option.topGlow, option.background, option.bottomGlow)), previewShape)
                    .border(1.dp, if (soniq) SoniqPalette.border.copy(alpha = .5f)
                        else option.accent.copy(alpha = .3f), previewShape),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (soniq) {
                        Text("Ⅱ", color = SoniqPalette.controlInk, fontSize = 17.sp,
                            modifier = Modifier.background(SoniqPalette.metalDark, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp))
                            .background(SoniqPalette.lcdTop, RoundedCornerShape(5.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("DIGITAL MP3 PLAYER", color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont,
                                fontSize = 5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Text("♫  Tu biblioteca local", color = SoniqPalette.lcdActiveText, fontFamily = SoniqMonoFont,
                                fontSize = 8.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
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
                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp))
                    androidx.compose.foundation.layout.Box(Modifier.widthIn(min = 100.dp).heightIn(min = 34.dp)
                        .background(if (selectedId == option.id && soniq) SoniqPalette.metalLight
                            else option.accent.copy(alpha = .15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (selectedId == option.id) "En uso" else "Aplicar",
                            color = if (soniq) SoniqPalette.ink else theme.accent,
                            fontFamily = if (soniq) SoniqBodyFont else androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1,
                            softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
