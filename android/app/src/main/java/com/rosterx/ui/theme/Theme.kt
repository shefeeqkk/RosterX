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

private val DarkColorScheme = darkColorScheme(
    primary = NeumorphicTokens.AccentSalmon,
    secondary = NeumorphicTokens.AccentCoral,
    tertiary = NeumorphicTokens.StatusCyan,
    background = NeumorphicTokens.DarkCanvas,
    surface = NeumorphicTokens.DarkSurface,
    surfaceVariant = NeumorphicTokens.DarkSurfaceContrast,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NeumorphicTokens.DarkTextMain,
    onSurface = NeumorphicTokens.DarkTextMain,
    onSurfaceVariant = NeumorphicTokens.DarkTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = NeumorphicTokens.AccentSalmon,
    secondary = NeumorphicTokens.AccentCoral,
    tertiary = NeumorphicTokens.StatusCyan,
    background = NeumorphicTokens.LightCanvas,
    surface = NeumorphicTokens.LightSurface,
    surfaceVariant = NeumorphicTokens.LightSurfaceContrast,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NeumorphicTokens.LightTextMain,
    onSurface = NeumorphicTokens.LightTextMain,
    onSurfaceVariant = NeumorphicTokens.LightTextMuted
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
