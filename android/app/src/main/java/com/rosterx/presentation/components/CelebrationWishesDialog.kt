package com.rosterx.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class CelebrationType {
    BIRTHDAY,
    ANNIVERSARY,
    BOTH
}

@Composable
fun CelebrationWishesDialog(
    show: Boolean,
    celebrationType: CelebrationType,
    userName: String,
    anniversaryYears: Int = 1,
    onDismiss: () -> Unit
) {
    if (!show) return

    // Pulse & wobble animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_pulse")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble_rotation"
    )

    fun ordinal(n: Int): String {
        return "$n" + if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    val properName = userName.toProperCase().ifBlank { "Teammate" }

    val (badgeColors, mainIcon, titleText, subtitleText, wishBody) = when (celebrationType) {
        CelebrationType.BIRTHDAY -> {
            val colors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6))
            Tuple5(
                colors,
                Icons.Default.Cake,
                "Happy Birthday, $properName! 🎂",
                "CELEBRATING YOUR SPECIAL DAY 🎉",
                "On this wonderful day, the entire team at RosterX wishes you joy, vibrant health, and extraordinary success! Thank you for bringing your energy, brilliance, and positive spirit to our operations every single day. Have a fabulous birthday celebration! 🎈🥳✨"
            )
        }
        CelebrationType.ANNIVERSARY -> {
            val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B))
            val yrStr = ordinal(anniversaryYears)
            Tuple5(
                colors,
                Icons.Default.EmojiEvents,
                "Happy $yrStr Work Anniversary! 🏆",
                "MILESTONE ACHIEVED 🌟",
                "Congratulations $properName on completing $yrStr with our team! Your unwavering dedication, operational excellence, and teamwork continue to make a lasting impact. Here's to honoring your journey and achieving many more stellar milestones together! 🥂🎊"
            )
        }
        CelebrationType.BOTH -> {
            val colors = listOf(Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF10B981))
            val yrStr = ordinal(anniversaryYears)
            Tuple5(
                colors,
                Icons.Default.Celebration,
                "Double Celebration: Birthday & $yrStr Anniversary! 🎂🏆",
                "A DOUBLE MILESTONE TO CHERISH 🌟",
                "Today is doubly special! We celebrate both your Birthday and your $yrStr Work Anniversary at RosterX! Thank you $properName for being a pillar of our operations. May this year bring you abundant happiness, fulfillment, and great shifts ahead! 🎈🥂🎉"
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.linearGradient(badgeColors)
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                // Animated Hero Icon Badge
                Box(
                    modifier = Modifier
                        .scale(scaleAnim)
                        .rotate(rotateAnim)
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(badgeColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = mainIcon,
                            contentDescription = null,
                            tint = badgeColors[0],
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Subtitle Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColors[0].copy(alpha = 0.12f)
                ) {
                    Text(
                        text = subtitleText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColors[0],
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Main Title
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                // Personalized Wish Body
                Text(
                    text = wishBody,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Celebratory Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = badgeColors[0]
                    )
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thank You! 🙏",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
