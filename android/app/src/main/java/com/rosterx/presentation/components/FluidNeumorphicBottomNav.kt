package com.rosterx.presentation.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicAccentGlow
import com.rosterx.ui.neumorphic.neumorphicRaised
import kotlin.math.abs

data class NavTabItem(
    val index: Int,
    val title: String,
    val icon: ImageVector
)

/**
 * RosterX — Animated Fluid Bottom Navigation Bar (Neumorphic Wave Notch & Floating Curved Pill)
 *
 * Design Features:
 * 1. Monochromatic Neumorphic surface floating dock matching system Light/Dark theme.
 * 2. Continuous, fluid concave cubic-bezier wave cutout notch centered beneath active selection.
 * 3. Elevated active Curved Stadium/Pill floating -18dp above notch, containing BOTH active Icon + Text Label together!
 * 4. Spring-dampened motion curve with lateral squash & stretch physics deformation.
 */
@Composable
fun FluidNeumorphicBottomNav(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val itemCount = items.size.coerceAtLeast(1)
    val tabWidthPx = if (containerWidthPx > 0) containerWidthPx / itemCount else 0f

    // Target center X position of active tab
    val targetNotchCenterX = if (tabWidthPx > 0) {
        (selectedIndex * tabWidthPx) + (tabWidthPx / 2f)
    } else 0f

    // Fluid spring-dampened X-axis translation animation
    val animatedNotchCenterX by animateFloatAsState(
        targetValue = targetNotchCenterX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "notchXAnimation"
    )

    // Physics Squash & Stretch deformation during transition
    val isMoving = abs(targetNotchCenterX - animatedNotchCenterX) > with(density) { 4.dp.toPx() }
    val bubbleScaleX by animateFloatAsState(
        targetValue = if (isMoving) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bubbleScaleX"
    )
    val bubbleScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bubbleScaleY"
    )

    val notchWidthPx = with(density) { 120.dp.toPx() }
    val notchDepthPx = with(density) { 22.dp.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

    val canvasColor = NeumorphicTokens.Canvas
    val surfaceColor = NeumorphicTokens.Surface
    val textMainColor = NeumorphicTokens.TextMain
    val textMutedColor = NeumorphicTokens.TextMuted
    val shadowDarkColor = NeumorphicTokens.ShadowDark
    val shadowLightColor = NeumorphicTokens.ShadowLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .onGloballyPositioned { coordinates ->
                containerWidthPx = coordinates.size.width.toFloat()
            }
    ) {
        // A. Custom Neumorphic Dock Surface with Concave Cutout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
                .drawBehind {
                    if (size.width <= 0 || size.height <= 0) return@drawBehind

                    val path = createFluidDockPath(
                        size = size,
                        notchCenterX = animatedNotchCenterX,
                        notchWidth = notchWidthPx,
                        notchDepth = notchDepthPx,
                        cornerRadius = cornerRadiusPx
                    )

                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas

                        // 1. Bottom-Right Soft Occlusion Shadow (+X, +Y)
                        val darkPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = shadowDarkColor.toArgb()
                            setShadowLayer(14.dp.toPx(), 6.dp.toPx(), 6.dp.toPx(), shadowDarkColor.toArgb())
                            maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawPath(path.asAndroidPath(), darkPaint)

                        // 2. Top-Left Diffuse Specular Highlight (-X, -Y)
                        val lightPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = shadowLightColor.toArgb()
                            setShadowLayer(12.dp.toPx(), -4.dp.toPx(), -4.dp.toPx(), shadowLightColor.toArgb())
                            maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawPath(path.asAndroidPath(), lightPaint)
                    }

                    // 3. Molded Canvas Base Fill
                    drawPath(path = path, color = surfaceColor)
                }
        )

        // B. Floating Active Circular Bubble (Icon only, fluid morphing)
        if (containerWidthPx > 0 && selectedIndex in items.indices) {
            val activeItem = items[selectedIndex]
            val bubbleSize = 56.dp
            val bubbleSizePx = with(density) { bubbleSize.toPx() }
            val bubbleLeftPx = animatedNotchCenterX - (bubbleSizePx / 2f)
            val bubbleLeftDp = with(density) { bubbleLeftPx.toDp() }

            Box(
                modifier = Modifier
                    .offset(x = bubbleLeftDp, y = 0.dp)
                    .size(bubbleSize)
                    .scale(scaleX = bubbleScaleX, scaleY = bubbleScaleY)
                    .neumorphicAccentGlow(glowColor = NeumorphicTokens.AccentGlow, radius = 10.dp)
                    .neumorphicRaised(
                        shape = CircleShape,
                        elevation = NeumorphicTokens.ElevationRaised,
                        surfaceColor = surfaceColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activeItem.icon,
                    contentDescription = activeItem.title,
                    tint = NeumorphicTokens.AccentSalmon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // C. Interactive Inactive Tab Triggers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "tabItemScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scale(itemScale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(item.index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isSelected) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = textMutedColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.title,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                color = textMutedColor
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxHeight()
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeumorphicTokens.AccentSalmon
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates an organic cubic-bezier concave cutout wave shape along the top perimeter of the dock.
 */
private fun createFluidDockPath(
    size: Size,
    notchCenterX: Float,
    notchWidth: Float,
    notchDepth: Float,
    cornerRadius: Float
): Path {
    val path = Path()
    val width = size.width
    val height = size.height

    val halfNotch = notchWidth / 2f
    val startNotchX = (notchCenterX - halfNotch).coerceIn(0f, (width - notchWidth).coerceAtLeast(0f))
    val endNotchX = (notchCenterX + halfNotch).coerceIn(notchWidth, width)
    val actualCenterX = (startNotchX + endNotchX) / 2f

    // Top-left corner start
    path.moveTo(0f, cornerRadius)
    path.quadraticBezierTo(0f, 0f, cornerRadius, 0f)

    // Flat line to start of notch
    path.lineTo(startNotchX, 0f)

    // Organic Concave Cubic Bezier Cutout Wave
    path.cubicTo(
        startNotchX + notchWidth * 0.22f, 0f,
        startNotchX + notchWidth * 0.32f, notchDepth,
        actualCenterX, notchDepth
    )
    path.cubicTo(
        endNotchX - notchWidth * 0.32f, notchDepth,
        endNotchX - notchWidth * 0.22f, 0f,
        endNotchX, 0f
    )

    // Line to top-right corner
    path.lineTo((width - cornerRadius).coerceAtLeast(endNotchX), 0f)
    path.quadraticBezierTo(width, 0f, width, cornerRadius)

    // Right, Bottom, Left perimeter
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()

    return path
}
