package com.rosterx.presentation.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.theme.LocalIsDarkTheme
import com.rosterx.ui.theme.LocalThemeAccent
import com.rosterx.util.DeviceCalendar
import com.rosterx.util.ShiftCalendarExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ReminderChoice(
    val label: String,
    val minutes: Int?
)

private val REMINDER_CHOICES = listOf(
    ReminderChoice("None", null),
    ReminderChoice("15m", 15),
    ReminderChoice("30m", 30),
    ReminderChoice("1h", 60)
)

@Composable
fun CalendarSyncDialog(
    engineerName: String,
    username: String,
    year: Int,
    month: Int,
    dayShiftCodes: Map<Int, String>,
    onDismiss: () -> Unit,
    onSyncRecorded: ((month: String, count: Int, target: String) -> Unit)? = null,
    onSyncCleared: ((month: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val themeAccent = LocalThemeAccent.current
    val isDark = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()

    val monthLabel = remember(year, month) {
        runCatching {
            YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        }.getOrDefault("$year-$month")
    }
    val preview = remember(dayShiftCodes) { ShiftCalendarExporter.computePreview(dayShiftCodes) }

    var hasPermission by remember { mutableStateOf(ShiftCalendarExporter.hasCalendarPermissions(context)) }
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var selectedCalendarId by remember { mutableStateOf<Long?>(ShiftCalendarExporter.getSavedCalendarId(context)) }
    var selectedReminderMinutes by remember { mutableStateOf<Int?>(ShiftCalendarExporter.getSavedReminderMinutes(context)) }
    var existingSyncedCount by remember { mutableIntStateOf(0) }
    var isLoadingCalendars by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    fun refreshCalendars() {
        if (!ShiftCalendarExporter.hasCalendarPermissions(context)) {
            hasPermission = false
            calendars = emptyList()
            existingSyncedCount = 0
            return
        }
        hasPermission = true
        scope.launch {
            isLoadingCalendars = true
            val found = withContext(Dispatchers.IO) {
                ShiftCalendarExporter.getWritableCalendars(context)
            }
            calendars = found
            val validSaved = selectedCalendarId?.takeIf { id -> found.any { it.id == id } }
            val chosenId = validSaved ?: found.firstOrNull()?.id
            selectedCalendarId = chosenId
            existingSyncedCount = if (chosenId != null) {
                withContext(Dispatchers.IO) {
                    ShiftCalendarExporter.countSyncedEventsForMonth(context, chosenId, username, year, month)
                }
            } else {
                0
            }
            isLoadingCalendars = false
        }
    }

    LaunchedEffect(hasPermission, year, month, username) {
        if (hasPermission) {
            refreshCalendars()
        }
    }

    LaunchedEffect(selectedCalendarId, hasPermission, year, month, username) {
        val calId = selectedCalendarId
        if (hasPermission && calId != null) {
            existingSyncedCount = withContext(Dispatchers.IO) {
                ShiftCalendarExporter.countSyncedEventsForMonth(context, calId, username, year, month)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.READ_CALENDAR] == true &&
            grants[Manifest.permission.WRITE_CALENDAR] == true
        hasPermission = granted || ShiftCalendarExporter.hasCalendarPermissions(context)
        if (hasPermission) {
            statusMessage = null
            statusIsError = false
            refreshCalendars()
        } else {
            statusMessage = "Calendar permission was declined. You can still use Share .ics below."
            statusIsError = true
        }
    }

    NeumorphicDialog(
        onDismissRequest = { if (!isSyncing && !isClearing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    NeumorphicCard(
                        shape = RoundedCornerShape(14.dp),
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Sync to Calendar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = monthLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
                NeumorphicCard(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isSyncing && !isClearing
                    ) { onDismiss() },
                    shape = CircleShape,
                    style = NeumorphicElevationStyle.RaisedSubtle,
                    surfaceColor = NeumorphicTokens.Surface,
                    contentPadding = 0.dp
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Personal Scope Summary Well (Neumorphic Inset)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        shape = RoundedCornerShape(18.dp),
                        elevation = NeumorphicTokens.ElevationInsetSubtle,
                        surfaceColor = NeumorphicTokens.InputSurface
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = engineerName.ifBlank { "Team Member" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (username.isNotBlank()) {
                            Text(
                                text = "@${username.trim()} • Personal schedule only",
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeAccent.copy(alpha = 0.13f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${preview.totalCount} days",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themeAccent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryBadgePill(
                        label = "${preview.workingCount} Shifts • Timed",
                        accent = themeAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryBadgePill(
                        label = "${preview.nonWorkingCount} Off/Leave • All-Day",
                        accent = NeumorphicTokens.StatusEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Only your personal shifts are synced. Existing personal events are never modified.",
                    fontSize = 10.5.sp,
                    color = NeumorphicTokens.TextMuted,
                    lineHeight = 14.sp
                )
            }

            // Target Calendar Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = themeAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "TARGET CALENDAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMuted,
                            letterSpacing = 0.6.sp
                        )
                    }
                    if (hasPermission && existingSyncedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeumorphicTokens.StatusEmerald.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Synced ($existingSyncedCount)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.StatusEmerald
                            )
                        }
                    }
                }

                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(
                                shape = RoundedCornerShape(16.dp),
                                elevation = NeumorphicTokens.ElevationInsetSubtle,
                                surfaceColor = NeumorphicTokens.InputSurface
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Allow Calendar access below to pick your Google or Work calendar and keep monthly shifts updated with zero duplicates.",
                            fontSize = 11.5.sp,
                            color = NeumorphicTokens.TextMain,
                            lineHeight = 16.sp
                        )
                    }
                } else if (isLoadingCalendars) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = themeAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Loading device calendars...",
                            fontSize = 12.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                } else if (calendars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(
                                shape = RoundedCornerShape(16.dp),
                                elevation = NeumorphicTokens.ElevationInsetSubtle,
                                surfaceColor = NeumorphicTokens.InputSurface
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "No writable calendar account found on this device. Use Share .ics below instead.",
                            fontSize = 11.5.sp,
                            color = NeumorphicTokens.TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(
                                shape = RoundedCornerShape(16.dp),
                                elevation = NeumorphicTokens.ElevationInsetSubtle,
                                surfaceColor = NeumorphicTokens.InputSurface
                            )
                            .heightIn(max = 176.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        calendars.forEach { cal ->
                            val isSelected = cal.id == selectedCalendarId
                            val rowShape = RoundedCornerShape(12.dp)
                            val rowModifier = if (isSelected) {
                                Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = rowShape,
                                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.14f),
                                        spotColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.14f)
                                    )
                                    .clip(rowShape)
                                    .background(NeumorphicTokens.Surface)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(Color.Transparent)
                            }

                            Row(
                                modifier = rowModifier
                                    .clickable {
                                        selectedCalendarId = cal.id
                                        ShiftCalendarExporter.saveCalendarId(context, cal.id)
                                        statusMessage = null
                                    }
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cal.color))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cal.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) themeAccent else NeumorphicTokens.TextMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cal.accountName.isNotBlank() &&
                                        !cal.accountName.equals(cal.displayName, ignoreCase = true)
                                    ) {
                                        Text(
                                            text = cal.accountName,
                                            fontSize = 10.5.sp,
                                            color = NeumorphicTokens.TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = themeAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shift Reminder Neumorphic Pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = themeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "SHIFT REMINDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMuted,
                        letterSpacing = 0.6.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REMINDER_CHOICES.forEach { choice ->
                        val selected = selectedReminderMinutes == choice.minutes
                        val pillShape = RoundedCornerShape(12.dp)
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = pillShape,
                                        ambientColor = themeAccent.copy(alpha = 0.35f),
                                        spotColor = themeAccent.copy(alpha = 0.35f)
                                    )
                                    .clip(pillShape)
                                    .background(NeumorphicTokens.AccentGradient)
                                    .clickable {
                                        selectedReminderMinutes = choice.minutes
                                        ShiftCalendarExporter.saveReminderMinutes(context, choice.minutes)
                                    }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = choice.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            NeumorphicCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedReminderMinutes = choice.minutes
                                        ShiftCalendarExporter.saveReminderMinutes(context, choice.minutes)
                                    },
                                shape = pillShape,
                                style = NeumorphicElevationStyle.RaisedSubtle,
                                surfaceColor = NeumorphicTokens.Surface,
                                contentPadding = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = choice.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Applies to working shifts (Busy). Off, Leave & Swap Off sync silently as All-Day (Free).",
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = NeumorphicTokens.TextMuted
                )
            }

            // Inline Status Feedback
            if (!statusMessage.isNullOrBlank()) {
                val bannerColor = if (statusIsError) Color(0xFFDC2626) else NeumorphicTokens.StatusEmerald
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bannerColor.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bannerColor,
                        lineHeight = 16.sp
                    )
                }
            }

            // Single Smart Primary Neumorphic CTA
            if (!hasPermission) {
                NeumorphicButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    },
                    variant = NeumorphicButtonVariant.PrimaryAccent,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = 13.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Allow Calendar Access",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            } else {
                val canDirectSync = selectedCalendarId != null && preview.totalCount > 0 && !isSyncing && !isClearing
                NeumorphicButton(
                    onClick = {
                        val calId = selectedCalendarId ?: return@NeumorphicButton
                        scope.launch {
                            isSyncing = true
                            statusMessage = null
                            val result = withContext(Dispatchers.IO) {
                                ShiftCalendarExporter.syncMonthToCalendar(
                                    context = context,
                                    calendarId = calId,
                                    engineerName = engineerName,
                                    username = username,
                                    year = year,
                                    month = month,
                                    dayShiftCodes = dayShiftCodes,
                                    reminderMinutes = selectedReminderMinutes
                                )
                            }
                            isSyncing = false
                            result.onSuccess { stats ->
                                existingSyncedCount = stats.totalSynced
                                val monthKey = String.format(Locale.US, "%04d-%02d", year, month)
                                val chosenAccount = calendars.firstOrNull { it.id == calId }?.accountName ?: "Google Calendar"
                                onSyncRecorded?.invoke(monthKey, stats.totalSynced, chosenAccount)
                                val detail = buildString {
                                    append("Synced ${stats.totalSynced} days for $monthLabel")
                                    val parts = mutableListOf<String>()
                                    if (stats.inserted > 0) parts.add("${stats.inserted} added")
                                    if (stats.updated > 0) parts.add("${stats.updated} updated")
                                    if (stats.deleted > 0) parts.add("${stats.deleted} removed")
                                    if (parts.isNotEmpty()) {
                                        append(" (${parts.joinToString(", ")})")
                                    }
                                }
                                statusMessage = detail
                                statusIsError = false
                                Toast.makeText(context, detail, Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                statusMessage = err.message ?: "Calendar sync failed"
                                statusIsError = true
                            }
                        }
                    },
                    variant = NeumorphicButtonVariant.PrimaryAccent,
                    shape = RoundedCornerShape(16.dp),
                    enabled = canDirectSync,
                    isLoading = isSyncing,
                    contentPadding = 13.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (existingSyncedCount > 0) {
                            "Re-Sync ${preview.totalCount} Days"
                        } else {
                            "Sync ${preview.totalCount} Days to Calendar"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Secondary Neumorphic Actions: Share .ics fallback + Clear Synced Month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    onClick = {
                        val res = ShiftCalendarExporter.exportMonthlyShifts(
                            context = context,
                            engineerName = engineerName,
                            username = username,
                            year = year,
                            month = month,
                            dayShiftCodes = dayShiftCodes,
                            reminderMinutes = selectedReminderMinutes,
                            preferShareSheet = true
                        )
                        res.onSuccess { count ->
                            Toast.makeText(
                                context,
                                "Prepared $count personal calendar days (.ics)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { err ->
                            statusMessage = err.message ?: "Failed to generate .ics file"
                            statusIsError = true
                        }
                    },
                    variant = NeumorphicButtonVariant.Surface,
                    shape = RoundedCornerShape(14.dp),
                    enabled = preview.totalCount > 0 && !isSyncing && !isClearing,
                    contentPadding = 11.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = NeumorphicTokens.TextMain,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Share .ics",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMain
                    )
                }

                if (hasPermission && selectedCalendarId != null && existingSyncedCount > 0) {
                    NeumorphicButton(
                        onClick = {
                            val calId = selectedCalendarId ?: return@NeumorphicButton
                            scope.launch {
                                isClearing = true
                                val res = withContext(Dispatchers.IO) {
                                    ShiftCalendarExporter.clearMonthFromCalendar(
                                        context = context,
                                        calendarId = calId,
                                        username = username,
                                        year = year,
                                        month = month
                                    )
                                }
                                isClearing = false
                                res.onSuccess { removed ->
                                    existingSyncedCount = 0
                                    val monthKey = String.format(Locale.US, "%04d-%02d", year, month)
                                    onSyncCleared?.invoke(monthKey)
                                    val msg = "Cleared $removed RosterX events for $monthLabel"
                                    statusMessage = msg
                                    statusIsError = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    statusMessage = err.message ?: "Could not clear synced events"
                                    statusIsError = true
                                }
                            }
                        },
                        variant = NeumorphicButtonVariant.Surface,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSyncing && !isClearing,
                        isLoading = isClearing,
                        contentPadding = 11.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Clear Month",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBadgePill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.11f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
