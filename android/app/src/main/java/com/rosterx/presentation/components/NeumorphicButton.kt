package com.rosterx.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

enum class NeumorphicButtonVariant {
    Surface,
    PrimaryAccent
}

/**
 * Tactile Neumorphic Button with smooth kinetic physics and press animation.
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeumorphicButtonVariant = NeumorphicButtonVariant.Surface,
    shape: Shape = NeumorphicTokens.ShapeMedium,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: Dp = 12.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseModifier = when (variant) {
        NeumorphicButtonVariant.Surface -> {
            if (isPressed) {
                Modifier.neumorphicInset(shape = shape, elevation = NeumorphicTokens.ElevationInsetSubtle)
            } else {
                Modifier.neumorphicRaised(shape = shape, elevation = NeumorphicTokens.ElevationRaised)
            }
        }
        NeumorphicButtonVariant.PrimaryAccent -> {
            val glowModifier = if (!isPressed && enabled) Modifier.neumorphicAccentGlow() else Modifier
            glowModifier
                .clip(shape)
                .background(NeumorphicTokens.AccentGradient)
                .then(
                    if (isPressed) {
                        Modifier.neumorphicInset(
                            shape = shape,
                            elevation = NeumorphicTokens.ElevationInsetSubtle,
                            surfaceColor = Color.Transparent
                        )
                    } else {
                        Modifier
                    }
                )
        }
    }

    Box(
        modifier = modifier
            .then(baseModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (variant == NeumorphicButtonVariant.PrimaryAccent) Color.White else NeumorphicTokens.TextMain,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
