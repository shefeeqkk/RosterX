package com.rosterx.presentation.portal.more

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.presentation.components.UserAvatar
import com.rosterx.presentation.components.toProperCase
import com.rosterx.ui.neumorphic.*

@Composable
fun MyTeamView(
    engineers: List<Engineer>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val activeEngineers = remember(engineers) {
        engineers.filter {
            it.status != EngineerStatus.RESIGNED &&
                    it.role in listOf(EngineerRole.MANAGER, EngineerRole.ENGINEER, EngineerRole.APPRENTICE) &&
                    !it.username.equals("admin", ignoreCase = true)
        }
    }

    val filteredList = remember(activeEngineers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) activeEngineers
        else activeEngineers.filter { eng ->
            eng.name.contains(q, ignoreCase = true) ||
                    eng.fullName.contains(q, ignoreCase = true) ||
                    eng.role.name.contains(q, ignoreCase = true) ||
                    eng.phone.contains(q, ignoreCase = true)
        }
    }

    val managers = remember(filteredList) {
        filteredList.filter { it.role == EngineerRole.MANAGER }.sortedBy { it.name.lowercase() }
    }

    val shiftTeam = remember(filteredList) {
        filteredList.filter { it.role == EngineerRole.ENGINEER || it.role == EngineerRole.APPRENTICE }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicTokens.Canvas)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeumorphicTokens.Surface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeumorphicTokens.TextMain
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "My Team Directory",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeumorphicTokens.TextMain
                )
                Text(
                    text = "${activeEngineers.size} Active Team Members",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted
                )
            }
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search teammates by name, role or phone...",
                        fontSize = 13.sp,
                        color = NeumorphicTokens.TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeumorphicTokens.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeumorphicTokens.Surface,
                    unfocusedContainerColor = NeumorphicTokens.Surface,
                    focusedBorderColor = NeumorphicTokens.AccentSalmon,
                    unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Team Directory List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Managers
            if (managers.isNotEmpty()) {
                item {
                    DirectorySectionHeader(
                        title = "Operations Management",
                        count = managers.size,
                        badgeColor = Color(0xFF6366F1)
                    )
                }

                items(managers, key = { "mgr_${it.id}" }) { manager ->
                    TeamMemberCard(
                        engineer = manager,
                        roleDisplayName = "Operations Manager",
                        roleBadgeColor = Color(0xFF6366F1)
                    )
                }
            }

            // Section 2: Shift Engineers & Apprentices
            if (shiftTeam.isNotEmpty()) {
                item {
                    DirectorySectionHeader(
                        title = "Shift Engineers & Operations",
                        count = shiftTeam.size,
                        badgeColor = NeumorphicTokens.AccentSalmon
                    )
                }

                items(shiftTeam, key = { "team_${it.id}" }) { member ->
                    val roleTitle = if (member.role == EngineerRole.APPRENTICE) "Apprentice Engineer" else "Shift Engineer"
                    val badgeColor = if (member.role == EngineerRole.APPRENTICE) Color(0xFFF59E0B) else NeumorphicTokens.AccentSalmon

                    TeamMemberCard(
                        engineer = member,
                        roleDisplayName = roleTitle,
                        roleBadgeColor = badgeColor
                    )
                }
            }

            if (managers.isEmpty() && shiftTeam.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No team members found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMain
                            )
                            Text(
                                text = "Try searching with a different name or role",
                                fontSize = 12.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DirectorySectionHeader(
    title: String,
    count: Int,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NeumorphicTokens.TextMain
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = badgeColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = "$count",
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TeamMemberCard(
    engineer: Engineer,
    roleDisplayName: String,
    roleBadgeColor: Color
) {
    val context = LocalContext.current
    val rawPhone = engineer.phone.trim()
    val hasPhone = rawPhone.isNotBlank()

    // Normalizers
    val cleanDialPhone = remember(rawPhone) {
        rawPhone.replace(Regex("[^0-9+]"), "")
    }

    val whatsappPhone = remember(rawPhone) {
        val digitsOnly = rawPhone.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length == 10) "91$digitsOnly" else digitsOnly
    }

    val properName = remember(engineer.name, engineer.fullName) {
        engineer.fullName.ifBlank { engineer.name }.toProperCase()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                shape = RoundedCornerShape(18.dp),
                elevation = NeumorphicTokens.ElevationRaisedSubtle
            )
            .background(NeumorphicTokens.Surface, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            UserAvatar(
                name = properName,
                profilePicture = engineer.profilePicture,
                size = 46.dp,
                backgroundColor = roleBadgeColor.copy(alpha = 0.2f),
                textColor = roleBadgeColor,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = properName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTokens.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleBadgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, roleBadgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = roleDisplayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (hasPhone) {
                    Text(
                        text = rawPhone,
                        fontSize = 12.sp,
                        color = NeumorphicTokens.TextMuted,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "No direct phone",
                        fontSize = 11.sp,
                        color = NeumorphicTokens.TextMuted.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons: Call & WhatsApp
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasPhone) Color(0xFF059669).copy(alpha = 0.12f)
                            else NeumorphicTokens.TextMuted.copy(alpha = 0.08f)
                        )
                        .clickable {
                            if (!hasPhone || cleanDialPhone.isBlank()) {
                                Toast.makeText(context, "No phone number available for $properName", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanDialPhone"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch phone dialer", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (hasPhone) Color(0xFF059669) else NeumorphicTokens.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // WhatsApp Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasPhone) Color(0xFF25D366).copy(alpha = 0.15f)
                            else NeumorphicTokens.TextMuted.copy(alpha = 0.08f)
                        )
                        .clickable {
                            if (!hasPhone || whatsappPhone.isBlank()) {
                                Toast.makeText(context, "No phone number available for WhatsApp", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsappPhone"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = if (hasPhone) Color(0xFF25D366) else NeumorphicTokens.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
