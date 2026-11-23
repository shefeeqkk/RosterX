package com.rosterx.presentation.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicRaised
import com.rosterx.ui.theme.LocalIsDarkTheme
import kotlin.math.abs
import kotlin.math.roundToInt

data class NavTabItem(
    val index: Int,
    val title: String,
    val icon: ImageVector
)

/**
 * RosterX — Ultra-Smooth Fluid Bottom Navigation Bar (WhatsApp & Instagram Reverse-Engineered)
 *
 * Performance Architecture:
 * 1. Static Neumorphic Dock Surface: Pre-rendered hardware-accelerated elevated surface (no dynamic bezier recalculation).
 * 2. GPU-Accelerated Floating Bubble: Hardware layer translationX on RenderNode (zero CPU/recomposition cycles).
 * 3. 120fps Native Shadowing: Uses Android HWUI native RenderNode elevation shadows instead of CPU BlurMaskFilter.
 * 4. Continuous GPU Alpha Blending: Icon and label cross-fades evaluated directly on the GPU compositor.
 */
@Composable
fun FluidNeumorphicBottomNav(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fractionalPosition: () -> Float = { selectedIndex.toFloat() },
    boundaryOverscrollOffsetPx: () -> Float = { 0f },
    isDragging: Boolean = false
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val itemCount = items.size.coerceAtLeast(1)
    val tabWidthPx = if (containerWidthPx > 0) containerWidthPx / itemCount else 0f

    val surfaceColor = NeumorphicTokens.Surface
    val textMutedColor = NeumorphicTokens.TextMuted
    val shadowDarkColor = NeumorphicTokens.ShadowDark
    val accentColor = NeumorphicTokens.Accent

    val isDark = LocalIsDarkTheme.current
    val navBarInsets = WindowInsets.navigationBars
    val bottomInsetDp = with(density) { navBarInsets.getBottom(density).toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp + bottomInsetDp)
            .padding(bottom = bottomInsetDp)
            .onGloballyPositioned { coordinates ->
                containerWidthPx = coordinates.size.width.toFloat()
            }
    ) {
        // 1. Ambient Upward Drop Shadow (Differs and separates the navigation dock from the screen content)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) Color(0x88000000) else Color(0x380F172A)
                        )
                    )
                )
        )

        // A. Static Elegant Neumorphic Dock Surface (100% GPU RenderNode shadow, zero redraw on swipe)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0xFF0F172A).copy(alpha = 0.35f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF0F172A).copy(alpha = 0.18f)
                )
                .background(
                    color = surfaceColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        )

        // B. Floating Active Circular Bubble (Hardware-accelerated translationX on GPU layer)
        if (containerWidthPx > 0 && items.isNotEmpty()) {
            val bubbleSize = 54.dp
            val bubbleSizePx = with(density) { bubbleSize.toPx() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 2.dp)
                    .graphicsLayer {
                        val pos = fractionalPosition()
                        val overscroll = boundaryOverscrollOffsetPx()
                        val tabCenterX = if (tabWidthPx > 0) {
                            (pos * tabWidthPx) + (tabWidthPx / 2f) + (overscroll * 0.16f)
                        } else 0f
                        translationX = tabCenterX - (bubbleSizePx / 2f)
                    }
                    .size(bubbleSize)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = accentColor.copy(alpha = 0.45f),
                        ambientColor = shadowDarkColor.copy(alpha = 0.20f)
                    )
                    .background(
                        color = accentColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Stack tab icons and smoothly blend alpha on GPU RenderNode based on continuous distance
                items.forEach { item ->
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val pos = fractionalPosition()
                                val dist = abs(pos - item.index)
                                alpha = (1f - dist).coerceIn(0f, 1f)
                            }
                    )
                }
            }
        }

        // C. Interactive Inactive Tab Triggers (Smooth opacity cross-fade, zero layout jumping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "tabItemScale_${item.index}"
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
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = textMutedColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    val dist = abs(fractionalPosition() - item.index)
                                    alpha = dist.coerceIn(0f, 1f)
                                }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.title,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = textMutedColor,
                            modifier = Modifier.graphicsLayer {
                                val dist = abs(fractionalPosition() - item.index)
                                alpha = if (dist < 0.5f) 1.0f else 0.72f
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility overloads accepting Float and lambda variants.
 */
@Composable
fun FluidNeumorphicBottomNav(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fractionalPosition: () -> Float,
    boundaryOverscrollOffsetPx: Float,
    isDragging: Boolean = false
) = FluidNeumorphicBottomNav(
    items = items,
    selectedIndex = selectedIndex,
    onTabSelected = onTabSelected,
    modifier = modifier,
    fractionalPosition = fractionalPosition,
    boundaryOverscrollOffsetPx = { boundaryOverscrollOffsetPx },
    isDragging = isDragging
)

@Composable
fun FluidNeumorphicBottomNav(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fractionalPosition: Float,
    boundaryOverscrollOffsetPx: Float = 0f,
    isDragging: Boolean = false
) = FluidNeumorphicBottomNav(
    items = items,
    selectedIndex = selectedIndex,
    onTabSelected = onTabSelected,
    modifier = modifier,
    fractionalPosition = { fractionalPosition },
    boundaryOverscrollOffsetPx = { boundaryOverscrollOffsetPx },
    isDragging = isDragging
)

/**
 * RosterX — Compact Vertical Fluid Neumorphic Navigation Rail for Landscape Mode.
 * Anchored on the LEFT side with display-cutout / gesture-bar safe insets,
 * preserving RosterX's tactile Neumorphic raised active pill & spring animation
 * while reclaiming full vertical viewport height.
 */
@Composable
fun FluidNeumorphicNavRail(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val safeStartDp = with(density) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Start).getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr).toDp()
    }
    val safeBottomDp = with(density) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).getBottom(density).toDp()
    }
    val railWidth = 72.dp
    val surfaceColor = NeumorphicTokens.Surface
    val shadowDarkColor = NeumorphicTokens.ShadowDark
    val textMainColor = NeumorphicTokens.TextMain
    val textMutedColor = NeumorphicTokens.TextMuted
    val accentColor = NeumorphicTokens.Accent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth + safeStartDp)
            .drawBehind {
                val cornerPx = 22.dp.toPx()
                val shadowBlur = 12.dp.toPx()
                val shadowOffsetX = 4.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width - cornerPx, 0f)
                    quadraticBezierTo(size.width, 0f, size.width, cornerPx)
                    lineTo(size.width, size.height - cornerPx)
                    quadraticBezierTo(size.width, size.height, size.width - cornerPx, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawIntoCanvas { canvas ->
                    val darkPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = shadowDarkColor.copy(alpha = 0.24f).toArgb()
                        maskFilter = BlurMaskFilter(shadowBlur, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(shadowOffsetX, 0f)
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), darkPaint)
                    canvas.nativeCanvas.restore()

                    val fillPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = surfaceColor.toArgb()
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), fillPaint)
                }
            }
            .padding(
                start = safeStartDp + 4.dp,
                end = 6.dp,
                top = 6.dp,
                bottom = (safeBottomDp + 6.dp).coerceAtLeast(6.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val isSelected = item.index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val iconScale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.90f
                        isSelected -> 1.08f
                        else -> 1.0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "railItemScale_${item.index}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(item.index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(iconScale)
                            .size(if (isSelected) 38.dp else 32.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .neumorphicRaised(
                                            shape = CircleShape,
                                            elevation = NeumorphicTokens.ElevationRaisedSubtle,
                                            surfaceColor = surfaceColor
                                        )
                                        .clip(CircleShape)
                                        .background(surfaceColor)
                                } else {
                                    Modifier.clip(CircleShape)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) accentColor else textMutedColor,
                            modifier = Modifier.size(if (isSelected) 19.dp else 18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = if (isSelected) 9.sp else 8.5.sp,
                        lineHeight = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) accentColor else textMainColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
