package com.rosterx.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicAccentGlow
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicRaised

/**
 * Molded Neumorphic Switch with recessed track and tactile sliding thumb.
 */
@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 20.dp
    val padding = 4.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .neumorphicInset(
                shape = NeumorphicTokens.ShapeFull,
                elevation = NeumorphicTokens.ElevationInsetSubtle
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbModifier = if (checked) {
            Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .neumorphicAccentGlow(radius = 8.dp)
                .clip(CircleShape)
                .background(NeumorphicTokens.AccentGradient)
        } else {
            Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .neumorphicRaised(
                    shape = CircleShape,
                    elevation = NeumorphicTokens.ElevationRaisedSubtle
                )
        }

        Box(modifier = thumbModifier)
    }
}
