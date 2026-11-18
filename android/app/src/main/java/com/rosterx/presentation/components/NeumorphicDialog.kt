package com.rosterx.presentation.components

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicFloating
import com.rosterx.ui.theme.LocalIsDarkTheme

/**
 * Applies a modern hardware-accelerated frosted blur to the dialog's backdrop window
 * with a luminous, light tint (dimAmount = 0.45f) with fallback for OEM firmware.
 */
@Composable
fun ApplyDialogBlurBehind(
    blurRadiusPx: Int = 60,
    dimAmount: Float = 0.45f
) {
    val view = LocalView.current
    val dialogWindow = remember(view) {
        var parent: Any? = view.parent
        var foundWindow: Window? = null
        while (parent != null) {
            if (parent is DialogWindowProvider) {
                foundWindow = parent.window
                break
            }
            parent = (parent as? View)?.parent
        }
        foundWindow
            ?: (view.rootView as? DialogWindowProvider)?.window
            ?: (view.context as? Activity)?.window
    }

    SideEffect {
        dialogWindow?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = blurRadiusPx
                    }
                } catch (_: Exception) {}
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
        val isDark = LocalIsDarkTheme.current
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0x990A0F1A) else Color(0xB8F1F5F9))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = if (isLandscape) 10.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .then(if (isLandscape) Modifier.widthIn(max = 540.dp) else Modifier)
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .neumorphicFloating(shape = NeumorphicTokens.ShapeContainer)
                    .background(NeumorphicTokens.Surface, NeumorphicTokens.ShapeContainer)
                    .padding(contentPadding),
                content = content
            )
        }
    }
}
