package com.rosterx.presentation.portal.leave

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.rosterx.domain.model.EligibleSwapPartner
import com.rosterx.domain.model.ShiftCode
import com.rosterx.presentation.components.NeumorphicCard
import com.rosterx.presentation.components.NeumorphicElevationStyle
import com.rosterx.presentation.portal.PortalState
import com.rosterx.ui.neumorphic.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale


private fun formatDisplayDateCompact(dateStr: String): String {
    return try {
        val clean = dateStr.trim().take(10)
        val dt = java.time.LocalDate.parse(clean)
        val day = String.format(java.util.Locale.ENGLISH, "%02d", dt.dayOfMonth)
        val month = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "$day $month ${dt.year}"
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatDatesListCompact(dates: List<String>): String {
    if (dates.isEmpty()) return ""
    return dates.joinToString(", ") { formatDisplayDateCompact(it) }
}

private fun formatTimestampCompact(tsStr: String): String {
    if (tsStr.isBlank()) return ""
    return try {
        val clean = tsStr.trim().replace("Z", "").replace("T", " ")
        val parts = clean.split(" ")
        val datePart = parts[0]
        val formattedDate = formatDisplayDateCompact(datePart)
        if (parts.size > 1 && parts[1].isNotBlank()) {
            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toIntOrNull() ?: 0
            val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val ampm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            "$formattedDate, ${String.format(java.util.Locale.ENGLISH, "%02d:%02d %s", displayHour, min, ampm)}"
        } else {
            formattedDate
        }
    } catch (_: Exception) {
        tsStr.take(19)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    state: PortalState,
    onBack: () -> Unit,
    onSubmitLeave: (List<String>, String, String, (String) -> Unit, (String) -> Unit) -> Unit,
    onFetchPartners: (String) -> Unit,
    onCancelRequest: ((requestId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0: Apply, 1: History
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Calendar state for application form
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val selectedDates = remember { mutableStateListOf<LocalDate>() }
    var preferredPartner by rememberSaveable { mutableStateOf("") }
    var reasonText by rememberSaveable { mutableStateOf("") }
    var partnerDropdownExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<com.rosterx.data.remote.LeaveRequestDto?>(null) }
    var showSubmitConfirmation by remember { mutableStateOf(false) }

    // Fetch eligible partners when first date is selected
    LaunchedEffect(selectedDates.firstOrNull()) {
        val firstDate = selectedDates.firstOrNull()
        if (firstDate != null) {
            onFetchPartners(firstDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        } else {
            onFetchPartners(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Leave Request",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "Annual leave application & balance tracking",
                            fontSize = 11.5.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
            }
        },
        containerColor = NeumorphicTokens.Canvas,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (isLandscape) 16.dp else 18.dp)
        ) {
            // Segmented Tab Switcher [ Apply Request ] ⇄ [ My History ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(NeumorphicTokens.InputSurface, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Apply Tab Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == 0) NeumorphicTokens.AccentSalmon else Color.Transparent
                        )
                        .clickable { activeTab = 0 }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = if (activeTab == 0) Color.White else NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Apply Request",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) Color.White else NeumorphicTokens.TextMuted
                        )
                    }
                }

                // History Tab Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == 1) NeumorphicTokens.AccentSalmon else Color.Transparent
                        )
                        .clickable { activeTab = 1 }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (activeTab == 1) Color.White else NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (state.myLeaveRequests.isNotEmpty()) "My History (${state.myLeaveRequests.size})" else "My History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) Color.White else NeumorphicTokens.TextMuted
                        )
                    }
                }
            }

            // Snackbar banner if present
            snackbarMessage?.let { msg ->
                Surface(
                    color = if (isErrorMessage) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = msg,
                        color = if (isErrorMessage) Color(0xFFDC2626) else Color(0xFF059669),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (activeTab == 0) {
                // APPLY REQUEST VIEW
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Month Calendar Card
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 14.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Title & Subtitle
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Select Leave Dates",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NeumorphicTokens.TextMain
                                )
                                Text(
                                    text = "Pick 1 to 5 days (Default OFF indicated)",
                                    fontSize = 11.5.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }

                            // Prominent Month Switcher Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeumorphicTokens.InputSurface)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = NeumorphicTokens.TextMain, modifier = Modifier.size(22.dp))
                                }
                                Text(
                                    text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp,
                                    color = NeumorphicTokens.TextMain,
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = NeumorphicTokens.TextMain, modifier = Modifier.size(22.dp))
                                }
                            }

                            // 7-day Weekday Header
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                    Text(
                                        text = day,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTokens.TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Day Matrix
                            val daysInMonth = currentYearMonth.lengthOfMonth()
                            val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sun
                            val totalSlots = firstDayOfWeek + daysInMonth
                            val rows = (totalSlots + 6) / 7

                            val applicantName = state.loggedInEngineer?.name ?: ""
                            val isCurrentLoadedMonth = currentYearMonth.year == state.gridYear && currentYearMonth.monthValue == state.gridMonth
                            val monthlyShifts = if (isCurrentLoadedMonth) {
                                state.gridRoster[applicantName] ?: state.liveRoster[applicantName] ?: emptyMap()
                            } else {
                                emptyMap()
                            }

                            val derivedDefaultOffs = remember(state.loggedInEngineer, state.engineers, state.liveRoster, state.gridRoster, applicantName) {
                                val direct = state.loggedInEngineer?.defaultWeeklyOffs?.filter { it.isNotBlank() } ?: emptyList()
                                if (direct.isNotEmpty()) return@remember direct

                                val fromList = state.engineers.find {
                                    it.username.equals(state.loggedInEngineer?.username, ignoreCase = true) ||
                                    it.name.equals(applicantName, ignoreCase = true)
                                }?.defaultWeeklyOffs?.filter { it.isNotBlank() } ?: emptyList()
                                if (fromList.isNotEmpty()) return@remember fromList

                                val rosterToScan = state.gridRoster[applicantName] ?: state.liveRoster[applicantName] ?: emptyMap()
                                if (rosterToScan.isNotEmpty()) {
                                    val offDayCounts = mutableMapOf<java.time.DayOfWeek, Int>()
                                    rosterToScan.forEach { (dayStr, shift) ->
                                        val d = dayStr.toIntOrNull()
                                        if (d != null && (shift == ShiftCode.OFF || shift.code.startsWith("SW"))) {
                                            try {
                                                val dt = LocalDate.of(state.gridYear, state.gridMonth, d)
                                                offDayCounts[dt.dayOfWeek] = (offDayCounts[dt.dayOfWeek] ?: 0) + 1
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    val recurringOffs = offDayCounts.filter { it.value >= 2 }.keys.map { it.name }
                                    if (recurringOffs.isNotEmpty()) return@remember recurringOffs
                                }

                                listOf("SATURDAY", "SUNDAY")
                            }

                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (c in 0 until 7) {
                                        val slotIndex = r * 7 + c
                                        val dayNum = slotIndex - firstDayOfWeek + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val date = currentYearMonth.atDay(dayNum)
                                            val isPast = date.isBefore(LocalDate.now())
                                            val resDateStr = state.loggedInEngineer?.resignationDate?.takeIf { it.isNotBlank() }
                                            val resDate = try { resDateStr?.let { LocalDate.parse(it.trim().take(10)) } } catch (_: Exception) { null }
                                            val isPostResignation = resDate != null && date.isAfter(resDate)
                                            val isDisabled = isPast || isPostResignation
                                            val dayOfWeekName = date.dayOfWeek.name
                                            val isDefaultWeeklyOff = derivedDefaultOffs.any { it.equals(dayOfWeekName, ignoreCase = true) }

                                            val shiftCode = monthlyShifts[dayNum.toString()]
                                            val isOff = when {
                                                shiftCode != null -> shiftCode == ShiftCode.OFF || shiftCode.code.startsWith("SW")
                                                else -> isDefaultWeeklyOff
                                            }
                                            val isSelected = selectedDates.contains(date)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> NeumorphicTokens.AccentSalmon
                                                            isDisabled -> NeumorphicTokens.InputSurface.copy(alpha = 0.5f)
                                                            else -> NeumorphicTokens.InputSurface
                                                        }
                                                    )
                                                    .clickable(enabled = !isDisabled) {
                                                        if (isSelected) {
                                                            selectedDates.remove(date)
                                                        } else {
                                                            if (selectedDates.size >= 5) {
                                                                snackbarMessage = "Maximum 5 days can be selected per request"
                                                                isErrorMessage = true
                                                            } else {
                                                                selectedDates.add(date)
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isDisabled -> NeumorphicTokens.TextMuted.copy(alpha = 0.4f)
                                                            else -> NeumorphicTokens.TextMain
                                                        }
                                                    )
                                                    if (isPostResignation) {
                                                        Text(
                                                            text = "EXIT",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                    } else if (isOff) {
                                                        Text(
                                                            text = "OFF",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFFD97706)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Selection counter and clear action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedDates.isNotEmpty()) NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f) else NeumorphicTokens.InputSurface
                                ) {
                                    Text(
                                        text = "Selected: ${selectedDates.size} / 5 days",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDates.isNotEmpty()) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (selectedDates.isNotEmpty()) {
                                    Text(
                                        text = "Clear selection",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedDates.clear() }
                                    )
                                }
                            }
                        }
                    }

                    // Reason Text Input
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 14.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Reason / Operational Notes (Optional)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMain
                            )
                            TextField(
                                value = reasonText,
                                onValueChange = { reasonText = it },
                                placeholder = { Text("Brief note for supervisor or partner...", fontSize = 12.5.sp, color = NeumorphicTokens.TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NeumorphicTokens.InputSurface,
                                    unfocusedContainerColor = NeumorphicTokens.InputSurface,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                    }

                    // Submit Action Button
                    Button(
                        onClick = {
                            if (selectedDates.isEmpty()) {
                                snackbarMessage = "Please select at least 1 date on the calendar"
                                isErrorMessage = true
                                return@Button
                            }
                            showSubmitConfirmation = true
                        },
                        enabled = selectedDates.isNotEmpty() && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeumorphicTokens.AccentSalmon,
                            disabledContainerColor = NeumorphicTokens.AccentSalmon.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isSubmitting) "Submitting..." else "Submit Leave Request",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // MY HISTORY VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Quota Summary Banner
                    item {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            style = NeumorphicElevationStyle.RaisedSubtle,
                            surfaceColor = NeumorphicTokens.Surface,
                            contentPadding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (state.leaveCappingEnabled) "ANNUAL LEAVE QUOTA" else "LEAVE REQUESTS",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeumorphicTokens.AccentSalmon
                                    )
                                    Text(
                                        text = if (state.leaveCappingEnabled && state.leaveRemainingDays != null && state.leaveAnnualQuota != null) {
                                            "${state.leaveRemainingDays} of ${state.leaveAnnualQuota} Days Remaining"
                                        } else {
                                            "Leave Requests"
                                        },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                    if (state.leaveCappingEnabled) {
                                        Text(
                                            text = "${state.leaveApprovedDays} approved days in ${LocalDate.now().year}",
                                            fontSize = 11.5.sp,
                                            color = NeumorphicTokens.TextMuted
                                        )
                                    } else {
                                        Text(
                                            text = "Submit and track your annual leave requests",
                                            fontSize = 11.5.sp,
                                            color = NeumorphicTokens.TextMuted
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = NeumorphicTokens.AccentSalmon,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Request History Cards
                    if (state.myLeaveRequests.isEmpty()) {
                        item {
                            NeumorphicCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                style = NeumorphicElevationStyle.RaisedSubtle,
                                surfaceColor = NeumorphicTokens.Surface,
                                contentPadding = 32.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = NeumorphicTokens.TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No leave requests submitted yet",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                    Text(
                                        text = "Applied leave and shift swap requests will appear here.",
                                        fontSize = 12.sp,
                                        color = NeumorphicTokens.TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.myLeaveRequests, key = { it.requestId }) { req ->
                            val st = req.status.lowercase()
                            val statusColor = when (st) {
                                "approved_swap" -> Color(0xFF059669)
                                "approved_leave" -> Color(0xFF2563EB)
                                "rejected" -> Color(0xFFDC2626)
                                else -> Color(0xFFD97706)
                            }
                            val statusBg = when (st) {
                                "approved_swap" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "approved_leave" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                "rejected" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            }
                            val statusLabel = when (st) {
                                "approved_swap" -> "Approved as Swap"
                                "approved_leave" -> "Approved as Leave"
                                "rejected" -> "Declined"
                                else -> "Pending Review"
                            }

                            NeumorphicCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                style = NeumorphicElevationStyle.RaisedSubtle,
                                surfaceColor = NeumorphicTokens.Surface,
                                contentPadding = 14.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = req.requestId,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = NeumorphicTokens.TextMain
                                        )
                                        Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                                            Text(
                                                text = statusLabel,
                                                color = statusColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Requested Dates Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        req.dates.forEach { d ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = NeumorphicTokens.InputSurface
                                            ) {
                                                Text(
                                                    text = formatDisplayDateCompact(d),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeumorphicTokens.TextMain,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (req.reason.isNotBlank()) {
                                        Text(
                                            text = "\"${req.reason}\"",
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = NeumorphicTokens.TextMuted
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val partner = req.approval?.swapPartner?.takeIf { it.isNotBlank() } ?: req.preferredPartner
                                        if (partner.isNotBlank()) {
                                            Text(
                                                text = "Partner: $partner",
                                                fontSize = 11.sp,
                                                color = NeumorphicTokens.TextMuted
                                            )
                                        }
                                        Text(
                                            text = "Submitted ${formatTimestampCompact(req.createdAt)}",
                                            fontSize = 11.sp,
                                            color = NeumorphicTokens.TextMuted
                                        )
                                    }

                                    req.approval?.adminNote?.takeIf { it.isNotBlank() }?.let { note ->
                                        Text(
                                            text = "Admin Note: $note",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (req.status.contains("reject", ignoreCase = true)) Color(0xFFEF4444) else NeumorphicTokens.TextMuted
                                        )
                                    }

                                    if (req.status.equals("pending", ignoreCase = true) && onCancelRequest != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { cancelTarget = req },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cancel Request", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
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
    }

    // Confirmation Dialog for Submission
    if (showSubmitConfirmation) {
        val sortedDates = selectedDates.sorted()
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = NeumorphicTokens.AccentSalmon)
                    Text("Submit Leave Request?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Submit leave request for ${sortedDates.size} day(s):\n${sortedDates.joinToString { formatDisplayDateCompact(it.format(DateTimeFormatter.ISO_LOCAL_DATE)) }}?\n\n" +
                    (if (preferredPartner.isNotBlank()) "Preferred cover partner: $preferredPartner\n" else "") +
                    (if (reasonText.isNotBlank()) "Reason: \"$reasonText\"" else ""),
                    fontSize = 13.sp,
                    color = NeumorphicTokens.TextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        isSubmitting = true
                        val formattedDates = sortedDates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                        onSubmitLeave(
                            formattedDates,
                            reasonText,
                            preferredPartner,
                            { msg ->
                                isSubmitting = false
                                snackbarMessage = msg
                                isErrorMessage = false
                                selectedDates.clear()
                                reasonText = ""
                                preferredPartner = ""
                                activeTab = 1
                            },
                            { err ->
                                isSubmitting = false
                                snackbarMessage = err
                                isErrorMessage = true
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.AccentSalmon)
                ) {
                    Text("Yes, Submit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Cancel", color = NeumorphicTokens.TextMuted)
                }
            }
        )
    }

    // Confirmation Dialog for Cancellation
    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("Cancel Leave Request?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to cancel your leave request for ${formatDatesListCompact(target.dates)}? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = NeumorphicTokens.TextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = target
                        cancelTarget = null
                        onCancelRequest?.invoke(req.requestId, { msg ->
                            snackbarMessage = msg
                            isErrorMessage = false
                        }, { err ->
                            snackbarMessage = err
                            isErrorMessage = true
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Yes, Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep Request", color = NeumorphicTokens.TextMuted)
                }
            }
        )
    }
}