package com.pizzadough.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pizzadough.app.R

/** Italian trattoria palette — ported from the web app (v5.2). */
val BasilDark = Color(0xFF17452C)
val Basil = Color(0xFF1F603C)
val Tomato = Color(0xFFA93226)
val TomatoDark = Color(0xFF8A271E)
val Gold = Color(0xFFC99700)

private val LightScheme = lightColorScheme(
    primary = Tomato,
    onPrimary = Color.White,
    secondary = Basil,
    onSecondary = Color.White,
    tertiary = Gold,
    background = Color(0xFFF7F3E9),   // flour ivory
    onBackground = Color(0xFF232A22),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF232A22),
    surfaceVariant = Color(0xFFEFE7D2),
    onSurfaceVariant = Color(0xFF5B564A),
    outline = Color(0xFFE0D6BC),
    error = Color(0xFFB3261E)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC24A36),
    onPrimary = Color.White,
    secondary = Color(0xFF9CC7A6),
    onSecondary = Color(0xFF0F1F14),
    tertiary = Color(0xFFD9A441),
    background = Color(0xFF141711),
    onBackground = Color(0xFFF1ECDD),
    surface = Color(0xFF1F241E),
    onSurface = Color(0xFFF1ECDD),
    surfaceVariant = Color(0xFF2A302A),
    onSurfaceVariant = Color(0xFFB9B39F),
    outline = Color(0xFF3A4034),
    error = Color(0xFFFFB4AB)
)

/**
 * Liquid Glass tokens. Deliberately cheap: static translucent layers +
 * hairline light borders instead of per-frame blur (see performance notes).
 */
@Immutable
data class GlassColors(
    val top: Color,
    val bottom: Color,
    val border: Color
)

private val LightGlass = GlassColors(
    top = Color.White.copy(alpha = 0.55f),
    bottom = Color.White.copy(alpha = 0.22f),
    border = Color.White.copy(alpha = 0.65f)
)

private val DarkGlass = GlassColors(
    top = Color.White.copy(alpha = 0.10f),
    bottom = Color.White.copy(alpha = 0.04f),
    border = Color.White.copy(alpha = 0.18f)
)

val LocalGlassColors = compositionLocalOf { LightGlass }

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/** Tabular numerals for result tables (mirrors the web app's font-variant-numeric). */
val TabularStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun PizzaDoughTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val glass = if (darkTheme) DarkGlass else LightGlass
    CompositionLocalProvider(LocalGlassColors provides glass) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = scheme,
            typography = Typography(defaultFontFamily = Vazirmatn),
            content = content
        )
    }
}
