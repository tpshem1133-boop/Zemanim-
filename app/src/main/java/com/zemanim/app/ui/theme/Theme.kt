package com.zemanim.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Futuristic modern palette: electric cyan primary, violet secondary,
 * gold reserved for the sun / countdown accents. Dark mode is the
 * showcase (deep-space background, dark-glass surfaces); light mode is
 * the same accent system on a clean near-white base.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0A0F1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0F1E),
    surfaceVariant = Color(0xFFE9EEF7),
    onSurfaceVariant = Color(0xFF5B6B8C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5FA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9EEF7),
    surfaceContainerHighest = Color(0xFFDEE6F2),
    outline = Color(0xFFD8E0EE),
    outlineVariant = Color(0xFFE6EBF4),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF062A33),
    primaryContainer = Color(0xFF0B2E38),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF241B4D),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF231A04),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFF2F6FF),
    surface = Color(0xFF101828),
    onSurface = Color(0xFFEEF2FA),
    surfaceVariant = Color(0xFF182238),
    onSurfaceVariant = Color(0xFF9AA7C2),
    surfaceContainerLowest = Color(0xFF05080F),
    surfaceContainerLow = Color(0xFF0B1120),
    surfaceContainer = Color(0xFF101828),
    surfaceContainerHigh = Color(0xFF16203A),
    surfaceContainerHighest = Color(0xFF1C2947),
    outline = Color(0xFF24304A),
    outlineVariant = Color(0xFF1B2740),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

/** Crisp sans throughout — futuristic means clean sans, not serif. */
private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp)
)

/**
 * App theme. [themePref] is "auto" (follow system), "light" or "dark".
 */
@Composable
fun ZemanimTheme(themePref: String, content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
