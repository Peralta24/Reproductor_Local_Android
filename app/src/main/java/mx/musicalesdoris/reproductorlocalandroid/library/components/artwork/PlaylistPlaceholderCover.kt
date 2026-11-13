package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

/** Empty playlist cover geometry from the verified Figma SVG asset. */
@Composable
fun PlaylistPlaceholderCover(size: Dp, modifier: Modifier = Modifier) {
    val case = RoundedCornerShape(size * .125f)
    Box(modifier.size(size).shadow(7.dp, case).background(
        Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight, Color(0xFFBFDBF7), SoniqPalette.pageTop)), case)
        .border(1.2.dp, Color.White, case), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * .698f).background(
            Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight, Color(0xFFBFDBF7), SoniqPalette.pageTop)), CircleShape)
            .border(1.dp, SoniqPalette.border, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * .1356f).background(SoniqPalette.paper, CircleShape)
                .border(1.dp, SoniqPalette.border, CircleShape))
        }
    }
}
