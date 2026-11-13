package mx.musicalesdoris.reproductorlocalandroid.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SoniqTransportIcon(name: String, modifier: Modifier = Modifier, size: Dp = 24.dp,
                       color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * .085f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
            color, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = stroke, cap = StrokeCap.Round)
        fun poly(vararg points: Pair<Float, Float>, fill: Boolean = false) {
            val path = Path().apply {
                moveTo(points.first().first * s, points.first().second * s)
                points.drop(1).forEach { lineTo(it.first * s, it.second * s) }
                if (fill) close()
            }
            drawPath(path, color, style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke, cap = StrokeCap.Round))
        }
        when (name) {
            "play" -> poly(.32f to .18f, .78f to .5f, .32f to .82f, fill = true)
            "pause" -> { drawRoundRect(color, Offset(.28f * s, .2f * s), Size(.16f * s, .6f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke)); drawRoundRect(color, Offset(.56f * s, .2f * s), Size(.16f * s, .6f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke)) }
            "previous" -> { line(.25f, .18f, .25f, .82f); poly(.72f to .2f, .38f to .5f, .72f to .8f, fill = true) }
            "next" -> { poly(.28f to .2f, .62f to .5f, .28f to .8f, fill = true); line(.75f, .18f, .75f, .82f) }
            "shuffle" -> { poly(.14f to .24f, .3f to .24f, .7f to .76f, .88f to .76f); poly(.14f to .76f, .3f to .76f, .7f to .24f, .88f to .24f); poly(.7f to .14f, .88f to .24f, .7f to .34f); poly(.7f to .66f, .88f to .76f, .7f to .86f) }
            "repeat" -> { poly(.24f to .36f, .24f to .28f, .38f to .18f, .76f to .18f, .76f to .38f); poly(.76f to .64f, .76f to .72f, .62f to .82f, .24f to .82f, .24f to .62f); poly(.68f to .28f, .76f to .38f, .84f to .28f); poly(.16f to .72f, .24f to .62f, .32f to .72f) }
            "volume" -> { poly(.14f to .42f, .3f to .42f, .48f to .27f, .48f to .73f, .3f to .58f, .14f to .58f, fill = true); drawArc(color, 300f, 120f, false, Offset(.34f * s, .25f * s), Size(.48f * s, .5f * s), style = Stroke(stroke, cap = StrokeCap.Round)); drawArc(color, 300f, 120f, false, Offset(.43f * s, .14f * s), Size(.64f * s, .72f * s), style = Stroke(stroke, cap = StrokeCap.Round)) }
            "queue" -> { line(.12f, .25f, .63f, .25f); line(.12f, .47f, .54f, .47f); line(.12f, .69f, .47f, .69f); poly(.68f to .4f, .86f to .5f, .68f to .6f); line(.68f, .4f, .68f, .77f); drawCircle(color, s * .07f, Offset(.61f * s, .8f * s)) }
        }
    }
}
