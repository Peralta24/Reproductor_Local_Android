package mx.musicalesdoris.reproductorlocalandroid.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.runtime.Composable
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme

/** A no-op indication for Foundation controls inside the SONIQ theme. */
internal object SoniqNoIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        SoniqNoIndicationInstance
}

private object SoniqNoIndicationInstance : IndicationInstance {
    override fun ContentDrawScope.drawIndication() = drawContent()
}

/** Disables ripple layers used internally by Material 3 controls. */
internal object SoniqNoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = Color.Transparent

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
        pressedAlpha = 0f,
        focusedAlpha = 0f,
        draggedAlpha = 0f,
        hoveredAlpha = 0f
    )
}

/** Clips touch semantics to the control shape; the SONIQ theme supplies a no-op indication. */
fun Modifier.soniqClickable(
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = clip(shape).clickable(enabled = enabled, onClick = onClick)
