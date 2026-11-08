package com.rosterx.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min

/**
 * Global In-Memory LRU Cache for decoded Base64 Avatar Bitmaps to avoid
 * redundant CPU work across fast-scrolling LazyColumns and LazyGrids.
 */
private val avatarBitmapCache = object : LruCache<String, Bitmap>(100) {}

fun decodeBase64ToBitmap(dataUriOrBase64: String?): Bitmap? {
    if (dataUriOrBase64.isNullOrBlank()) return null
    val clean = dataUriOrBase64.trim()
    val cached = avatarBitmapCache.get(clean)
    if (cached != null) return cached

    val base64Data = if (clean.contains("base64,")) {
        clean.substringAfter("base64,")
    } else {
        clean
    }

    return try {
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        if (bitmap != null) {
            avatarBitmapCache.put(clean, bitmap)
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * Standard utility to read, center-crop to square, scale down, and compress
 * an image selected from the Android gallery into a lightweight WebP/JPEG Base64 data URI.
 */
fun compressImageUriToBase64(
    context: Context,
    imageUri: Uri,
    maxDimension: Int = 256,
    quality: Int = 85
): String? {
    var inputStream: InputStream? = null
    return try {
        inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

        // Center-crop to 1:1 square
        val width = originalBitmap.width
        val height = originalBitmap.height
        val minDim = min(width, height)
        val left = (width - minDim) / 2
        val top = (height - minDim) / 2

        val squareBitmap = Bitmap.createBitmap(originalBitmap, left, top, minDim, minDim)

        // Downscale to standard avatar dimension if larger
        val scaledBitmap = if (minDim > maxDimension) {
            Bitmap.createScaledBitmap(squareBitmap, maxDimension, maxDimension, true)
        } else {
            squareBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
        null
    } finally {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
    }
}

fun extractInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "U"
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        (parts.first().take(1) + parts.last().take(1)).uppercase()
    } else {
        clean.take(2).uppercase()
    }
}

@Composable
fun UserAvatar(
    name: String,
    profilePicture: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    textColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: androidx.compose.ui.unit.TextUnit = (size.value * 0.38f).sp,
    onClick: (() -> Unit)? = null
) {
    val bitmap = remember(profilePicture) {
        decodeBase64ToBitmap(profilePicture)
    }

    var boxModifier = modifier
        .size(size)
        .clip(shape)

    if (border != null) {
        boxModifier = boxModifier.border(border, shape)
    }

    if (onClick != null) {
        boxModifier = boxModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = boxModifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = extractInitials(name),
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = textColor
            )
        }
    }
}
