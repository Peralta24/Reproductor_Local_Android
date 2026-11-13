package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import java.io.File

/** Dynamic artwork framed as the reusable SONIQ CD jewel case from ReproV1. */
@Composable
fun SoniqCdCase(file: File?, theme: PlayerTheme, size: Dp, seed: String = "") {
    val artwork = rememberArtworkBitmap(file)
    val caseShape = RoundedCornerShape(5.dp)
    Box(Modifier.size(size).shadow(8.dp, caseShape).clip(caseShape)
        .background(Brush.verticalGradient(listOf(Color(0xFF59606A), Color(0xFF151A22), Color(0xFF353B44))))
        .border(1.dp, Color(0xFF7A8795), caseShape).padding(5.dp),
        contentAlignment = Alignment.Center) {
        if (artwork != null) {
            Image(artwork, "Carátula", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            ArtworkView(null, theme, size - 10.dp, seed)
            Box(Modifier.fillMaxSize().background(Color(0xFF09152A).copy(alpha = .18f)))
            Box(Modifier.fillMaxSize(.91f).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(
                    Color(0xFFB6D5FF).copy(alpha = .16f), Color(0xFF111B37).copy(alpha = .52f),
                    Color(0xFF92B7F5).copy(alpha = .22f), Color(0xFF202849).copy(alpha = .46f),
                    Color(0xFFB6D5FF).copy(alpha = .16f)
                )))
                .border(1.dp, Color(0xFFA9C7F0).copy(alpha = .42f), CircleShape),
                contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize(.55f).border(1.dp, Color.White.copy(alpha = .32f), CircleShape))
                Box(Modifier.fillMaxSize(.28f).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF41475E), Color(0xFF111827))))
                    .border(1.dp, Color(0xFF8EA2C0), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize(.25f).clip(CircleShape).background(Color(0xFF080E18))
                        .border(1.dp, Color(0xFFB4C5D8), CircleShape))
                }
            }
        }
        Box(Modifier.align(Alignment.TopStart).fillMaxSize().padding(1.dp)
            .border(1.dp, Color.White.copy(alpha = .2f), caseShape))
    }
}
