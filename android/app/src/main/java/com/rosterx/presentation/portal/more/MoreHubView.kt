package com.rosterx.presentation.portal.more

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.presentation.portal.PortalState
import com.rosterx.ui.neumorphic.*

@Composable
fun MoreHubView(
    state: PortalState,
    onNavigateToCredentialTracker: () -> Unit,
    isAdmin: Boolean = false,
    onNavigateToServerManagement: (() -> Unit)? = null,
    onNavigateToMyTeam: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicTokens.Canvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hub Header Banner
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = "Operations Hub",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeumorphicTokens.TextMain
            )
            Text(
                text = if (isAdmin) "Server controls & infrastructure credentials" else "Extended duty utilities & automated server monitors",
                fontSize = 13.sp,
                color = NeumorphicTokens.TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Feature 1: Server Credential Tracker Card (Primary Action)
        MoreHubFeatureCard(
            title = "Server Credential Tracker",
            description = "Monitor root & service account password validity with automated 9:00 AM IST expiry alerts.",
            icon = Icons.Default.VpnKey,
            iconTint = NeumorphicTokens.AccentSalmon,
            badge = {
                val expired = state.credentialsExpiredCount
                val expiringSoon = state.credentialsExpiringSoonCount
                val total = state.credentialsTotalCount

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expired > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$expired Expired",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (expiringSoon > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$expiringSoon Expiring Soon",
                                color = Color(0xFFD97706),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (expired == 0 && expiringSoon == 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (total > 0) "$total Active Trackers" else "All Healthy",
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            },
            onClick = onNavigateToCredentialTracker
        )

        // Feature 2: Admin -> Server Management Card
        if (isAdmin && onNavigateToServerManagement != null) {
            MoreHubFeatureCard(
                title = "Server Management",
                description = "Configure backend server URL, inspect MongoDB Atlas diagnostics, and review live server logs.",
                icon = Icons.Default.Dns,
                iconTint = Color(0xFF3B82F6),
                badge = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Diagnostics & Logs",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                },
                enabled = true,
                onClick = onNavigateToServerManagement
            )
        }

        // Feature 2: Non-Admin -> My Team Directory Card
        if (!isAdmin && onNavigateToMyTeam != null) {
            val activeMembersCount = remember(state.engineers) {
                state.engineers.count { it.status != EngineerStatus.RESIGNED }
            }

            MoreHubFeatureCard(
                title = "My Team",
                description = "Team directory, hierarchy, and direct contact options.",
                icon = Icons.Default.Groups,
                iconTint = Color(0xFF6366F1),
                badge = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = if (activeMembersCount > 0) "$activeMembersCount Active Members" else "Team Directory",
                            color = Color(0xFF4F46E5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                },
                enabled = true,
                onClick = onNavigateToMyTeam
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MoreHubFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    badge: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                shape = RoundedCornerShape(20.dp),
                elevation = if (enabled) NeumorphicTokens.ElevationRaised else NeumorphicTokens.ElevationRaisedSubtle
            )
            .background(NeumorphicTokens.Surface, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) NeumorphicTokens.TextMain else NeumorphicTokens.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = NeumorphicTokens.TextMuted,
                        lineHeight = 16.sp
                    )
                }

                if (enabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = NeumorphicTokens.TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                badge()
            }
        }
    }
}
