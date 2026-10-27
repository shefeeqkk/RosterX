package com.rosterx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicAccentGlow
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicRaised

enum class NeumorphicBadgeStyle {
    Raised,
    Inset,
    Accent
}

/**
 * Molded or Inset Neumorphic Badge / Chip for shift tags, status pills, and counters.
 */
@Composable
fun NeumorphicBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: NeumorphicBadgeStyle = NeumorphicBadgeStyle.Raised,
    shape: Shape = NeumorphicTokens.ShapeSmall,
    textColor: Color = NeumorphicTokens.TextMain,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 5.dp
) {
    val styleModifier = when (style) {
        NeumorphicBadgeStyle.Raised -> Modifier.neumorphicRaised(
            shape = shape,
            elevation = NeumorphicTokens.ElevationRaisedSubtle
        )
        NeumorphicBadgeStyle.Inset -> Modifier.neumorphicInset(
            shape = shape,
            elevation = NeumorphicTokens.ElevationInsetSubtle
        )
        NeumorphicBadgeStyle.Accent -> Modifier
            .neumorphicAccentGlow(radius = 8.dp)
            .clip(shape)
            .background(NeumorphicTokens.AccentGradient)
    }

    Box(
        modifier = modifier
            .then(styleModifier)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (style == NeumorphicBadgeStyle.Accent) Color.White else textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
