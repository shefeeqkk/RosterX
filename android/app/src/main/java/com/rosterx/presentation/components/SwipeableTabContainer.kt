package com.rosterx.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * RosterX — Ultra-Smooth Live 1:1 Horizontal Tab Pager with Synchronized Boundary Spring Recoil
 *
 * - Native 1:1 direct finger tracking powered by Compose HorizontalPager (beyondBoundsPageCount = 1).
 * - Zero touch-event fighting: HorizontalPager is the sole horizontal drag gesture handler.
 * - Hardware-accelerated boundary overscroll resistance via graphicsLayer + NestedScrollConnection.
 * - Bouncy spring recoil upon finger lift.
 * - Fully uninhibited swipe across blank areas, with strict table grid isolation delegated to child.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealtimeSwipeableTabPager(
    pagerState: PagerState,
    isSuspended: Boolean,
    overscrollAnim: Animatable<Float, AnimationVector1D>,
    onBoundaryOverscroll: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    isRosterTab: Boolean = false,
    content: @Composable (page: Int) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val maxOverscrollPx = with(density) { 65.dp.toPx() }

    // NestedScrollConnection captures unconsumed horizontal delta ONLY when pager is at bounds
    val boundaryNestedScrollConnection = remember(pagerState, isSuspended) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isSuspended) return Offset.Zero

                val isAtStart = pagerState.currentPage == 0 && available.x > 0f
                val isAtEnd = pagerState.currentPage == (pagerState.pageCount - 1) && available.x < 0f

                if (isAtStart || isAtEnd) {
                    val currentVal = overscrollAnim.value
                    val delta = available.x * 0.28f
                    val newOverscroll = currentVal + delta
                    val clamped = sign(newOverscroll) * abs(newOverscroll).coerceAtMost(maxOverscrollPx)
                    coroutineScope.launch {
                        overscrollAnim.snapTo(clamped)
                        onBoundaryOverscroll(clamped)
                    }
                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollAnim.value != 0f) {
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
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val currentViewConfig = LocalViewConfiguration.current
    val customViewConfig = remember(currentViewConfig, density) {
        object : ViewConfiguration by currentViewConfig {
            // Calibrated 8dp touch slop eliminates starting dead-zones while filtering micro-finger diagonal drag
            override val touchSlop: Float
                get() = with(density) { 8.dp.toPx() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = overscrollAnim.value
            }
            .nestedScroll(boundaryNestedScrollConnection)
    ) {
        CompositionLocalProvider(LocalViewConfiguration provides customViewConfig) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSuspended,
                beyondBoundsPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Restore standard ViewConfiguration for page child contents (e.g. vertical LazyColumns)
                CompositionLocalProvider(LocalViewConfiguration provides currentViewConfig) {
                    content(page)
                }
            }
        }
    }
}
