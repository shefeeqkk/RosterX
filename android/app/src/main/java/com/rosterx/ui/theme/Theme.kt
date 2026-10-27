package com.rosterx.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rosterx.ui.neumorphic.NeumorphicTokens

val LocalNeumorphicTokens = staticCompositionLocalOf { NeumorphicTokens }

// Ambient Space Theme Colors (Legacy Dark Mode NOC Command Center)
private val AmbientDarkBackground = Color(0xFF090D16)
private val AmbientCyan = Color(0xFF0284C7)
private val AmbientIndigo = Color(0xFF4F46E5)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = AmbientIndigo,
    tertiary = Color(0xFF06B6D4),
    background = AmbientDarkBackground,
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFFD1D5DB)
)

// Universal Neumorphic Soft UI Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = NeumorphicTokens.AccentSalmon,
    secondary = NeumorphicTokens.AccentCoral,
    tertiary = NeumorphicTokens.StatusCyan,
    background = NeumorphicTokens.Canvas,
    surface = NeumorphicTokens.Surface,
    surfaceVariant = NeumorphicTokens.SurfaceContrast,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NeumorphicTokens.TextMain,
    onSurface = NeumorphicTokens.TextMain,
    onSurfaceVariant = NeumorphicTokens.TextMuted
)

@Composable
fun RosterXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNeumorphicTokens provides NeumorphicTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
