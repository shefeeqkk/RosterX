package com.rosterx.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
 * RosterX — Live 1:1 Horizontal Tab Pager with Synchronized Edge Spring Recoil (WhatsApp-Style)
 *
 * - Live 1:1 finger tracking between adjacent tabs using HorizontalPager.
 * - Continuous fractional position (`pagerState.currentPage + pagerState.currentPageOffsetFraction`)
 *   for real-time bottom navigation indicator gliding.
 * - Synchronized boundary rubber-band stretch (when dragging past Tab 0 right or Tab 4 left)
 *   with bouncy spring recoil upon release.
 * - Month Roster edge-safe protection (prioritizes 31-day table scrolling unless dragging from screen edge).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealtimeSwipeableTabPager(
    pagerState: PagerState,
    isRosterTab: Boolean,
    isSuspended: Boolean,
    overscrollAnim: Animatable<Float, AnimationVector1D>,
    onBoundaryOverscroll: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (page: Int) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var touchStartX by remember { mutableFloatStateOf(0f) }
    var isEdgeSwipeOnRoster by remember { mutableStateOf(false) }

    val maxOverscrollPx = with(density) { 62.dp.toPx() }
    val edgeZonePx = with(density) { 38.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(x = overscrollAnim.value.roundToInt(), y = 0) }
            .pointerInput(pagerState.currentPage, isRosterTab, isSuspended) {
                if (isSuspended) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        accumulatedDrag = 0f
                        touchStartX = offset.x
                        isEdgeSwipeOnRoster = !isRosterTab || (touchStartX <= edgeZonePx || touchStartX >= size.width - edgeZonePx)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!isEdgeSwipeOnRoster) return@detectHorizontalDragGestures
                        accumulatedDrag += dragAmount

                        val isAtStartBoundary = pagerState.currentPage == 0 && accumulatedDrag > 0f
                        val isAtEndBoundary = pagerState.currentPage == (pagerState.pageCount - 1) && accumulatedDrag < 0f

                        if (isAtStartBoundary || isAtEndBoundary) {
                            val damped = sign(accumulatedDrag) * (kotlin.math.abs(accumulatedDrag) * 0.26f).coerceAtMost(maxOverscrollPx)
                            coroutineScope.launch {
                                overscrollAnim.snapTo(damped)
                                onBoundaryOverscroll(damped)
                            }
                            change.consume()
                        } else {
                            if (overscrollAnim.value != 0f) {
                                coroutineScope.launch {
                                    overscrollAnim.snapTo(0f)
                                    onBoundaryOverscroll(0f)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        accumulatedDrag = 0f
                        if (overscrollAnim.value != 0f) {
                            coroutineScope.launch {
                                overscrollAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onBoundaryOverscroll(value)
                                }
                                onBoundaryOverscroll(0f)
                            }
                        }
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        if (overscrollAnim.value != 0f) {
                            coroutineScope.launch {
                                overscrollAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onBoundaryOverscroll(value)
                                }
                                onBoundaryOverscroll(0f)
                            }
                        }
                    }
                )
            }
    ) {
        val userScroll = if (isSuspended) false else if (isRosterTab) isEdgeSwipeOnRoster else true
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = userScroll,
            beyondBoundsPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            content(page)
        }
    }
}
