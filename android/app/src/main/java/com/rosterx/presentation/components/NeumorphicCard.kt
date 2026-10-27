package com.rosterx.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicFloating
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicRaised

enum class NeumorphicElevationStyle {
    Raised,
    RaisedSubtle,
    Inset,
    Floating
}

/**
 * Reusable Neumorphic Card container molded directly from the background canvas.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = NeumorphicTokens.ShapeContainer,
    style: NeumorphicElevationStyle = NeumorphicElevationStyle.Raised,
    surfaceColor: Color = NeumorphicTokens.Surface,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val elevationModifier = when (style) {
        NeumorphicElevationStyle.Raised -> Modifier.neumorphicRaised(
            shape = shape,
            elevation = NeumorphicTokens.ElevationRaised,
            surfaceColor = surfaceColor
        )
        NeumorphicElevationStyle.RaisedSubtle -> Modifier.neumorphicRaised(
            shape = shape,
            elevation = NeumorphicTokens.ElevationRaisedSubtle,
            surfaceColor = surfaceColor
        )
        NeumorphicElevationStyle.Inset -> Modifier.neumorphicInset(
            shape = shape,
            elevation = NeumorphicTokens.ElevationInset,
            surfaceColor = surfaceColor
        )
        NeumorphicElevationStyle.Floating -> Modifier.neumorphicFloating(
            shape = shape,
            surfaceColor = surfaceColor
        )
    }

    Box(
        modifier = modifier
            .then(elevationModifier)
            .padding(contentPadding),
        content = content
    )
}
