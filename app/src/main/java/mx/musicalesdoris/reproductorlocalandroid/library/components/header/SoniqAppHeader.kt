package mx.musicalesdoris.reproductorlocalandroid.library.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.R
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont

@Composable
fun SoniqAppHeader(theme: PlayerTheme, onImport: () -> Unit, onProfile: () -> Unit, onEqualizer: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).background(
        Brush.verticalGradient(listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle)))
        .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(SoniqPalette.electricBlue, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.soniq_audio_lines), null, Modifier.size(23.dp))
        }
        Spacer(Modifier.width(9.dp))
        Text("SONIQ", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
            fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.4).sp)
        Spacer(Modifier.width(9.dp))
        Text("AUDIO\nLOCAL", color = SoniqPalette.mutedInk, fontFamily = SoniqMonoFont,
            fontSize = 9.sp, lineHeight = 10.8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        HeaderMenu(theme, true, onImport, onProfile = onProfile, onEqualizer = onEqualizer)
    }
}
