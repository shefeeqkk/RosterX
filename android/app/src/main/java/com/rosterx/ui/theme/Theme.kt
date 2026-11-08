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
import com.rosterx.data.local.AppThemeMode
import com.rosterx.ui.neumorphic.DefaultPalette
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.ThemePalette
import com.rosterx.ui.neumorphic.getPaletteById

val LocalNeumorphicTokens = staticCompositionLocalOf { NeumorphicTokens }
val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalThemeAccent = staticCompositionLocalOf { Color(0xFF174EAC) }
val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }
val LocalThemePalette = staticCompositionLocalOf { DefaultPalette }

@Composable
fun RosterXTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    paletteId: String = "tory_blue",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val palette = getPaletteById(paletteId)
    val accentColor = palette.resolve(isDark)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            secondary = accentColor,
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
    } else {
        lightColorScheme(
            primary = accentColor,
            secondary = accentColor,
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
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalNeumorphicTokens provides NeumorphicTokens,
        LocalIsDarkTheme provides isDark,
        LocalThemeAccent provides accentColor,
        LocalThemeMode provides themeMode,
        LocalThemePalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
