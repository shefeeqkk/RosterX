package com.rosterx.presentation.portal.credentials

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    var trackerToDelete by remember { mutableStateOf<CredentialTrackerDto?>(null) }
    var trackerToPickDateFor by remember { mutableStateOf<CredentialTrackerDto?>(null) }
    var pendingRenewalConfirmation by remember { mutableStateOf<Pair<CredentialTrackerDto, String>?>(null) }

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

                // Status Filter Chips Row with Live Counter for Healthy
                val healthyCount = state.credentialTrackers.count { it.status == "HEALTHY" }
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
                        label = "Healthy ($healthyCount)",
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
                // Server-Grouped Compact Tracker List (Always expanded static sections as requested)
                val groupedTrackers = remember(filteredTrackers) {
                    filteredTrackers.groupBy { it.serverName }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    groupedTrackers.forEach { (serverName, trackersForServer) ->
                        item(key = serverName) {
                            ServerGroupCard(
                                serverName = serverName,
                                trackers = trackersForServer,
                                isManagerOrAdmin = isManagerOrAdmin,
                                currentUserEmail = currentUserEmail,
                                currentUsername = currentUsername,
                                onUpdateDateClick = { tracker ->
                                    trackerToPickDateFor = tracker
                                },
                                onEditClick = { tracker ->
                                    trackerToEdit = tracker
                                    showAddEditDialog = true
                                },
                                onDeleteClick = { tracker ->
                                    trackerToDelete = tracker
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Material3 DatePickerDialog for Updating Rotation Date
    if (trackerToPickDateFor != null) {
        val tracker = trackerToPickDateFor!!
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Restrict renewal dates to today or past dates (no future dates)
                    return utcTimeMillis <= System.currentTimeMillis() + 86400000L
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { trackerToPickDateFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()

                        val today = LocalDate.now()
                        if (selectedDate.isAfter(today)) {
                            Toast.makeText(context, "Renewal date cannot be in the future", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        val selectedDateStr = selectedDate.toString()
                        trackerToPickDateFor = null
                        pendingRenewalConfirmation = Pair(tracker, selectedDateStr)
                    }
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold, color = NeumorphicTokens.AccentSalmon)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val todayStr = LocalDate.now().toString()
                            trackerToPickDateFor = null
                            pendingRenewalConfirmation = Pair(tracker, todayStr)
                        }
                    ) {
                        Text("Today", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { trackerToPickDateFor = null }) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Rotate '${tracker.serverName}' (${tracker.username})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NeumorphicTokens.TextMain
                    )
                }
            )
        }
    }

    // Quick Confirmation Dialog (Option A2 from Pre-Execution Grill)
    if (pendingRenewalConfirmation != null) {
        val (tracker, selectedDateStr) = pendingRenewalConfirmation!!
        val parsedDate = try { LocalDate.parse(selectedDateStr) } catch (_: Throwable) { LocalDate.now() }
        val newExpiry = parsedDate.plusDays(tracker.validityDays.toLong()).toString()
        val daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), parsedDate.plusDays(tracker.validityDays.toLong()))

        AlertDialog(
            onDismissRequest = { pendingRenewalConfirmation = null },
            icon = { Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF059669)) },
            title = { Text("Confirm Credential Rotation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Update credential rotation date for server '${tracker.serverName}' (${tracker.username})?",
                        fontSize = 13.sp,
                        color = NeumorphicTokens.TextMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeumorphicTokens.Canvas,
                        border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New Rotation Date:", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                                Text(selectedDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMain)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Validity Cycle:", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                                Text("${tracker.validityDays} Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMain)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calculated Expiry:", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                                Text(newExpiry, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (daysUntilExpiry > 5) Color(0xFF059669) else Color(0xFFD97706))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tId = tracker.id
                        pendingRenewalConfirmation = null
                        viewModel.renewCredentialTracker(
                            id = tId,
                            lastUpdatedDate = selectedDateStr,
                            onSuccess = {
                                Toast.makeText(context, "Rotated '${tracker.serverName}' credentials to $selectedDateStr!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Confirm & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRenewalConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditCredentialTrackerDialog(
            existingTracker = trackerToEdit,
            allTrackers = state.credentialTrackers,
            rosterEngineers = state.engineers,
            isManagerOrAdmin = isManagerOrAdmin,
            onDismiss = { showAddEditDialog = false },
            onSave = { serverName, username, lastUpdatedDate, validityDays, spokespersons, alertThresholdDays ->
                if (trackerToEdit == null) {
                    viewModel.createCredentialTracker(
                        serverName = serverName,
                        username = username,
                        lastUpdatedDate = lastUpdatedDate,
                        validityDays = validityDays,
                        spokespersonEmails = spokespersons,
                        alertThresholdDays = alertThresholdDays,
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
                        alertThresholdDays = alertThresholdDays,
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
                                Toast.makeText(context, "Deleted tracker for ${tracker.serverName}", Toast.LENGTH_SHORT).show()
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

/**
 * Server-Grouped Compact Card (Always Expanded static section)
 */
@Composable
private fun ServerGroupCard(
    serverName: String,
    trackers: List<CredentialTrackerDto>,
    isManagerOrAdmin: Boolean,
    currentUserEmail: String,
    currentUsername: String,
    onUpdateDateClick: (CredentialTrackerDto) -> Unit,
    onEditClick: (CredentialTrackerDto) -> Unit,
    onDeleteClick: (CredentialTrackerDto) -> Unit
) {
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
            // Server Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = serverName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                }

                // Account count badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeumorphicTokens.Canvas,
                    border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${trackers.size} Account${if (trackers.size > 1) "s" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(
                color = NeumorphicTokens.TextMuted.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // High-density Accounts List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                trackers.forEachIndexed { index, tracker ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = NeumorphicTokens.TextMuted.copy(alpha = 0.06f),
                            thickness = 1.dp
                        )
                    }

                    val isSpokesperson = tracker.spokespersonEmails.any {
                        val lower = it.trim().lowercase()
                        lower == currentUserEmail || lower == currentUsername
                    }
                    val canRenew = isManagerOrAdmin || isSpokesperson

                    val statusColor = when (tracker.status) {
                        "EXPIRED" -> Color(0xFFDC2626)
                        "EXPIRES_TODAY" -> Color(0xFFDC2626)
                        "EXPIRING_SOON" -> Color(0xFFD97706)
                        else -> Color(0xFF059669)
                    }

                    val statusText = when (tracker.status) {
                        "EXPIRED" -> "Expired ${-tracker.daysRemaining}d ago"
                        "EXPIRES_TODAY" -> "Expires Today!"
                        "EXPIRING_SOON" -> "Expires in ${tracker.daysRemaining}d"
                        else -> "${tracker.daysRemaining}d remaining"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Username with User Icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tracker.username,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.TextMain
                                )
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }

                        // Dates Line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rotated: ${tracker.lastUpdatedDate} (${tracker.validityDays}d cycle)",
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                            Text(
                                text = "Expires: ${tracker.expiryDate}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }

                        // Designated Spokesperson Emails (Shown ONLY if isManagerOrAdmin == true)
                        if (isManagerOrAdmin && tracker.spokespersonEmails.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Alerts:",
                                    fontSize = 10.sp,
                                    color = NeumorphicTokens.TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                tracker.spokespersonEmails.forEach { email ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeumorphicTokens.Canvas,
                                        border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = email,
                                            fontSize = 10.sp,
                                            color = NeumorphicTokens.TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Actions Row for this account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canRenew) {
                                Box(
                                    modifier = Modifier
                                        .neumorphicRaised(
                                            shape = RoundedCornerShape(20.dp),
                                            elevation = NeumorphicTokens.ElevationRaisedSubtle
                                        )
                                        .background(NeumorphicTokens.Surface, RoundedCornerShape(20.dp))
                                        .border(
                                            BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onUpdateDateClick(tracker) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditCalendar,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Update Date",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            if (isManagerOrAdmin) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onEditClick(tracker) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = NeumorphicTokens.TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteClick(tracker) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCredentialTrackerDialog(
    existingTracker: CredentialTrackerDto?,
    allTrackers: List<CredentialTrackerDto>,
    rosterEngineers: List<com.rosterx.domain.model.Engineer>,
    isManagerOrAdmin: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (serverName: String, username: String, lastUpdatedDate: String, validityDays: Int, spokespersons: List<String>, alertThresholdDays: Int?) -> Unit
) {
    var serverName by remember { mutableStateOf(existingTracker?.serverName ?: "") }
    var username by remember { mutableStateOf(existingTracker?.username ?: "") }
    var lastUpdatedDate by remember { mutableStateOf(existingTracker?.lastUpdatedDate ?: LocalDate.now().toString()) }
    var validityDays by remember { mutableIntStateOf(existingTracker?.validityDays ?: 30) }
    var spokespersons by remember { mutableStateOf(existingTracker?.spokespersonEmails ?: emptyList()) }
    var customEmailInput by remember { mutableStateOf("") }
    var customValidityInput by remember { mutableStateOf(existingTracker?.validityDays?.toString() ?: "30") }
    var alertThresholdInput by remember { mutableStateOf((existingTracker?.alertThresholdDays ?: 5).toString()) }

    var showDatePicker by remember { mutableStateOf(false) }

    // Autocomplete distinct servers & usernames from existing trackers
    val existingServerNames = remember(allTrackers) {
        allTrackers.map { it.serverName.trim() }.filter { it.isNotBlank() }.distinct()
    }
    val existingUsernames = remember(allTrackers) {
        allTrackers.map { it.username.trim() }.filter { it.isNotBlank() }.distinct()
    }

    // Matching suggestions
    val matchingServers = remember(serverName, existingServerNames) {
        if (serverName.isBlank()) emptyList()
        else existingServerNames.filter { it.contains(serverName, ignoreCase = true) && !it.equals(serverName, ignoreCase = true) }.take(3)
    }
    val matchingUsernames = remember(username, existingUsernames) {
        if (username.isBlank()) emptyList()
        else existingUsernames.filter { it.contains(username, ignoreCase = true) && !it.equals(username, ignoreCase = true) }.take(3)
    }

    // Available team roster suggestions
    val availableEngineers = remember(rosterEngineers, spokespersons) {
        rosterEngineers
            .filter {
                val emailLower = it.email.trim().lowercase()
                emailLower.isNotBlank() && emailLower !in spokespersons.map { s -> s.trim().lowercase() }
            }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis() + 86400000L
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        lastUpdatedDate = selected.toString()
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, color = NeumorphicTokens.AccentSalmon)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            lastUpdatedDate = LocalDate.now().toString()
                            showDatePicker = false
                        }
                    ) {
                        Text("Today", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Rotation Date",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NeumorphicTokens.TextMain
                    )
                }
            )
        }
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
                // Server Name with Auto-Suggest Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server / Service Name") },
                        placeholder = { Text("e.g. prod-db-01, jump-host-02") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (matchingServers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Suggested:", fontSize = 10.sp, color = NeumorphicTokens.TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                            matchingServers.forEach { sug ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, NeumorphicTokens.AccentSalmon.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable { serverName = sug }
                                ) {
                                    Text(
                                        text = sug,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTokens.AccentSalmon,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Username / Account with Auto-Suggest Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Account Name") },
                        placeholder = { Text("e.g. root, svc_analytics, admin") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (matchingUsernames.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Suggested:", fontSize = 10.sp, color = NeumorphicTokens.TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                            matchingUsernames.forEach { sug ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, NeumorphicTokens.AccentSalmon.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable { username = sug }
                                ) {
                                    Text(
                                        text = sug,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTokens.AccentSalmon,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Last Rotated Date (DatePicker field + Today quick button)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Last Rotated Date:", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeumorphicTokens.Canvas,
                            border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Date",
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lastUpdatedDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.TextMain
                                )
                            }
                        }

                        Button(
                            onClick = { lastUpdatedDate = LocalDate.now().toString() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669).copy(alpha = 0.15f))
                        ) {
                            Text("Today", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Validity Cycle (Days) - Clean Manual Number Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Validity Cycle (Days):", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                    OutlinedTextField(
                        value = customValidityInput,
                        onValueChange = {
                            customValidityInput = it
                            val parsed = it.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                validityDays = parsed
                            }
                        },
                        placeholder = { Text("e.g. 30, 45, 60, 90") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Manager/Admin Alert Threshold Setting
                if (isManagerOrAdmin) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Alert Threshold (Days before expiry):", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)
                        OutlinedTextField(
                            value = alertThresholdInput,
                            onValueChange = { alertThresholdInput = it },
                            placeholder = { Text("e.g. 5") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val expiryLocalDate = remember(lastUpdatedDate, validityDays) {
                            try {
                                LocalDate.parse(lastUpdatedDate).plusDays(validityDays.toLong())
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val thresholdVal = alertThresholdInput.toIntOrNull() ?: 5
                        val alarmStartDate = expiryLocalDate?.minusDays(thresholdVal.toLong())

                        if (alarmStartDate != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Alarming begins on: $alarmStartDate ($thresholdVal days before expiry)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }

                // Designated Spokespersons (Email Alerts)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Designated Spokespersons (9 AM Alerts):", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)

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

                    // Teammates Dropdown / Quick Add from Roster
                    if (availableEngineers.isNotEmpty()) {
                        Text("Add Teammate as Spokesperson:", fontSize = 10.sp, color = NeumorphicTokens.TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableEngineers.take(8).forEach { eng ->
                                val engEmail = eng.email.trim().lowercase()
                                val engName = eng.name.ifBlank { eng.username }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeumorphicTokens.Surface,
                                    border = BorderStroke(1.dp, NeumorphicTokens.TextMuted.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        if (engEmail !in spokespersons) {
                                            spokespersons = spokespersons + engEmail
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
                                            text = engName,
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
                            placeholder = { Text("Add custom external email...", fontSize = 12.sp) },
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
                    val alertDays = if (isManagerOrAdmin) alertThresholdInput.toIntOrNull() ?: 5 else null
                    onSave(serverName, username, lastUpdatedDate, validityDays, spokespersons, alertDays)
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
