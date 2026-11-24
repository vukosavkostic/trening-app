package com.vukos.trening.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vukos.trening.R

val Accent = Color(0xFFFF5A1F)
val Danger = Color(0xFFE5484D)
val Success = Color(0xFF3DD68C)

/**
 * Boje dizajna v2 (Claude Design, "Trening Prototype v2"): tamna tema.
 * bg = pozadina, s1 = kartica, s2 = prazan dan / polje, s3 = traka / sekundarno dugme.
 */
object Ds {
    val bg = Color(0xFF0B0B0C)
    val s1 = Color(0xFF161618)
    val s2 = Color(0xFF1C1C1E)
    val s3 = Color(0xFF2A2A2D)
    val ink = Color(0xFFF5F5F7)
    val sub = Color(0xFF8E8E93)
    val faint = Color(0xFF5A5A5F)
    val line = Color(0x0FFFFFFF)
    /** Narandzasti tekst na tamnoj podlozi (malo svetliji od [Accent]). */
    val aText = Color(0xFFFF6A33)
    val aSoft = Accent.copy(alpha = 0.16f)
    val aOn = Color(0xFFFFB693)
    val green = Color(0xFF30D158)
    val danger = Color(0xFFFF453A)
}

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1D10),
    onPrimaryContainer = Ds.aOn,
    secondary = Color(0xFFB0B0B0),
    // Izabrani cipovi / segmenti: neutralno (bez podrazumevane ljubicaste).
    secondaryContainer = Color(0xFF34343A),
    onSecondaryContainer = Ds.ink,
    tertiary = Accent,
    background = Ds.bg,
    onBackground = Ds.ink,
    surface = Ds.bg,
    onSurface = Ds.ink,
    surfaceVariant = Color(0xFF232327),
    onSurfaceVariant = Ds.sub,
    surfaceContainerLowest = Color(0xFF080809),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Ds.s1,
    surfaceContainerHigh = Ds.s2,
    surfaceContainerHighest = Ds.s3,
    outline = Color(0xFF4A4A52),
    outlineVariant = Color(0xFF2A2A2F),
    error = Danger,
)

/** Manrope (variabilni font, OFL) - moderan geometrijski sans. */
@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(
    R.font.manrope,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
)

private val AppTypography = Typography(
    displaySmall = style(36, FontWeight.ExtraBold, 42, -0.5),
    headlineLarge = style(32, FontWeight.ExtraBold, 38, -0.5),
    headlineMedium = style(28, FontWeight.ExtraBold, 34, -0.3),
    headlineSmall = style(22, FontWeight.Bold, 28, -0.2),
    titleLarge = style(20, FontWeight.Bold, 26),
    titleMedium = style(16, FontWeight.Bold, 22),
    titleSmall = style(14, FontWeight.Bold, 20),
    bodyLarge = style(16, FontWeight.Medium, 24),
    bodyMedium = style(14, FontWeight.Medium, 20),
    bodySmall = style(13, FontWeight.Medium, 18),
    labelLarge = style(15, FontWeight.Bold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 16),
    labelSmall = style(11, FontWeight.SemiBold, 14, 0.3),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TreningTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, shapes = AppShapes, content = content)
}
