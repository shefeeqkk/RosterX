package com.rosterx.presentation.portal.credentials

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.data.remote.CredentialTrackerLogDto
import com.rosterx.ui.neumorphic.NeumorphicTokens

/**
 * RosterX — Unified Credential Tracker Audit Logs Sheet
 * 
 * Provides an authoritative, chronologically ordered audit trail of all credential
 * tracker additions, updates, renewals, and deletions across all servers and accounts.
 * Strictly gated to Admin and Manager roles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialTrackerLogsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    logs: List<CredentialTrackerLogDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    if (!isOpen) return

    val configuration = LocalConfiguration.current
    val sheetHeight = (configuration.screenHeightDp.dp - 80.dp).coerceAtLeast(420.dp)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, UPDATED, CREATED, DELETED

    // Filter logs by search query and action category
    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "UPDATED" -> log.action.equals("UPDATE", ignoreCase = true) || log.action.equals("RENEW", ignoreCase = true)
                "CREATED" -> log.action.equals("CREATE", ignoreCase = true)
                "DELETED" -> log.action.equals("DELETE", ignoreCase = true)
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                log.serverName.contains(searchQuery, ignoreCase = true) ||
                log.username.contains(searchQuery, ignoreCase = true) ||
                log.updatedBy.contains(searchQuery, ignoreCase = true) ||
                log.updatedByName.contains(searchQuery, ignoreCase = true) ||
                log.summary.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val updatedCount = remember(logs) {
        logs.count { it.action.equals("UPDATE", ignoreCase = true) || it.action.equals("RENEW", ignoreCase = true) }
    }
    val createdCount = remember(logs) {
        logs.count { it.action.equals("CREATE", ignoreCase = true) }
    }
    val deletedCount = remember(logs) {
        logs.count { it.action.equals("DELETE", ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeumorphicTokens.Surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeumorphicTokens.SurfaceContrast.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Audit History",
                            tint = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Credential Audit Logs",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "Authoritative history across all servers & accounts",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeumorphicTokens.AccentSalmon
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Logs",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search server, account, updater...",
                        fontSize = 13.sp,
                        color = NeumorphicTokens.TextMuted.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeumorphicTokens.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp),
                                tint = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeumorphicTokens.AccentSalmon,
                    unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.25f),
                    focusedContainerColor = NeumorphicTokens.Canvas,
                    unfocusedContainerColor = NeumorphicTokens.Canvas
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Action Filter Chips Row (Unified 'UPDATED' eliminating redundancy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogFilterChip(
                    label = "All (${logs.size})",
                    isSelected = selectedFilter == "ALL",
                    selectedColor = NeumorphicTokens.AccentSalmon,
                    onClick = { selectedFilter = "ALL" }
                )
                LogFilterChip(
                    label = "Updated ($updatedCount)",
                    isSelected = selectedFilter == "UPDATED",
                    selectedColor = Color(0xFF6366F1), // Indigo
                    onClick = { selectedFilter = "UPDATED" }
                )
                LogFilterChip(
                    label = "Created ($createdCount)",
                    isSelected = selectedFilter == "CREATED",
                    selectedColor = Color(0xFF10B981), // Emerald
                    onClick = { selectedFilter = "CREATED" }
                )
                LogFilterChip(
                    label = "Deleted ($deletedCount)",
                    isSelected = selectedFilter == "DELETED",
                    selectedColor = Color(0xFFEF4444), // Rose
                    onClick = { selectedFilter = "DELETED" }
                )
            }

            // Logs Content List / Empty State
            if (isLoading && logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Loading credential audit history...",
                            fontSize = 13.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
            } else if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NeumorphicTokens.Canvas,
                        border = BorderStroke(1.dp, NeumorphicTokens.Border.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NeumorphicTokens.TextMuted.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") "No matching audit logs" else "No audit logs recorded yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMain
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") "Try clearing your search query or selecting 'All'." else "Operations will be automatically recorded here.",
                                fontSize = 12.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filteredLogs, key = { it.id.ifEmpty { "${it.trackerId}_${it.timestamp}" } }) { log ->
                        CredentialLogItemCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFilterChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else NeumorphicTokens.Canvas,
        border = BorderStroke(
            1.dp,
            if (isSelected) selectedColor.copy(alpha = 0.5f) else NeumorphicTokens.TextMuted.copy(alpha = 0.15f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) selectedColor else NeumorphicTokens.TextMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CredentialLogItemCard(log: CredentialTrackerLogDto) {
    val isUpdated = log.action.equals("UPDATE", ignoreCase = true) || log.action.equals("RENEW", ignoreCase = true)
    val isCreated = log.action.equals("CREATE", ignoreCase = true)
    val isDeleted = log.action.equals("DELETE", ignoreCase = true)

    val actionColor = when {
        isUpdated -> Color(0xFF6366F1) // Indigo
        isCreated -> Color(0xFF10B981) // Emerald
        isDeleted -> Color(0xFFEF4444) // Rose
        else -> NeumorphicTokens.AccentSalmon
    }

    val actionIcon = when {
        isUpdated -> Icons.Default.Autorenew
        isCreated -> Icons.Default.AddCircleOutline
        isDeleted -> Icons.Default.DeleteOutline
        else -> Icons.Default.History
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NeumorphicTokens.Canvas,
        border = BorderStroke(1.dp, NeumorphicTokens.Border.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Server & Account Badge + Action Badge Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Server & Account Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, NeumorphicTokens.Border.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = log.serverName.ifEmpty { "Server" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                        Text(
                            text = log.username.ifEmpty { "account" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.AccentSalmon
                        )
                    }
                }

                // Action Badge Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = actionColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, actionColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = log.actionLabel.ifEmpty { log.action },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                    }
                }
            }

            // Row 2: Date Transition Diff (if available)
            if (!log.previousDate.isNullOrBlank() && !log.newDate.isNullOrBlank() && log.previousDate != log.newDate) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeumorphicTokens.Surface,
                    border = BorderStroke(0.5.dp, NeumorphicTokens.Border.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = log.previousDate,
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = NeumorphicTokens.TextMuted
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Changed to",
                            tint = actionColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = log.newDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                        if (!log.expiryDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Expires ${log.expiryDate}",
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                }
            } else if (!log.newDate.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = NeumorphicTokens.TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Password Date: ${log.newDate}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeumorphicTokens.TextMain
                    )
                    if (!log.expiryDate.isNullOrBlank()) {
                        Text(
                            text = "• Expires: ${log.expiryDate}",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
            }

            // Row 3: Human readable summary prose
            if (log.summary.isNotBlank()) {
                Text(
                    text = log.summary,
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMain.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(
                color = NeumorphicTokens.Border.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            // Row 4: Footer (2-Level Smart Row: Updater Attribution & Un-squeezable Timestamp Pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Who updated: Person icon + 2-Level Column (Full Name on top, @username below)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NeumorphicTokens.SurfaceContrast.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    val updaterName = log.updatedByName.ifEmpty { log.updatedBy.ifEmpty { "System" } }
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = updaterName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (log.updatedBy.isNotBlank() && log.updatedBy != updaterName) {
                            Text(
                                text = "@${log.updatedBy}",
                                fontSize = 10.sp,
                                color = NeumorphicTokens.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Dedicated Un-squeezable Timestamp Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, NeumorphicTokens.Border.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = log.datetimeStr.ifEmpty { "Recently" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicTokens.TextMuted,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
