package mx.musicalesdoris.reproductorlocalandroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import mx.musicalesdoris.reproductorlocalandroid.R

/** Typeface families named by the SONIQ Y2K styles in ReproV1. */
val SoniqHeadingFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)
val SoniqBodyFont = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)
val SoniqMonoFont = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold)
)

private fun TextStyle.withFont(family: FontFamily, weight: FontWeight? = null) = copy(
    fontFamily = family,
    fontWeight = weight ?: fontWeight
)

/** Preserve the platform scale of each Material style while using SONIQ's font families. */
val Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.withFont(SoniqHeadingFont, FontWeight.Bold),
        displayMedium = base.displayMedium.withFont(SoniqHeadingFont, FontWeight.Bold),
        displaySmall = base.displaySmall.withFont(SoniqHeadingFont, FontWeight.Bold),
        headlineLarge = base.headlineLarge.withFont(SoniqHeadingFont, FontWeight.Bold),
        headlineMedium = base.headlineMedium.withFont(SoniqHeadingFont, FontWeight.Bold),
        headlineSmall = base.headlineSmall.withFont(SoniqHeadingFont, FontWeight.Bold),
        titleLarge = base.titleLarge.withFont(SoniqHeadingFont, FontWeight.Bold),
        titleMedium = base.titleMedium.withFont(SoniqBodyFont, FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFont(SoniqBodyFont, FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.withFont(SoniqBodyFont, FontWeight.Normal),
        bodyMedium = base.bodyMedium.withFont(SoniqBodyFont, FontWeight.Normal),
        bodySmall = base.bodySmall.withFont(SoniqBodyFont, FontWeight.Normal),
        labelLarge = base.labelLarge.withFont(SoniqBodyFont, FontWeight.SemiBold),
        labelMedium = base.labelMedium.withFont(SoniqBodyFont, FontWeight.SemiBold),
        labelSmall = base.labelSmall.withFont(SoniqBodyFont, FontWeight.SemiBold)
    )
}
