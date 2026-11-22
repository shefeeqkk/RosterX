package com.rosterx.presentation.portal.leave

import android.content.res.Configuration
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.data.remote.LeaveRequestDto
import com.rosterx.domain.model.EligibleSwapPartner
import com.rosterx.presentation.components.NeumorphicCard
import com.rosterx.presentation.components.NeumorphicElevationStyle
import com.rosterx.presentation.portal.PortalState
import com.rosterx.ui.neumorphic.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveApprovalsScreen(
    state: PortalState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitAction: (requestId: String, action: String, partner: String, swapCode: String, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onFetchPartnersForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0: Pending, 1: History
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Dialog states
    var swapRequestTarget by remember { mutableStateOf<LeaveRequestDto?>(null) }
    var leaveRequestTarget by remember { mutableStateOf<LeaveRequestDto?>(null) }
    var rejectRequestTarget by remember { mutableStateOf<LeaveRequestDto?>(null) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    // Auto-fetch queue on mount
    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            Surface(
                color = NeumorphicTokens.Canvas,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicCard(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onBack),
                        shape = CircleShape,
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeumorphicTokens.TextMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Leave & Swap Approvals",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "Manager review & 1-click approvals queue",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }

                    // Refresh Button
                    NeumorphicCard(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onRefresh),
                        shape = CircleShape,
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = NeumorphicTokens.TextMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = NeumorphicTokens.Canvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (isLandscape) 16.dp else 14.dp)
        ) {
            // Segmented Tab Controls
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        shape = RoundedCornerShape(24.dp),
                        elevation = NeumorphicTokens.ElevationInsetSubtle
                    )
                    .background(NeumorphicTokens.Surface, RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val pendingCount = state.adminPendingLeaveRequests.size
                val historyCount = state.adminHistoricalLeaveRequests.size

                val tabLabels = listOf(
                    "Pending Queue ($pendingCount)",
                    "Audit History ($historyCount)"
                )

                tabLabels.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .neumorphicRaised(
                                            shape = RoundedCornerShape(20.dp),
                                            elevation = NeumorphicTokens.ElevationRaisedSubtle
                                        )
                                        .background(NeumorphicTokens.Surface)
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (index == 0 && pendingCount > 0) Color(0xFFD97706) else NeumorphicTokens.Accent
                                } else NeumorphicTokens.TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snackbar feedback banner
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = if (isErrorMessage) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isErrorMessage) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErrorMessage) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = snackbarMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isErrorMessage) Color(0xFFDC2626) else Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { snackbarMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ApprovalTabContent"
            ) { tab ->
                when (tab) {
                    0 -> PendingQueueTab(
                        state = state,
                        onOpenSwapDialog = { swapRequestTarget = it },
                        onOpenLeaveDialog = { leaveRequestTarget = it },
                        onOpenRejectDialog = { rejectRequestTarget = it }
                    )
                    1 -> AuditHistoryTab(
                        state = state
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Swap Approval Dialog
    swapRequestTarget?.let { req ->
        SwapApprovalDialog(
            request = req,
            eligiblePartners = state.eligibleLeavePartners,
            isLoadingPartners = state.isLoadingPartners,
            onDismiss = { swapRequestTarget = null },
            onFetchPartners = onFetchPartnersForDate,
            onConfirm = { partner, swapCode, note ->
                onSubmitAction(
                    req.requestId,
                    "swap",
                    partner,
                    swapCode,
                    note,
                    {
                        swapRequestTarget = null
                        snackbarMessage = "Shift swap approved with $partner ($swapCode)!"
                        isErrorMessage = false
                    },
                    { err ->
                        snackbarMessage = err
                        isErrorMessage = true
                    }
                )
            }
        )
    }

    // 2. Direct Leave Approval Dialog
    leaveRequestTarget?.let { req ->
        LeaveApprovalConfirmDialog(
            request = req,
            onDismiss = { leaveRequestTarget = null },
            onConfirm = { note ->
                onSubmitAction(
                    req.requestId,
                    "leave",
                    "",
                    "",
                    note,
                    {
                        leaveRequestTarget = null
                        snackbarMessage = "Approved as Annual Leave for ${req.engineerName}!"
                        isErrorMessage = false
                    },
                    { err ->
                        snackbarMessage = err
                        isErrorMessage = true
                    }
                )
            }
        )
    }

    // 3. Reject Dialog
    rejectRequestTarget?.let { req ->
        RejectApprovalDialog(
            request = req,
            onDismiss = { rejectRequestTarget = null },
            onConfirm = { note ->
                onSubmitAction(
                    req.requestId,
                    "reject",
                    "",
                    "",
                    note,
                    {
                        rejectRequestTarget = null
                        snackbarMessage = "Request rejected."
                        isErrorMessage = false
                    },
                    { err ->
                        snackbarMessage = err
                        isErrorMessage = true
                    }
                )
            }
        )
    }
}

@Composable
private fun PendingQueueTab(
    state: PortalState,
    onOpenSwapDialog: (LeaveRequestDto) -> Unit,
    onOpenLeaveDialog: (LeaveRequestDto) -> Unit,
    onOpenRejectDialog: (LeaveRequestDto) -> Unit
) {
    if (state.isLoadingAdminLeave && state.adminPendingLeaveRequests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = NeumorphicTokens.Accent,
                modifier = Modifier.size(36.dp)
            )
        }
        return
    }

    if (state.adminPendingLeaveRequests.isEmpty()) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            style = NeumorphicElevationStyle.RaisedSubtle,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Queue is Clear",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTokens.TextMain
                )

                Text(
                    text = "No pending leave or duty shift swap requests at this time. All team schedules are up to date.",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(state.adminPendingLeaveRequests, key = { it.requestId }) { req ->
            PendingRequestCard(
                request = req,
                onSwap = { onOpenSwapDialog(req) },
                onLeave = { onOpenLeaveDialog(req) },
                onReject = { onOpenRejectDialog(req) }
            )
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: LeaveRequestDto,
    onSwap: () -> Unit,
    onLeave: () -> Unit,
    onReject: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        style = NeumorphicElevationStyle.RaisedSubtle,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Engineer name, timestamp, badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeumorphicTokens.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.engineerName.take(1).uppercase().ifBlank { "E" },
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.Accent,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.engineerName.ifBlank { request.engineerId },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMain
                    )
                    Text(
                        text = "Submitted ${request.createdAt}",
                        fontSize = 11.sp,
                        color = NeumorphicTokens.TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Dates & Duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeumorphicTokens.Canvas)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = NeumorphicTokens.Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${request.dates.size} Day(s): ${request.dates.joinToString(", ")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicTokens.TextMain
                )
            }

            // Reason
            if (request.reason.isNotBlank()) {
                Text(
                    text = "\"${request.reason}\"",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            // Preferred Swap Partner badge
            if (request.preferredPartner.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Requested Swap with: ${request.preferredPartner}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            // Action Buttons
            HorizontalDivider(
                color = NeumorphicTokens.TextMuted.copy(alpha = 0.15f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Approve as Swap
                Button(
                    onClick = onSwap,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Approve Swap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 2. Approve as Leave
                Button(
                    onClick = onLeave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Approve Leave",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 3. Reject
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444).copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text(
                        text = "Reject",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditHistoryTab(
    state: PortalState
) {
    if (state.adminHistoricalLeaveRequests.isEmpty()) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            style = NeumorphicElevationStyle.RaisedSubtle,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeumorphicTokens.TextMuted,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "No Historical Requests",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTokens.TextMain
                )
                Text(
                    text = "Processed leave and swap decisions will appear in this audit log.",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(state.adminHistoricalLeaveRequests, key = { it.requestId }) { req ->
            val isSwap = req.status.contains("swap", ignoreCase = true)
            val isLeave = req.status.contains("leave", ignoreCase = true)
            val isRejected = req.status.contains("reject", ignoreCase = true)

            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                style = NeumorphicElevationStyle.RaisedSubtle,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = req.engineerName.ifBlank { req.engineerId },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isSwap -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                isLeave -> Color(0xFF10B981).copy(alpha = 0.15f)
                                isRejected -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                else -> NeumorphicTokens.TextMuted.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = req.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSwap -> Color(0xFF2563EB)
                                    isLeave -> Color(0xFF059669)
                                    isRejected -> Color(0xFFDC2626)
                                    else -> NeumorphicTokens.TextMuted
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${req.dates.size} Day(s): ${req.dates.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = NeumorphicTokens.TextMuted
                    )

                    req.approval?.let { app ->
                        if (app.swapPartner.isNotBlank()) {
                            Text(
                                text = "Swapped with: ${app.swapPartner} (${app.swapShiftCode})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB)
                            )
                        }
                        if (app.adminNote.isNotBlank()) {
                            Text(
                                text = "Admin Note: ${app.adminNote}",
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                        Text(
                            text = "Actioned by ${app.approvedByName.ifBlank { app.approvedBy }} at ${app.approvedAt}",
                            fontSize = 10.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapApprovalDialog(
    request: LeaveRequestDto,
    eligiblePartners: List<EligibleSwapPartner>,
    isLoadingPartners: Boolean,
    onDismiss: () -> Unit,
    onFetchPartners: (String) -> Unit,
    onConfirm: (partner: String, swapCode: String, note: String) -> Unit
) {
    var selectedPartner by remember { mutableStateOf(request.preferredPartner) }
    var swapCode by remember { mutableStateOf("") }
    var adminNote by remember { mutableStateOf("") }
    var partnerDropdownExpanded by remember { mutableStateOf(false) }

    // Fetch eligible partners on dialog mount
    LaunchedEffect(Unit) {
        val date = request.dates.firstOrNull().orEmpty()
        if (date.isNotBlank()) {
            onFetchPartners(date)
        }
    }

    // Auto-derive swap code when partner changes
    LaunchedEffect(selectedPartner) {
        if (selectedPartner.isNotBlank() && swapCode.isBlank()) {
            val initial = selectedPartner.firstOrNull()?.uppercase() ?: "A"
            swapCode = "SW-$initial"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Approve Shift Swap",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphicTokens.TextMain
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assign an off-duty partner to cover ${request.engineerName}'s shift on ${request.dates.joinToString(", ")}.",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    lineHeight = 16.sp
                )

                // Partner Selection Dropdown
                Text(
                    text = "Covering Partner:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicTokens.TextMain
                )

                ExposedDropdownMenuBox(
                    expanded = partnerDropdownExpanded,
                    onExpandedChange = { partnerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPartner,
                        onValueChange = { selectedPartner = it },
                        readOnly = false,
                        placeholder = { Text("Select or type covering colleague", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partnerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeumorphicTokens.Accent,
                            unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = partnerDropdownExpanded,
                        onDismissRequest = { partnerDropdownExpanded = false }
                    ) {
                        if (isLoadingPartners) {
                            DropdownMenuItem(
                                text = { Text("Loading available colleagues...", fontSize = 12.sp) },
                                onClick = { }
                            )
                        } else if (eligiblePartners.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No off-duty partners found for this date", fontSize = 12.sp) },
                                onClick = { partnerDropdownExpanded = false }
                            )
                        } else {
                            eligiblePartners.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = p.name.ifBlank { p.username },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (p.isOff) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (p.isOff) "OFF" else p.shift,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (p.isOff) Color(0xFF059669) else Color(0xFF2563EB),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPartner = p.username
                                        partnerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Swap Shift Code Input
                OutlinedTextField(
                    value = swapCode,
                    onValueChange = { swapCode = it.uppercase() },
                    label = { Text("Roster Shift Code (e.g. SW-A)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeumorphicTokens.Accent,
                        unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Optional Admin Note
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Manager Note (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeumorphicTokens.Accent,
                        unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedPartner, swapCode.ifBlank { "SW-A" }, adminNote)
                },
                enabled = selectedPartner.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Swap", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeumorphicTokens.TextMuted)
            }
        },
        containerColor = NeumorphicTokens.Surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun LeaveApprovalConfirmDialog(
    request: LeaveRequestDto,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var adminNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Approve as Annual Leave",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphicTokens.TextMain
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Approve ${request.dates.size} day(s) (${request.dates.joinToString(", ")}) for ${request.engineerName} as Paid Annual Leave?\n\nThis will assign 'L' to their roster shifts and deduct ${request.dates.size} day(s) from their annual quota balance.",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Manager Note (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeumorphicTokens.Accent,
                        unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(adminNote) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Leave", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeumorphicTokens.TextMuted)
            }
        },
        containerColor = NeumorphicTokens.Surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun RejectApprovalDialog(
    request: LeaveRequestDto,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var rejectReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reject Request",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Are you sure you want to reject the leave/swap request from ${request.engineerName} for ${request.dates.joinToString(", ")}?",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason for Rejection", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = NeumorphicTokens.TextMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rejectReason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reject Request", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeumorphicTokens.TextMuted)
            }
        },
        containerColor = NeumorphicTokens.Surface,
        shape = RoundedCornerShape(20.dp)
    )
}
