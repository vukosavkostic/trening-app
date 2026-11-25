package com.vukos.trening.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.vukos.trening.i18n.tr
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

/** Boje dizajna v2 (Claude Design, "Trening Prototype v2") za jednu temu. */
data class DsColors(
    val bg: Color, val s1: Color, val s2: Color, val s3: Color, val sheet: Color,
    val ink: Color, val sub: Color, val faint: Color, val line: Color,
    val aText: Color, val aSoft: Color, val aOn: Color, val navInd: Color,
    val green: Color, val danger: Color, val dark: Boolean,
)

val DarkDs = DsColors(
    bg = Color(0xFF0B0B0C), s1 = Color(0xFF161618), s2 = Color(0xFF1C1C1E), s3 = Color(0xFF2A2A2D), sheet = Color(0xFF1C1C1E),
    ink = Color(0xFFF5F5F7), sub = Color(0xFF8E8E93), faint = Color(0xFF5A5A5F), line = Color(0x0FFFFFFF),
    aText = Color(0xFFFF6A33), aSoft = Accent.copy(alpha = 0.16f), aOn = Color(0xFFFFB693), navInd = Color(0xFF3A1D10),
    green = Color(0xFF30D158), danger = Color(0xFFFF453A), dark = true,
)

val LightDs = DsColors(
    bg = Color(0xFFF2F2F4), s1 = Color(0xFFFFFFFF), s2 = Color(0xFFEDEDF0), s3 = Color(0xFFE1E1E6), sheet = Color(0xFFF2F2F4),
    ink = Color(0xFF111113), sub = Color(0xFF6C6C72), faint = Color(0xFFA8A8AE), line = Color(0x12000000),
    aText = Color(0xFFD9460E), aSoft = Accent.copy(alpha = 0.12f), aOn = Color(0xFFA8370C), navInd = Color(0xFFFFE3D6),
    green = Color(0xFF1F9D55), danger = Color(0xFFE5352B), dark = false,
)

val LocalDs = staticCompositionLocalOf { DarkDs }

/**
 * Boje trenutne teme: bg = pozadina, s1 = kartica, s2 = prazan dan / polje, s3 = traka / sekundarno dugme.
 * Citaju se samo u @Composable kodu (za crtanje na Canvas-u prvo uzmi vrednost u promenljivu).
 */
object Ds {
    val bg: Color @Composable @ReadOnlyComposable get() = LocalDs.current.bg
    val s1: Color @Composable @ReadOnlyComposable get() = LocalDs.current.s1
    val s2: Color @Composable @ReadOnlyComposable get() = LocalDs.current.s2
    val s3: Color @Composable @ReadOnlyComposable get() = LocalDs.current.s3
    val sheet: Color @Composable @ReadOnlyComposable get() = LocalDs.current.sheet
    val ink: Color @Composable @ReadOnlyComposable get() = LocalDs.current.ink
    val sub: Color @Composable @ReadOnlyComposable get() = LocalDs.current.sub
    val faint: Color @Composable @ReadOnlyComposable get() = LocalDs.current.faint
    val line: Color @Composable @ReadOnlyComposable get() = LocalDs.current.line
    /** Narandzasti tekst (na tamnoj podlozi malo svetliji od [Accent], na svetloj tamniji). */
    val aText: Color @Composable @ReadOnlyComposable get() = LocalDs.current.aText
    val aSoft: Color @Composable @ReadOnlyComposable get() = LocalDs.current.aSoft
    val aOn: Color @Composable @ReadOnlyComposable get() = LocalDs.current.aOn
    val navInd: Color @Composable @ReadOnlyComposable get() = LocalDs.current.navInd
    val green: Color @Composable @ReadOnlyComposable get() = LocalDs.current.green
    val danger: Color @Composable @ReadOnlyComposable get() = LocalDs.current.danger
}

/** Izgled: sistem / tamno / svetlo. Stanje je "snapshot state", pa se UI odmah menja. */
object Appearance {
    const val SYSTEM = "system"
    const val DARK = "dark"
    const val LIGHT = "light"
    var mode by mutableStateOf(DARK)
        private set

    fun init(context: Context) {
        mode = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("appearance", DARK) ?: DARK
    }

    fun set(context: Context, m: String) {
        mode = m
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("appearance", m).apply()
    }

    fun label(m: String = mode): String = when (m) {
        SYSTEM -> tr("Sistem", "System"); LIGHT -> tr("Svetlo", "Light"); else -> tr("Tamno", "Dark")
    }
}

private fun scheme(d: DsColors) = if (d.dark) darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = d.navInd,
    onPrimaryContainer = d.aOn,
    secondary = Color(0xFFB0B0B0),
    // Izabrani cipovi / segmenti: neutralno (bez podrazumevane ljubicaste).
    secondaryContainer = Color(0xFF34343A),
    onSecondaryContainer = d.ink,
    tertiary = Accent,
    background = d.bg,
    onBackground = d.ink,
    surface = d.bg,
    onSurface = d.ink,
    surfaceVariant = Color(0xFF232327),
    onSurfaceVariant = d.sub,
    surfaceContainerLowest = Color(0xFF080809),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = d.s1,
    surfaceContainerHigh = d.s2,
    surfaceContainerHighest = d.s3,
    outline = Color(0xFF4A4A52),
    outlineVariant = Color(0xFF2A2A2F),
    error = Danger,
) else lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = d.navInd,
    onPrimaryContainer = d.aOn,
    secondary = Color(0xFF5A5A60),
    secondaryContainer = Color(0xFFE1E1E6),
    onSecondaryContainer = d.ink,
    tertiary = Accent,
    background = d.bg,
    onBackground = d.ink,
    surface = d.bg,
    onSurface = d.ink,
    surfaceVariant = Color(0xFFE6E6EA),
    onSurfaceVariant = d.sub,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = d.s1,
    surfaceContainerHigh = d.s2,
    surfaceContainerHighest = d.s3,
    outline = Color(0xFFB4B4BA),
    outlineVariant = Color(0xFFD8D8DD),
    error = d.danger,
)

private val DarkScheme = scheme(DarkDs)
private val LightScheme = scheme(LightDs)

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

/** Da li je trenutno tamna tema (po izboru u Podešavanjima ili po sistemu). */
@Composable
fun isDarkTheme(): Boolean = when (Appearance.mode) {
    Appearance.LIGHT -> false
    Appearance.SYSTEM -> isSystemInDarkTheme()
    else -> true
}

@Composable
fun TreningTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    CompositionLocalProvider(LocalDs provides if (dark) DarkDs else LightDs) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
