package com.rosterx.presentation.portal.credentials

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.data.remote.CredentialTrackerDto
import com.rosterx.domain.model.EngineerRole
import com.rosterx.presentation.portal.EngineerPortalViewModel
import com.rosterx.presentation.portal.PortalState
import com.rosterx.ui.neumorphic.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialTrackerScreen(
    state: PortalState,
    viewModel: EngineerPortalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, EXPIRING, EXPIRED, HEALTHY

    var showAddEditDialog by remember { mutableStateOf(false) }
    var trackerToEdit by remember { mutableStateOf<CredentialTrackerDto?>(null) }
    var trackerToRenew by remember { mutableStateOf<CredentialTrackerDto?>(null) }
    var trackerToDelete by remember { mutableStateOf<CredentialTrackerDto?>(null) }

    val isManagerOrAdmin = state.loggedInEngineer?.role == EngineerRole.ADMIN ||
            state.loggedInEngineer?.role == EngineerRole.MANAGER

    val currentUserEmail = state.loggedInEngineer?.email?.trim()?.lowercase() ?: ""
    val currentUsername = state.loggedInEngineer?.username?.trim()?.lowercase() ?: ""

    // Filter trackers
    val filteredTrackers = remember(state.credentialTrackers, searchQuery, selectedFilter) {
        state.credentialTrackers.filter { tracker ->
            val matchesQuery = searchQuery.isBlank() ||
                    tracker.serverName.contains(searchQuery, ignoreCase = true) ||
                    tracker.username.contains(searchQuery, ignoreCase = true) ||
                    tracker.spokespersonEmails.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                "EXPIRING" -> tracker.status in listOf("EXPIRING_SOON", "EXPIRES_TODAY")
                "EXPIRED" -> tracker.status == "EXPIRED"
                "HEALTHY" -> tracker.status == "HEALTHY"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = NeumorphicTokens.Canvas,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeumorphicTokens.AccentSalmon
                        )
                    }
                },
                title = {
                    Text(
                        text = "Credential Tracker",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = NeumorphicTokens.AccentSalmon
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCredentialTrackers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = NeumorphicTokens.TextMuted
                        )
                    }
                    if (isManagerOrAdmin) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeumorphicTokens.AccentSalmon.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    trackerToEdit = null
                                    showAddEditDialog = true
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Tracker",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.AccentSalmon
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeumorphicTokens.Canvas,
                    titleContentColor = NeumorphicTokens.TextMain
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(NeumorphicTokens.Canvas)
        ) {
            // KPI Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerKpiCard(
                    title = "Total",
                    count = state.credentialsTotalCount,
                    accentColor = NeumorphicTokens.TextMuted,
                    modifier = Modifier.weight(1f)
                )
                TrackerKpiCard(
                    title = "Expiring Soon",
                    count = state.credentialsExpiringSoonCount,
                    accentColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1.3f)
                )
                TrackerKpiCard(
                    title = "Expired",
                    count = state.credentialsExpiredCount,
                    accentColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by server, username, or email...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeumorphicTokens.AccentSalmon,
                        unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.25f),
                        focusedContainerColor = NeumorphicTokens.Surface,
                        unfocusedContainerColor = NeumorphicTokens.Surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        label = "All (${state.credentialsTotalCount})",
                        isSelected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    FilterChipItem(
                        label = "Expiring Soon (${state.credentialsExpiringSoonCount})",
                        isSelected = selectedFilter == "EXPIRING",
                        selectedColor = Color(0xFFD97706),
                        onClick = { selectedFilter = "EXPIRING" }
                    )
                    FilterChipItem(
                        label = "Expired (${state.credentialsExpiredCount})",
                        isSelected = selectedFilter == "EXPIRED",
                        selectedColor = Color(0xFFDC2626),
                        onClick = { selectedFilter = "EXPIRED" }
                    )
                    FilterChipItem(
                        label = "Healthy",
                        isSelected = selectedFilter == "HEALTHY",
                        selectedColor = Color(0xFF059669),
                        onClick = { selectedFilter = "HEALTHY" }
                    )
                }
            }

            // Trackers List or Loading / Empty state
            if (state.isLoadingCredentials && state.credentialTrackers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeumorphicTokens.AccentSalmon)
                }
            } else if (filteredTrackers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = NeumorphicTokens.AccentSalmon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") "No matching credentials" else "No Credential Trackers Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = if (isManagerOrAdmin) "Track server passwords, expiry dates, and automated email alerts." else "Tracked server credentials will appear here.",
                            fontSize = 12.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                        if (isManagerOrAdmin && searchQuery.isBlank() && selectedFilter == "ALL") {
                            Button(
                                onClick = {
                                    trackerToEdit = null
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.AccentSalmon)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Tracker", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrackers, key = { it.id }) { tracker ->
                        val isSpokesperson = tracker.spokespersonEmails.any {
                            val lower = it.trim().lowercase()
                            lower == currentUserEmail || lower == currentUsername
                        }
                        val canRenew = isManagerOrAdmin || isSpokesperson

                        CredentialTrackerCard(
                            tracker = tracker,
                            canRenew = canRenew,
                            canManage = isManagerOrAdmin,
                            onRenewClick = { trackerToRenew = tracker },
                            onEditClick = {
                                trackerToEdit = tracker
                                showAddEditDialog = true
                            },
                            onDeleteClick = { trackerToDelete = tracker }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditCredentialTrackerDialog(
            existingTracker = trackerToEdit,
            rosterEngineers = state.engineers,
            onDismiss = { showAddEditDialog = false },
            onSave = { serverName, username, lastUpdatedDate, validityDays, spokespersons ->
                if (trackerToEdit == null) {
                    viewModel.createCredentialTracker(
                        serverName = serverName,
                        username = username,
                        lastUpdatedDate = lastUpdatedDate,
                        validityDays = validityDays,
                        spokespersonEmails = spokespersons,
                        onSuccess = {
                            showAddEditDialog = false
                            Toast.makeText(context, "Tracker added for $serverName", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    viewModel.updateCredentialTracker(
                        id = trackerToEdit!!.id,
                        serverName = serverName,
                        username = username,
                        lastUpdatedDate = lastUpdatedDate,
                        validityDays = validityDays,
                        spokespersonEmails = spokespersons,
                        onSuccess = {
                            showAddEditDialog = false
                            Toast.makeText(context, "Tracker updated for $serverName", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // 1-Tap Quick Renew Confirmation Dialog
    if (trackerToRenew != null) {
        val tracker = trackerToRenew!!
        AlertDialog(
            onDismissRequest = { trackerToRenew = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669)) },
            title = { Text("Confirm Credential Renewal", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Have you updated/rotated the credentials for '${tracker.serverName}' (${tracker.username})?\n\n" +
                            "This will reset the last updated date to today (${LocalDate.now()}) and extend validity by ${tracker.validityDays} days.",
                    fontSize = 13.sp,
                    color = NeumorphicTokens.TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tId = tracker.id
                        trackerToRenew = null
                        viewModel.renewCredentialTracker(
                            id = tId,
                            onSuccess = {
                                Toast.makeText(context, "Renewed '${tracker.serverName}' credentials to today!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Yes, Renewed Today", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackerToRenew = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (trackerToDelete != null) {
        val tracker = trackerToDelete!!
        AlertDialog(
            onDismissRequest = { trackerToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Delete Credential Tracker", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to stop tracking credentials for '${tracker.serverName}' (${tracker.username})?\n\nAutomated expiry email alerts will be disabled for this server.",
                    fontSize = 13.sp,
                    color = NeumorphicTokens.TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tId = tracker.id
                        trackerToDelete = null
                        viewModel.deleteCredentialTracker(
                            id = tId,
                            onSuccess = {
                                Toast.makeText(context, "Tracker removed", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TrackerKpiCard(
    title: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neumorphicRaised(
                shape = RoundedCornerShape(16.dp),
                elevation = NeumorphicTokens.ElevationRaised
            )
            .background(NeumorphicTokens.Surface, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphicTokens.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    selectedColor: Color = NeumorphicTokens.AccentSalmon,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else NeumorphicTokens.Surface,
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
private fun CredentialTrackerCard(
    tracker: CredentialTrackerDto,
    canRenew: Boolean,
    canManage: Boolean,
    onRenewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusColor = when (tracker.status) {
        "EXPIRED" -> Color(0xFFDC2626)
        "EXPIRES_TODAY" -> Color(0xFFDC2626)
        "EXPIRING_SOON" -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }

    val statusText = when (tracker.status) {
        "EXPIRED" -> "Expired ${-tracker.daysRemaining}d ago"
        "EXPIRES_TODAY" -> "Expires Today!"
        "EXPIRING_SOON" -> "Expires in ${tracker.daysRemaining} days"
        else -> "${tracker.daysRemaining} days remaining"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                shape = RoundedCornerShape(20.dp),
                elevation = NeumorphicTokens.ElevationRaised
            )
            .background(NeumorphicTokens.Surface, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Server & Username + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tracker.serverName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tracker.username,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }

                // Expiry Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(
                color = NeumorphicTokens.TextMuted.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Timeline & Dates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Last Rotated", fontSize = 10.sp, color = NeumorphicTokens.TextMuted)
                    Text(tracker.lastUpdatedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMain)
                }
                Column {
                    Text("Validity Cycle", fontSize = 10.sp, color = NeumorphicTokens.TextMuted)
                    Text("${tracker.validityDays} Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMain)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expiry Date", fontSize = 10.sp, color = NeumorphicTokens.TextMuted)
                    Text(tracker.expiryDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            // Spokesperson Email Chips
            if (tracker.spokespersonEmails.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Designated Spokespersons (Email Alerts):", fontSize = 10.sp, color = NeumorphicTokens.TextMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tracker.spokespersonEmails.forEach { email ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeumorphicTokens.Canvas,
                                border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MailOutline,
                                        contentDescription = null,
                                        tint = NeumorphicTokens.TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = email,
                                        fontSize = 11.sp,
                                        color = NeumorphicTokens.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Tap Quick Renewal Button
                if (canRenew) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.35f)),
                        modifier = Modifier.clickable(onClick = onRenewClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Renewed Today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Edit & Delete Icons (Managers & Admins only)
                if (canManage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCredentialTrackerDialog(
    existingTracker: CredentialTrackerDto?,
    rosterEngineers: List<com.rosterx.domain.model.Engineer>,
    onDismiss: () -> Unit,
    onSave: (serverName: String, username: String, lastUpdatedDate: String, validityDays: Int, spokespersons: List<String>) -> Unit
) {
    var serverName by remember { mutableStateOf(existingTracker?.serverName ?: "") }
    var username by remember { mutableStateOf(existingTracker?.username ?: "") }
    var lastUpdatedDate by remember { mutableStateOf(existingTracker?.lastUpdatedDate ?: LocalDate.now().toString()) }
    var validityDays by remember { mutableIntStateOf(existingTracker?.validityDays ?: 30) }
    var spokespersons by remember { mutableStateOf(existingTracker?.spokespersonEmails ?: emptyList()) }
    var customEmailInput by remember { mutableStateOf("") }
    var customValidityInput by remember { mutableStateOf(existingTracker?.validityDays?.toString() ?: "30") }

    val validityPresets = listOf(30, 60, 90, 180)

    // Filter available team roster suggestions (excluding ones already added)
    val availableTeamEmails = remember(rosterEngineers, spokespersons) {
        rosterEngineers
            .map { it.email.trim().lowercase() }
            .filter { it.isNotBlank() && it !in spokespersons.map { s -> s.trim().lowercase() } }
            .distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingTracker == null) "Add Credential Tracker" else "Edit Credential Tracker",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Server Name
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server / Service Name") },
                    placeholder = { Text("e.g. prod-db-01, root-jumphost") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Username / Account
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Account Name") },
                    placeholder = { Text("e.g. root, svc_analytics, admin") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Last Updated Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lastUpdatedDate,
                        onValueChange = { lastUpdatedDate = it },
                        label = { Text("Last Rotated Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { lastUpdatedDate = LocalDate.now().toString() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.Surface)
                    ) {
                        Text("Today", color = NeumorphicTokens.AccentSalmon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Validity Days Presets + Custom
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Validity Cycle (Days):", fontSize = 12.sp, color = NeumorphicTokens.TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        validityPresets.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (validityDays == preset) NeumorphicTokens.AccentSalmon.copy(alpha = 0.2f) else NeumorphicTokens.Surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (validityDays == preset) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMuted.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        validityDays = preset
                                        customValidityInput = preset.toString()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${preset}d",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (validityDays == preset) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMain
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customValidityInput,
                        onValueChange = {
                            customValidityInput = it
                            val parsed = it.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                validityDays = parsed
                            }
                        },
                        label = { Text("Custom Validity Days") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Designated Spokespersons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Designated Spokespersons (9 AM Alerts):", fontSize = 12.sp, color = NeumorphicTokens.TextMuted)

                    // Selected Spokesperson Chips
                    if (spokespersons.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            spokespersons.forEach { email ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, NeumorphicTokens.AccentSalmon.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = email,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeumorphicTokens.TextMain
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = NeumorphicTokens.TextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    spokespersons = spokespersons.filter { it != email }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Team Roster Quick-Suggest Chips (Confirmed in Q2)
                    if (availableTeamEmails.isNotEmpty()) {
                        Text("Quick Add Teammate:", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTeamEmails.take(6).forEach { teamEmail ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeumorphicTokens.Surface,
                                    border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        if (teamEmail !in spokespersons) {
                                            spokespersons = spokespersons + teamEmail
                                        }
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = NeumorphicTokens.AccentSalmon,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = teamEmail.substringBefore("@"),
                                            fontSize = 11.sp,
                                            color = NeumorphicTokens.TextMain,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom Email Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customEmailInput,
                            onValueChange = { customEmailInput = it },
                            placeholder = { Text("Add custom email...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = customEmailInput.trim().lowercase()
                                if (clean.isNotBlank() && clean !in spokespersons) {
                                    spokespersons = spokespersons + clean
                                    customEmailInput = ""
                                }
                            },
                            enabled = customEmailInput.contains("@"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.AccentSalmon)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(serverName, username, lastUpdatedDate, validityDays, spokespersons)
                },
                enabled = serverName.isNotBlank() && username.isNotBlank() && lastUpdatedDate.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.AccentSalmon)
            ) {
                Text(if (existingTracker == null) "Create Tracker" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
