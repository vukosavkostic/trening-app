package com.vukos.trening.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF5A1F)
val Danger = Color(0xFFE53935)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1F14),
    onPrimaryContainer = Color(0xFFFFC2A8),
    secondary = Color(0xFFB0B0B0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF26272B),
    onSurfaceVariant = Color(0xFFA9AAB0),
    surfaceContainer = Color(0xFF1C1D20),
    surfaceContainerHigh = Color(0xFF232428),
    surfaceContainerHighest = Color(0xFF2B2C31),
    outline = Color(0xFF44454B),
    outlineVariant = Color(0xFF2F3035),
    error = Danger,
)

@Composable
fun TreningTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
