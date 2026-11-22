package com.rosterx.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp

/**
 * RosterX — Ultra-Smooth Live 1:1 Horizontal Tab Pager
 *
 * - Native 1:1 direct finger tracking powered by Compose HorizontalPager (beyondBoundsPageCount = 1).
 * - Zero touch-event fighting: HorizontalPager is the sole horizontal drag gesture handler.
 * - Uniform 5dp touch slop (~1mm) across all pages for instant, buttery WhatsApp-grade swipe initiation.
 * - Clean boundary handling: zero dead-zone or lag on boundary screens (Dashboard Tab 0 and More Tab 4).
 * - Pure fractional bottom nav tracking with zero artificial boundary distortion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealtimeSwipeableTabPager(
    pagerState: PagerState,
    isSuspended: Boolean,
    modifier: Modifier = Modifier,
    overscrollAnim: Animatable<Float, AnimationVector1D>? = null,
    onBoundaryOverscroll: (Float) -> Unit = {},
    isRosterTab: Boolean = false,
    content: @Composable (page: Int) -> Unit
) {
    val density = LocalDensity.current
    val currentViewConfig = LocalViewConfiguration.current
    val customViewConfig = remember(currentViewConfig, density) {
        object : ViewConfiguration by currentViewConfig {
            // Ultra-responsive 5dp touch slop (~1mm) for instant WhatsApp-tier 1:1 finger tracking
            override val touchSlop: Float
                get() = with(density) { 5.dp.toPx() }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(LocalViewConfiguration provides customViewConfig) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSuspended,
                beyondBoundsPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                content(page)
            }
        }
    }
}
