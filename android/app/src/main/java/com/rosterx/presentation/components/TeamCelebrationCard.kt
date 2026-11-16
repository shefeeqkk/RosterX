package com.rosterx.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.R
import com.rosterx.data.remote.TodayCelebrationDto
import com.rosterx.ui.neumorphic.*

/**
 * Festive Neumorphic TeamCelebrationCard
 * Displays today's birthday or work anniversary for colleagues with one-tap
 * WhatsApp wishing intent and phone call launcher.
 */
@Composable
fun TeamCelebrationCard(
    celebration: TodayCelebrationDto,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (badgeText, badgeIcon, badgeTint, badgeBg) = when (celebration.type) {
        "both" -> Quad(
            "🎉 Double Celebration Today!",
            Icons.Default.Celebration,
            Color(0xFFB45309),
            Color(0xFFFEF3C7)
        )
        "birthday" -> Quad(
            "🎂 Birthday Today!",
            Icons.Default.Cake,
            Color(0xFFBE123C),
            Color(0xFFFFE4E6)
        )
        else -> Quad(
            if (celebration.years > 0) "🏆 ${celebration.years} Years Work Anniversary!" else "🏆 Work Anniversary!",
            Icons.Default.EmojiEvents,
            Color(0xFFB45309),
            Color(0xFFFEF3C7)
        )
    }

    val rawPhone = celebration.phone ?: ""
    val cleanDigits = rawPhone.filter { it.isDigit() }
    val cleanDialPhone = if (rawPhone.startsWith("+")) "+$cleanDigits" else cleanDigits
    val hasPhone = cleanDigits.length >= 10

    val whatsappPhone = when {
        cleanDigits.length == 10 -> "91$cleanDigits"
        cleanDigits.startsWith("91") && cleanDigits.length == 12 -> cleanDigits
        cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "91" + cleanDigits.substring(1)
        else -> cleanDigits
    }

    NeumorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Badge & Dismiss 'X'
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTint
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = NeumorphicTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Colleague Profile & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UserAvatar(
                    name = celebration.name,
                    profilePicture = celebration.photo,
                    size = 52.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    shape = CircleShape
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = celebration.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (celebration.greetingText.isNotBlank()) celebration.greetingText
                        else "Join the team in wishing them a wonderful day!",
                        fontSize = 12.sp,
                        color = NeumorphicTokens.TextMuted,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons Row: WhatsApp & Direct Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Wishing Action
                Button(
                    onClick = {
                        val defaultGreeting = when (celebration.type) {
                            "both" -> "Happy Birthday & Happy ${celebration.years} Year Work Anniversary, ${celebration.name}! 🎉🎂🏆 Wishing you immense happiness and continued success! - From RosterX"
                            "birthday" -> "Happy Birthday, ${celebration.name}! 🎉🎂 Wishing you an amazing year ahead filled with joy and success! - From RosterX"
                            else -> "Happy ${celebration.years} Year Work Anniversary, ${celebration.name}! 🏆✨ Congratulations on this milestone and thank you for being an amazing colleague! - From RosterX"
                        }
                        val greeting = if (celebration.greetingText.isNotBlank()) celebration.greetingText else defaultGreeting
                        val encodedMsg = Uri.encode(greeting)
                        val uri = if (whatsappPhone.isNotBlank()) {
                            Uri.parse("https://wa.me/$whatsappPhone?text=$encodedMsg")
                        } else {
                            Uri.parse("https://wa.me/?text=$encodedMsg")
                        }
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not launch WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wish on WhatsApp",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Phone Call Action (Only if valid phone number exists)
                if (hasPhone && cleanDialPhone.isNotBlank()) {
                    FilledIconButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanDialPhone")))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Container Section supporting single or multiple celebrating colleagues.
 * For multiple celebrations, displays a swipeable horizontal carousel.
 */
@Composable
fun TeamCelebrationsSection(
    celebrations: List<TodayCelebrationDto>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (celebrations.isEmpty()) return

    if (celebrations.size == 1) {
        val single = celebrations.first()
        TeamCelebrationCard(
            celebration = single,
            onDismiss = { onDismiss(single.username) },
            modifier = modifier
        )
    } else {
        // Multi-celebration horizontal carousel
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            celebrations.forEach { celeb ->
                TeamCelebrationCard(
                    celebration = celeb,
                    onDismiss = { onDismiss(celeb.username) },
                    modifier = Modifier.width(320.dp)
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
