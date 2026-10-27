package com.rosterx.ui.neumorphic

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RosterX — Custom Neumorphic Compose Modifier Extensions
 * 
 * Implements universal light-physics equations:
 * - 135° Ambient Key Light dual-opposite vectors (-X, -Y specular, +X, +Y occlusion)
 * - Inset / Sunken inner shadows for wells and active states
 * - Tactile kinetic press interactions
 */

/**
 * Applies an Extruded / Raised Neumorphic elevation to any Composable.
 * Draws a top-left diffuse white highlight and a bottom-right dark occlusion shadow,
 * molding the component directly from the [NeumorphicTokens.Surface] canvas.
 */
@Composable
fun Modifier.neumorphicRaised(
    shape: Shape = NeumorphicTokens.ShapeMedium,
    elevation: NeumorphicTokens.ElevationSpec = NeumorphicTokens.ElevationRaised,
    surfaceColor: Color = NeumorphicTokens.Surface
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val offsetX = elevation.offsetX.toPx()
    val offsetY = elevation.offsetY.toPx()
    val blurRadius = elevation.blurRadius.toPx()

    drawIntoCanvas { canvas ->
        val frameworkCanvas = canvas.nativeCanvas

        // 1. Bottom-Right Soft Occlusion Drop Shadow (+X, +Y)
        val darkPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = elevation.darkShadow.toArgb()
            setShadowLayer(blurRadius, offsetX, offsetY, elevation.darkShadow.toArgb())
            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
        frameworkCanvas.save()
        frameworkCanvas.translate(offsetX * 0.5f, offsetY * 0.5f)
        drawOutlineIntoCanvas(frameworkCanvas, outline, darkPaint)
        frameworkCanvas.restore()

        // 2. Top-Left Diffuse Specular Highlight (-X, -Y)
        val lightPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = elevation.lightShadow.toArgb()
            setShadowLayer(blurRadius, -offsetX, -offsetY, elevation.lightShadow.toArgb())
            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
        frameworkCanvas.save()
        frameworkCanvas.translate(-offsetX * 0.5f, -offsetY * 0.5f)
        drawOutlineIntoCanvas(frameworkCanvas, outline, lightPaint)
        frameworkCanvas.restore()
    }

    // 3. Molded Canvas Surface Body
    drawOutline(outline, surfaceColor)
}

/**
 * Applies a Sunken / Inset Neumorphic depression to any Composable.
 * Simulates a recessed well with dark shadow along the top-left inner lip
 * and specular highlight along the bottom-right upward-facing lip.
 */
@Composable
fun Modifier.neumorphicInset(
    shape: Shape = NeumorphicTokens.ShapeMedium,
    elevation: NeumorphicTokens.ElevationSpec = NeumorphicTokens.ElevationInset,
    surfaceColor: Color = NeumorphicTokens.Surface
): Modifier = this
    .clip(shape)
    .drawWithContent {
        // Draw the base background first
        drawRect(color = surfaceColor)
        
        // Draw the child content
        drawContent()

        val width = size.width
        val height = size.height
        val depth = elevation.offsetY.toPx() * 1.5f

        // Top-Left Inner Occlusion Shadow (Simulates top lip casting shadow downward-right)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(elevation.darkShadow, Color.Transparent),
                startY = 0f,
                endY = depth
            ),
            size = Size(width, depth)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(elevation.darkShadow, Color.Transparent),
                startX = 0f,
                endX = depth
            ),
            size = Size(depth, height)
        )

        // Bottom-Right Inner Specular Highlight (Simulates ambient key light catching bottom lip)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, elevation.lightShadow),
                startY = height - depth,
                endY = height
            ),
            topLeft = Offset(0f, height - depth),
            size = Size(width, depth)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, elevation.lightShadow),
                startX = width - depth,
                endX = width
            ),
            topLeft = Offset(width - depth, 0f),
            size = Size(depth, height)
        )
    }

/**
 * High-Floating Layer elevation for Modals, Dialogs, and Action Drawers.
 */
@Composable
fun Modifier.neumorphicFloating(
    shape: Shape = NeumorphicTokens.ShapeContainer,
    surfaceColor: Color = NeumorphicTokens.Surface
): Modifier = neumorphicRaised(
    shape = shape,
    elevation = NeumorphicTokens.ElevationFloating,
    surfaceColor = surfaceColor
)

/**
 * Interactive Tactile Neumorphic Modifier.
 * Dynamically and smoothly transitions from Raised elevation to an Inset/sunken well upon press.
 */
fun Modifier.neumorphicClickable(
    shape: Shape = NeumorphicTokens.ShapeMedium,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val actualSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualSource.collectIsPressedAsState()

    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "neumorphicPressElevation"
    )

    if (isPressed) {
        this
            .neumorphicInset(shape = shape, elevation = NeumorphicTokens.ElevationInsetSubtle)
            .clickable(
                interactionSource = actualSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    } else {
        val currentElevation = if (animatedElevation > 0.5f) {
            NeumorphicTokens.ElevationRaised
        } else {
            NeumorphicTokens.ElevationRaisedSubtle
        }
        this
            .neumorphicRaised(shape = shape, elevation = currentElevation)
            .clickable(
                interactionSource = actualSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    }
}

/**
 * Applies a glowing kinetic accent shadow for primary CTA elements.
 */
fun Modifier.neumorphicAccentGlow(
    glowColor: Color = NeumorphicTokens.AccentGlow,
    radius: Dp = 16.dp
): Modifier = this.drawBehind {
    val blurPx = radius.toPx()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = glowColor.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            NeumorphicTokens.CornerMedium.toPx(),
            NeumorphicTokens.CornerMedium.toPx(),
            paint
        )
    }
}

// Helper to render Compose Outline onto Native Android Canvas
private fun drawOutlineIntoCanvas(
    nativeCanvas: android.graphics.Canvas,
    outline: Outline,
    paint: android.graphics.Paint
) {
    when (outline) {
        is Outline.Rectangle -> {
            val rect = outline.rect
            nativeCanvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
        }
        is Outline.Rounded -> {
            val rrect = outline.roundRect
            nativeCanvas.drawRoundRect(
                rrect.left,
                rrect.top,
                rrect.right,
                rrect.bottom,
                rrect.topLeftCornerRadius.x,
                rrect.topLeftCornerRadius.y,
                paint
            )
        }
        is Outline.Generic -> {
            val androidPath = outline.path.asAndroidPath()
            nativeCanvas.drawPath(androidPath, paint)
        }
    }
}

// Helper to fill Compose Outline with a solid color
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutline(
    outline: Outline,
    color: Color
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color, topLeft = outline.rect.topLeft, size = outline.rect.size)
        is Outline.Rounded -> drawRoundRect(
            color = color,
            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                outline.roundRect.topLeftCornerRadius.x,
                outline.roundRect.topLeftCornerRadius.y
            )
        )
        is Outline.Generic -> drawPath(outline.path, color = color)
    }
}
