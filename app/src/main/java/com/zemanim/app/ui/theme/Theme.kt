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

/** Warm light scheme: deep navy primary, gold accent, paper background. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF1E2A4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE3F5),
    onPrimaryContainer = Color(0xFF1E2A4A),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFDF0D0),
    tertiary = Color(0xFF8A6D1B),
    background = Color(0xFFF7F4EC),
    onBackground = Color(0xFF23232B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF23232B),
    surfaceVariant = Color(0xFFEFE9DC),
    onSurfaceVariant = Color(0xFF5C5C68),
    outline = Color(0xFFE7E0D2)
)

/** Dark scheme: softened navy/gold on near-black warm surfaces. */
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF1E2A4A),
    primaryContainer = Color(0xFF2C3F6E),
    onPrimaryContainer = Color(0xFFDCE3F5),
    secondary = Color(0xFFE0A458),
    onSecondary = Color(0xFF1E1E26),
    secondaryContainer = Color(0xFF2A2416),
    tertiary = Color(0xFFC9A227),
    background = Color(0xFF141419),
    onBackground = Color(0xFFF0EDE4),
    surface = Color(0xFF1E1E26),
    onSurface = Color(0xFFF0EDE4),
    surfaceVariant = Color(0xFF2A2A33),
    onSurfaceVariant = Color(0xFFA8A49A),
    outline = Color(0xFF33333D)
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp,
        lineHeight = 24.sp
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
