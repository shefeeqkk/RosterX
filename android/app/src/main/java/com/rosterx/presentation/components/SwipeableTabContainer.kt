package com.rosterx.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * RosterX — Edge-Safe Horizontal Touch Swipe Tab Container with Rubber-Band Spring Recoil
 *
 * Provides smooth gesture navigation across the 5 Android tabs:
 * - Swipe left -> Next tab
 * - Swipe right -> Previous tab
 * - Tactile rubber-band overscroll resistance when swiping past Tab 0 (Dashboard) or Last Tab (More)
 * - Bouncy spring recoil (Spring.DampingRatioMediumBouncy, Spring.StiffnessLow) on boundary release
 * - Edge-safe Month Roster protection (prioritizes 31-day table scrolling unless dragging from edge)
 * - Clean gesture suspension when full-screen subviews or modal sheets are active
 */
@Composable
fun SwipeableTabContainer(
    selectedTab: Int,
    totalTabs: Int = 5,
    isRosterTab: Boolean = false,
    isSuspended: Boolean = false,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val overscrollAnim = remember { Animatable(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var touchStartX by remember { mutableFloatStateOf(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var isEdgeSwipeOnRoster by remember { mutableStateOf(false) }

    val maxOverscrollPx = with(density) { 62.dp.toPx() }
    val swipeThresholdPx = with(density) { 52.dp.toPx() }
    val flickDistancePx = with(density) { 28.dp.toPx() }
    val edgeZonePx = with(density) { 40.dp.toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset(x = overscrollAnim.value.roundToInt(), y = 0) }
            .pointerInput(selectedTab, totalTabs, isRosterTab, isSuspended) {
                if (isSuspended) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        accumulatedDrag = 0f
                        touchStartX = offset.x
                        dragStartTime = System.currentTimeMillis()
                        // On Month Roster table, prioritize 31-day table horizontal scrolling unless dragging from screen edge
                        isEdgeSwipeOnRoster = !isRosterTab || (touchStartX <= edgeZonePx || touchStartX >= size.width - edgeZonePx)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!isEdgeSwipeOnRoster) return@detectHorizontalDragGestures
                        accumulatedDrag += dragAmount

                        val isAtStartBoundary = selectedTab == 0 && accumulatedDrag > 0f
                        val isAtEndBoundary = selectedTab == (totalTabs - 1) && accumulatedDrag < 0f

                        if (isAtStartBoundary || isAtEndBoundary) {
                            // Rubber-band resistance decay
                            val damped = sign(accumulatedDrag) * (kotlin.math.abs(accumulatedDrag) * 0.26f).coerceAtMost(maxOverscrollPx)
                            coroutineScope.launch {
                                overscrollAnim.snapTo(damped)
                            }
                            change.consume()
                        } else {
                            if (kotlin.math.abs(accumulatedDrag) > 15f) {
                                change.consume()
                            }
                        }
                    },
                    onDragEnd = {
                        if (!isEdgeSwipeOnRoster) return@detectHorizontalDragGestures

                        if (overscrollAnim.value != 0f) {
                            // Boundary rubber-band spring recoil!
                            coroutineScope.launch {
                                overscrollAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        } else {
                            val duration = (System.currentTimeMillis() - dragStartTime).coerceAtLeast(1L)
                            val velocity = kotlin.math.abs(accumulatedDrag) / duration
                            val isFlick = velocity > 0.40f && kotlin.math.abs(accumulatedDrag) > flickDistancePx

                            // Inner tab swipe transition
                            if ((accumulatedDrag < -swipeThresholdPx || (isFlick && accumulatedDrag < 0f)) && selectedTab < totalTabs - 1) {
                                onTabSelected(selectedTab + 1)
                            } else if ((accumulatedDrag > swipeThresholdPx || (isFlick && accumulatedDrag > 0f)) && selectedTab > 0) {
                                onTabSelected(selectedTab - 1)
                            }
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        if (overscrollAnim.value != 0f) {
                            coroutineScope.launch {
                                overscrollAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                        accumulatedDrag = 0f
                    }
                )
            },
        content = content
    )
}
