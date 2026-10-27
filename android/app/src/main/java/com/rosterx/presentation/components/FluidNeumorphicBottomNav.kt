package com.rosterx.presentation.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
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
import androidx.compose.ui.unit.Dp
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
 * RosterX — Animated Fluid Bottom Navigation Bar (Neumorphic Notch & Floating Bubble)
 *
 * Features:
 * 1. Monochromatic Neumorphic `#EEF2F6` floating dock surface.
 * 2. Continuous, fluid concave cubic-bezier wave cutout notch hugging active index.
 * 3. Elevated active circular bubble floating -22dp above notch with coral accent icon & glowing pip.
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
        targetValue = if (isMoving) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bubbleScaleX"
    )
    val bubbleScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.82f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bubbleScaleY"
    )

    val notchWidthPx = with(density) { 88.dp.toPx() }
    val notchDepthPx = with(density) { 22.dp.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

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
                            color = NeumorphicTokens.ShadowDark.toArgb()
                            setShadowLayer(14.dp.toPx(), 6.dp.toPx(), 6.dp.toPx(), NeumorphicTokens.ShadowDark.toArgb())
                            maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawPath(path.asAndroidPath(), darkPaint)

                        // 2. Top-Left Diffuse Specular Highlight (-X, -Y)
                        val lightPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = NeumorphicTokens.ShadowLight.toArgb()
                            setShadowLayer(12.dp.toPx(), -4.dp.toPx(), -4.dp.toPx(), NeumorphicTokens.ShadowLight.toArgb())
                            maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawPath(path.asAndroidPath(), lightPaint)
                    }

                    // 3. Molded Canvas Base Fill
                    drawPath(path = path, color = NeumorphicTokens.Surface)
                }
        )

        // B. Floating Active Circular Icon Bubble
        if (containerWidthPx > 0 && selectedIndex in items.indices) {
            val activeItem = items[selectedIndex]
            val bubbleSize = 54.dp
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
                        surfaceColor = NeumorphicTokens.Surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = activeItem.icon,
                        contentDescription = activeItem.title,
                        tint = NeumorphicTokens.AccentSalmon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    // Illuminated Glowing Pip / Dot
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(NeumorphicTokens.AccentSalmon)
                    )
                }
            }
        }

        // C. Interactive Tab Triggers Row
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Render inactive icon only when not selected (since active icon floats above in bubble)
                        if (!isSelected) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        } else {
                            // Blank spacer to preserve vertical alignment of tab title when active
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Text(
                            text = item.title,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMuted
                        )
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
