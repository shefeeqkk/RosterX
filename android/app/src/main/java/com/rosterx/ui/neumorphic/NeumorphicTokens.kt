package com.rosterx.ui.neumorphic

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RosterX — Unified Cross-Platform Neumorphic (Soft UI) Design Tokens
 * 
 * Establishes universal light physics (135° Key Light), monochromatic canvas baseline,
 * standard elevation strata, and organic curvature across Android Jetpack Compose.
 */
object NeumorphicTokens {

    // ==========================================
    // 1. Monochromatic Canvas & Surface Baseline
    // ==========================================
    /** Calibrated cool-slate off-white baseline tone for canvas and component bodies */
    val Surface: Color = Color(0xFFEEF2F6)
    val SurfaceContrast: Color = Color(0xFFE2E8F0)
    val Canvas: Color = Color(0xFFEEF2F6)

    // ==========================================
    // 2. Dual-Opposite Light-Source Coordinates (135° Key Light)
    // ==========================================
    /** Top-Left Light Vector (-X, -Y): Pure diffuse specular highlight */
    val ShadowLight: Color = Color(0xE6FFFFFF) // rgba(255, 255, 255, 0.90)
    val ShadowLightSubtle: Color = Color(0xA6FFFFFF) // rgba(255, 255, 255, 0.65)

    /** Bottom-Right Shadow Vector (+X, +Y): Soft desaturated occlusion shadow */
    val ShadowDark: Color = Color(0x8C92A3B8) // rgba(146, 163, 184, 0.55) - Deep soft drop shadow
    val ShadowDarkSubtle: Color = Color(0x5992A3B8) // rgba(146, 163, 184, 0.35)
    val ShadowDarkAmbient: Color = Color(0x3392A3B8) // rgba(166, 180, 200, 0.25)

    // ==========================================
    // 3. Kinetic Energy Accent & Accessibility
    // ==========================================
    val AccentCoral: Color = Color(0xFFFF6B6B)
    val AccentSalmon: Color = Color(0xFFFF5252)
    val AccentGlow: Color = Color(0x59FF5252) // rgba(255, 82, 82, 0.35)

    val AccentGradient: Brush = Brush.linearGradient(
        colors = listOf(AccentCoral, AccentSalmon),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // NOC Status Accent Accoutrements
    val StatusCyan: Color = Color(0xFF0284C7)
    val StatusEmerald: Color = Color(0xFF10B981)
    val StatusAmber: Color = Color(0xFFF59E0B)

    // ==========================================
    // 4. Typography & High-Contrast Guard
    // ==========================================
    val TextMain: Color = Color(0xFF2D3748) // Strict dark slate text for high contrast on #EEF2F6
    val TextMuted: Color = Color(0xFF718096)
    val TextLight: Color = Color(0xFFA0AEC0)

    // ==========================================
    // 5. Geometry & Organic Curvature Radii
    // ==========================================
    val CornerSmall: Dp = 14.dp      // Badges, chips, shift pills (12dp - 16dp)
    val CornerMedium: Dp = 20.dp     // Buttons, text inputs, tile cards (18dp - 22dp)
    val CornerContainer: Dp = 28.dp  // Containers, dialogs, bottom sheets (26dp - 32dp)
    val CornerFull: Dp = 999.dp      // Circular toggles, action FABs

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

    /** Level 1 — Extruded / Raised (Default Interactive & Surface Containers) */
    val ElevationRaised = ElevationSpec(
        offsetX = 6.dp,
        offsetY = 6.dp,
        blurRadius = 14.dp,
        darkShadow = ShadowDark,
        lightShadow = ShadowLight
    )

    val ElevationRaisedHover = ElevationSpec(
        offsetX = 8.dp,
        offsetY = 8.dp,
        blurRadius = 18.dp,
        darkShadow = ShadowDark,
        lightShadow = ShadowLight
    )

    val ElevationRaisedSubtle = ElevationSpec(
        offsetX = 3.dp,
        offsetY = 3.dp,
        blurRadius = 8.dp,
        darkShadow = ShadowDarkSubtle,
        lightShadow = ShadowLightSubtle
    )

    /** Level 2 — Sunken / Inset (Recessed Depressions: Form inputs, active toggles) */
    val ElevationInset = ElevationSpec(
        offsetX = 4.dp,
        offsetY = 4.dp,
        blurRadius = 8.dp,
        darkShadow = ShadowDark,
        lightShadow = ShadowLight
    )

    val ElevationInsetSubtle = ElevationSpec(
        offsetX = 2.dp,
        offsetY = 2.dp,
        blurRadius = 5.dp,
        darkShadow = ShadowDarkSubtle,
        lightShadow = ShadowLightSubtle
    )

    /** Level 3 — Deep Well (Engraved / Slotted: Progress tracks, counter wells) */
    val ElevationDeepWell = ElevationSpec(
        offsetX = 6.dp,
        offsetY = 6.dp,
        blurRadius = 12.dp,
        darkShadow = ShadowDark,
        lightShadow = ShadowLight
    )

    /** Level 4 — High-Floating Layer (Modals, Action Drawers, Sticky Overlays) */
    val ElevationFloating = ElevationSpec(
        offsetX = 14.dp,
        offsetY = 14.dp,
        blurRadius = 28.dp,
        darkShadow = ShadowDark,
        lightShadow = ShadowLight
    )
}
