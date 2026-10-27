package com.rosterx.ui.neumorphic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RosterX — Dynamic Cross-Platform Neumorphic (Soft UI) Design Tokens
 * 
 * Supports both Light Mode (#EEF2F6 baseline) and Dark Mode (#1E293B baseline)
 * with dual-opposite specular/occlusion light physics and contrast guard.
 */
object NeumorphicTokens {

    // ==========================================
    // 1. Light Theme Baseline Tokens
    // ==========================================
    val LightCanvas: Color = Color(0xFFEEF2F6)
    val LightSurface: Color = Color(0xFFEEF2F6)
    val LightSurfaceContrast: Color = Color(0xFFE2E8F0)
    val LightTextMain: Color = Color(0xFF1E293B)
    val LightTextMuted: Color = Color(0xFF64748B)
    val LightShadowDark: Color = Color(0x8C92A3B8) // rgba(146, 163, 184, 0.55)
    val LightShadowLight: Color = Color(0xE6FFFFFF)

    // ==========================================
    // 2. Dark Theme Baseline Tokens
    // ==========================================
    val DarkCanvas: Color = Color(0xFF1E293B) // Dark Slate Baseline
    val DarkSurface: Color = Color(0xFF1E293B)
    val DarkSurfaceContrast: Color = Color(0xFF334155)
    val DarkTextMain: Color = Color(0xFFF8FAFC) // Crisp Off-White Text
    val DarkTextMuted: Color = Color(0xFF94A3B8) // Soft Slate Text
    val DarkShadowDark: Color = Color(0xCC0B1120) // Deep Dark Occlusion
    val DarkShadowLight: Color = Color(0x26475569) // Subtle Specular Edge

    // ==========================================
    // 3. Dynamic Composable Color Getters
    // ==========================================
    val Canvas: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkCanvas else LightCanvas

    val Surface: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkSurface else LightSurface

    val SurfaceContrast: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkSurfaceContrast else LightSurfaceContrast

    val TextMain: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkTextMain else LightTextMain

    val TextMuted: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkTextMuted else LightTextMuted

    val ShadowDark: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkShadowDark else LightShadowDark

    val ShadowLight: Color
        @Composable get() = if (isSystemInDarkTheme()) DarkShadowLight else LightShadowLight

    // ==========================================
    // 4. Kinetic Energy Accent & Status Accents
    // ==========================================
    val AccentCoral: Color = Color(0xFFFF6B6B)
    val AccentSalmon: Color = Color(0xFFFF5252)
    val AccentGlow: Color = Color(0x59FF5252)

    val AccentGradient: Brush = Brush.linearGradient(
        colors = listOf(AccentCoral, AccentSalmon),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val StatusCyan: Color = Color(0xFF0284C7)
    val StatusEmerald: Color = Color(0xFF10B981)
    val StatusAmber: Color = Color(0xFFF59E0B)

    // ==========================================
    // 5. Geometry & Organic Curvature Radii
    // ==========================================
    val CornerSmall: Dp = 14.dp
    val CornerMedium: Dp = 20.dp
    val CornerContainer: Dp = 28.dp
    val CornerFull: Dp = 999.dp

    val ShapeSmall: Shape = RoundedCornerShape(CornerSmall)
    val ShapeMedium: Shape = RoundedCornerShape(CornerMedium)
    val ShapeContainer: Shape = RoundedCornerShape(CornerContainer)
    val ShapeFull: Shape = CircleShape

    // ==========================================
    // 6. Elevation Strata Presets
    // ==========================================
    data class ElevationSpec(
        val offsetX: Dp,
        val offsetY: Dp,
        val blurRadius: Dp,
        val darkShadow: Color,
        val lightShadow: Color
    )

    val ElevationRaised: ElevationSpec
        @Composable get() = ElevationSpec(
            offsetX = 6.dp,
            offsetY = 6.dp,
            blurRadius = 14.dp,
            darkShadow = ShadowDark,
            lightShadow = ShadowLight
        )

    val ElevationRaisedSubtle: ElevationSpec
        @Composable get() = ElevationSpec(
            offsetX = 3.dp,
            offsetY = 3.dp,
            blurRadius = 8.dp,
            darkShadow = ShadowDark,
            lightShadow = ShadowLight
        )

    val ElevationInset: ElevationSpec
        @Composable get() = ElevationSpec(
            offsetX = 4.dp,
            offsetY = 4.dp,
            blurRadius = 8.dp,
            darkShadow = ShadowDark,
            lightShadow = ShadowLight
        )

    val ElevationInsetSubtle: ElevationSpec
        @Composable get() = ElevationSpec(
            offsetX = 2.dp,
            offsetY = 2.dp,
            blurRadius = 5.dp,
            darkShadow = ShadowDark,
            lightShadow = ShadowLight
        )

    val ElevationFloating: ElevationSpec
        @Composable get() = ElevationSpec(
            offsetX = 14.dp,
            offsetY = 14.dp,
            blurRadius = 28.dp,
            darkShadow = ShadowDark,
            lightShadow = ShadowLight
        )
}
