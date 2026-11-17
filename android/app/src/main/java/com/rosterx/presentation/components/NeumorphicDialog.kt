package com.rosterx.presentation.components

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicFloating

/**
 * Applies a modern hardware-accelerated frosted blur to the dialog's backdrop window
 * with a luminous, light tint (dimAmount = 0.15f) instead of heavy dark dimming.
 */
@Composable
fun ApplyDialogBlurBehind(
    blurRadiusPx: Int = 45,
    dimAmount: Float = 0.15f
) {
    val view = LocalView.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        ?: (view.rootView as? DialogWindowProvider)?.window
        ?: (view.context as? Activity)?.window

    SideEffect {
        dialogWindow?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = blurRadiusPx
                }
            }
            window.setDimAmount(dimAmount)
        }
    }
}

/**
 * High-Floating Neumorphic Dialog wrapper with soft ambient backdrop.
 */
@Composable
fun NeumorphicDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    contentPadding: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ApplyDialogBlurBehind()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .neumorphicFloating(shape = NeumorphicTokens.ShapeContainer)
                    .padding(contentPadding),
                content = content
            )
        }
    }
}
