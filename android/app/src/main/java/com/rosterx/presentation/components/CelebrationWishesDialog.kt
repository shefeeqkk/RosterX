package com.rosterx.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rosterx.R

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
    isSelf: Boolean = true,
    celebrantPhone: String? = null,
    onDismiss: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current

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
    val yrStr = ordinal(anniversaryYears)
    val yrLabel = if (anniversaryYears == 1) "1 year" else "$anniversaryYears years"

    val (badgeColors, mainIcon, titleText, subtitleText, wishBody) = when (celebrationType) {
        CelebrationType.BIRTHDAY -> {
            val colors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6))
            val sub = if (isSelf) "CELEBRATING YOUR SPECIAL DAY 🎉" else "TEAM MEMBER BIRTHDAY 🎂"
            val title = "Happy Birthday, $properName! 🎂"
            val body = if (isSelf) {
                "On this wonderful day, the entire team at RosterX wishes you joy, vibrant health, and extraordinary success! Thank you for bringing your energy, brilliance, and positive spirit to our operations every single day. Have a fabulous birthday celebration! 🎈🥳✨"
            } else {
                "Today is $properName's birthday! Join the entire RosterX team in wishing them immense happiness, vibrant health, and continued success! 🎈🥳✨"
            }
            Tuple5(colors, Icons.Default.Cake, title, sub, body)
        }
        CelebrationType.ANNIVERSARY -> {
            val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B))
            val sub = "MILESTONE ACHIEVED 🌟"
            val title = if (isSelf) "Happy $yrStr Work Anniversary! 🏆" else "Happy $yrStr Work Anniversary, $properName! 🏆"
            val body = if (isSelf) {
                "Congratulations $properName on completing $yrStr with our team! Your unwavering dedication, operational excellence, and teamwork continue to make a lasting impact. Here's to honoring your journey and achieving many more stellar milestones together! 🥂🎊"
            } else {
                "Today $properName completes $yrLabel of operational excellence with our team! Let's celebrate their dedication, brilliance, and teamwork! 🥂🎊✨"
            }
            Tuple5(colors, Icons.Default.EmojiEvents, title, sub, body)
        }
        CelebrationType.BOTH -> {
            val colors = listOf(Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF10B981))
            val sub = if (isSelf) "A DOUBLE MILESTONE TO CHERISH 🌟" else "DOUBLE MILESTONE 🌟"
            val title = if (isSelf) "Double Celebration: Birthday & $yrStr Anniversary! 🎂🏆" else "Double Celebration for $properName! 🎂🏆"
            val body = if (isSelf) {
                "Today is doubly special! We celebrate both your Birthday and your $yrStr Work Anniversary at RosterX! Thank you $properName for being a pillar of our operations. May this year bring you abundant happiness, fulfillment, and great shifts ahead! 🎈🥂🎉"
            } else {
                "Today is doubly special for $properName celebrating their birthday and $yrLabel with RosterX! Send your warmest wishes! 🎈🥂🎉"
            }
            Tuple5(colors, Icons.Default.Celebration, title, sub, body)
        }
    }

    val rawPhone = celebrantPhone ?: ""
    val cleanDigits = rawPhone.filter { it.isDigit() }
    val cleanDialPhone = if (rawPhone.startsWith("+")) "+$cleanDigits" else cleanDigits
    val hasPhone = cleanDigits.length >= 10

    val whatsappPhone = when {
        cleanDigits.length == 10 -> "91$cleanDigits"
        cleanDigits.startsWith("91") && cleanDigits.length == 12 -> cleanDigits
        cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "91" + cleanDigits.substring(1)
        else -> cleanDigits
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

                if (isSelf) {
                    // Celebratory Action Button for Self
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
                } else {
                    // Teammate Actions: Side-by-side WhatsApp & Call, plus Close
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Wish on WhatsApp" Button
                            Button(
                                onClick = {
                                    val greeting = when (celebrationType) {
                                        CelebrationType.ANNIVERSARY -> "Happy $yrStr Work Anniversary, $properName! 🏆✨ Congratulations on completing $yrLabel with our team!"
                                        CelebrationType.BIRTHDAY -> "Happy Birthday, $properName! 🎉🎂 Wishing you a fantastic year ahead filled with joy and success!"
                                        CelebrationType.BOTH -> "Happy Birthday and Happy $yrStr Work Anniversary, $properName! 🎂🏆 Wishing you the very best!"
                                    }
                                    val encodedMsg = Uri.encode(greeting)
                                    val uri = if (whatsappPhone.isNotBlank()) {
                                        Uri.parse("https://wa.me/$whatsappPhone?text=$encodedMsg")
                                    } else {
                                        Uri.parse("https://wa.me/?text=$encodedMsg")
                                    }
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366)
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_whatsapp),
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wish on WhatsApp",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Compact Call Button (if phone exists)
                            if (hasPhone && cleanDialPhone.isNotBlank()) {
                                FilledIconButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanDialPhone")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Close Button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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