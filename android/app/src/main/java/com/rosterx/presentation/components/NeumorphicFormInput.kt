package com.rosterx.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.theme.LocalIsDarkTheme

/**
 * Reusable Compact Neumorphic Form Input Field
 * Features a clean top label and a sunken / inset well for compact dual & single column layouts.
 */
@Composable
fun NeumorphicFormInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
    onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) Color(0xFFEF4444) else NeumorphicTokens.TextMuted,
                letterSpacing = 0.2.sp
            )
        }

        var isFocused by remember { mutableStateOf(false) }
        val isDark = LocalIsDarkTheme.current
        val borderColor = when {
            isError -> Color(0xFFEF4444)
            isFocused -> NeumorphicTokens.Accent.copy(alpha = 0.85f)
            else -> if (isDark) Color(0x1FFFFFFF) else Color(0x1F94A3B8)
        }

        var boxModifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(shape = shape, elevation = NeumorphicTokens.ElevationInsetSubtle)
            .border(width = 1.dp, color = borderColor, shape = shape)

        if (onClick != null) {
            boxModifier = boxModifier.clickable(onClick = onClick)
        }

        Box(
            modifier = boxModifier.padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = NeumorphicTokens.TextMuted.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    var textModifier = Modifier.fillMaxWidth()
                    if (focusRequester != null) {
                        textModifier = textModifier.focusRequester(focusRequester)
                    }
                    if (onGloballyPositioned != null) {
                        textModifier = textModifier.onGloballyPositioned(onGloballyPositioned)
                    }
                    textModifier = textModifier.onFocusChanged { state ->
                        isFocused = state.isFocused
                        onFocusChanged?.invoke(state)
                    }

                    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = textModifier,
                        enabled = enabled && onClick == null,
                        readOnly = readOnly || onClick != null,
                        singleLine = singleLine,
                        textStyle = TextStyle(
                            color = if (!enabled) NeumorphicTokens.TextMuted else if (isError) Color(0xFFEF4444) else NeumorphicTokens.TextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(NeumorphicTokens.AccentSalmon),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = resolvedInteractionSource
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingIcon()
                }
            }
        }

        if (errorMessage != null && isError) {
            Text(
                text = errorMessage,
                fontSize = 10.5.sp,
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp)
            )
        } else if (supportingText != null) {
            supportingText()
        }
    }
}
