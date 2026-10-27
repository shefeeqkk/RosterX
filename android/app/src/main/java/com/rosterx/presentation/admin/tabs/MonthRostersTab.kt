package com.rosterx.presentation.admin.tabs

import android.graphics.Color.parseColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.domain.model.ShiftCode
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.portal.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getEngineerSwapCode(engName: String, allEngineers: List<Engineer>): String {
    if (engName.isBlank()) return "SW"
    val cleanName = engName.trim().uppercase()
    val firstName = cleanName.split(Regex("[\\s\\W]+"))[0]
    val firstChar = firstName.take(1)

    val sharingFirstLetter = allEngineers.filter {
        val fName = it.name.trim().uppercase().split(Regex("[\\s\\W]+"))[0]
        fName.startsWith(firstChar)
    }

    return if (sharingFirstLetter.size <= 1) {
        "SW$firstChar"
    } else {
        "SW${firstName.take(2)}"
    }
}

@Composable
fun CompactShiftCard(
    code: String,
    name: String,
    timing: String,
    bgColorHex: String,
    textColorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(parseColor(bgColorHex)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.height(52.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = code,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Color(parseColor(textColorHex))
            )
            Text(
                text = name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(parseColor(textColorHex))
            )
            Text(
                text = timing,
                fontSize = 7.5.sp,
                maxLines = 1,
                color = Color(parseColor(textColorHex)).copy(alpha = 0.85f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MonthRostersTab(
    state: AdminRosterState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveRoster: (Map<String, Map<String, ShiftCode>>) -> Unit,
    onUpdateShift: (Engineer, Int, ShiftCode?) -> Unit = { _, _, _ -> },
    onLoadBackups: () -> Unit = {},
    onRestoreBackup: (String) -> Unit = {},
    initialSearchQuery: String = ""
) {
    val yearMonth = YearMonth.of(state.currentYear, state.currentMonth)
    val daysInMonth = yearMonth.lengthOfMonth()
    val scrollState = rememberScrollState()

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var mutableRoster by remember(state.roster) { mutableStateOf(state.roster) }
    var lockedCells by remember { mutableStateOf(mutableSetOf<Pair<String, String>>()) }

    var activeCell by remember { mutableStateOf<Triple<Engineer, Int, ShiftCode?>?>(null) }
    var showShiftSheet by remember { mutableStateOf(false) }
    var showPublishConfirmDialog by remember { mutableStateOf(false) }
    var showBackupsDialog by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<com.rosterx.domain.repository.RosterBackup?>(null) }

    val filteredEngineers = remember(state.engineers, searchQuery, state.currentYear, state.currentMonth, mutableRoster) {
        val sorted = sortEngineersByWeekOffs(state.engineers)
        val active = filterEngineersByResignation(sorted, state.currentYear, state.currentMonth, 1, mutableRoster)
        if (searchQuery.isBlank()) active
        else active.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    // Helper to apply shift change instantly to local state and notify ViewModel in realtime
    val updateActiveCellShift: (ShiftCode?) -> Unit = { newShift ->
        val cell = activeCell
        if (cell != null) {
            val (eng, day, _) = cell
            val currentEngRoster = getRosterForEngineer(mutableRoster, eng).toMutableMap()
            if (newShift == null) {
                currentEngRoster.remove(day.toString())
            } else {
                currentEngRoster[day.toString()] = newShift
            }

            // Create a brand-new Map instance to guarantee immediate Compose recomposition
            val newRoster = mutableRoster.toMutableMap()
            val keys = listOfNotNull(
                eng.username.takeIf { it.isNotBlank() },
                eng.id.takeIf { it.isNotBlank() },
                eng.name.takeIf { it.isNotBlank() },
                eng.name.uppercase().takeIf { it.isNotBlank() },
                eng.name.lowercase().takeIf { it.isNotBlank() },
                eng.name.trim().split(" ").firstOrNull()?.lowercase()
            )
            keys.forEach { k -> newRoster[k] = currentEngRoster }
            mutableRoster = newRoster

            // Notify ViewModel state in real-time
            onUpdateShift(eng, day, newShift)

            // Auto-lock cell if swap shift or leave (matches web app behavior)
            val cellKey = Pair(eng.id, day.toString())
            if (newShift is ShiftCode.L || newShift is ShiftCode.SW || (newShift != null && newShift.code.startsWith("SW"))) {
                lockedCells = lockedCells.toMutableSet().apply { add(cellKey) }
            }

            showShiftSheet = false
        }
    }

    // Web-Matching Shift Selection Sheet with Swaps & Lock Toggle
    if (showShiftSheet && activeCell != null) {
        val (eng, day, _) = activeCell!!
        val isLocked = lockedCells.contains(Pair(eng.id, day.toString()))

        // All active peers excluding the current engineer and resigned peers (exact match with web app)
        val peerEngineers = remember(eng, state.engineers) {
            state.engineers.filter { peer ->
                peer.id != eng.id && 
                !peer.name.equals(eng.name, ignoreCase = true) &&
                peer.status != EngineerStatus.RESIGNED
            }
        }

        ModalBottomSheet(onDismissRequest = { showShiftSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Header with Dismiss X Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${yearMonth.month.name.take(3)} $day · ${eng.name.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select Shift Code",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showShiftSheet = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Compact 3-in-a-row Standard Shifts Grid (3 Rows x 3 Columns = 8 Shifts + Clear)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: FS, FG, G
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactShiftCard(
                            code = "FS",
                            name = "First Shift",
                            timing = "07:30 - 16:30",
                            bgColorHex = ShiftCode.FS.bgColorHex,
                            textColorHex = ShiftCode.FS.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.FS) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactShiftCard(
                            code = "FG",
                            name = "First Gen",
                            timing = "08:30 - 17:30",
                            bgColorHex = ShiftCode.FG.bgColorHex,
                            textColorHex = ShiftCode.FG.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.FG) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactShiftCard(
                            code = "G",
                            name = "General",
                            timing = "09:30 - 18:30",
                            bgColorHex = ShiftCode.G.bgColorHex,
                            textColorHex = ShiftCode.G.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.G) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: SG, SS, NS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactShiftCard(
                            code = "SG",
                            name = "Second Gen",
                            timing = "11:00 - 20:00",
                            bgColorHex = ShiftCode.SG.bgColorHex,
                            textColorHex = ShiftCode.SG.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.SG) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactShiftCard(
                            code = "SS",
                            name = "Second Shift",
                            timing = "13:00 - 22:00",
                            bgColorHex = ShiftCode.SS.bgColorHex,
                            textColorHex = ShiftCode.SS.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.SS) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactShiftCard(
                            code = "NS",
                            name = "Night Shift",
                            timing = "21:00 - 07:30",
                            bgColorHex = ShiftCode.NS.bgColorHex,
                            textColorHex = ShiftCode.NS.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.NS) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: OFF, L, Clear Shift (Make Blank)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactShiftCard(
                            code = "OFF",
                            name = "Weekly Off",
                            timing = "Rest Day",
                            bgColorHex = ShiftCode.OFF.bgColorHex,
                            textColorHex = ShiftCode.OFF.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.OFF) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactShiftCard(
                            code = "L",
                            name = "Leave",
                            timing = "Approved",
                            bgColorHex = ShiftCode.L.bgColorHex,
                            textColorHex = ShiftCode.L.textColorHex,
                            onClick = { updateActiveCellShift(ShiftCode.L) },
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { updateActiveCellShift(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "CLEAR",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                                Text(
                                    text = "Make Blank",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Reset Cell",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Swap with Peer (OFF) Section - Web Format Matching
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Swap with Peer (OFF):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Text(
                                "Peer covers shift",
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (peerEngineers.isEmpty()) {
                            Text(
                                "No peer engineers available for swap.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Scrollable container with FlowRow for all peers (matches web pickerSwapButtonsContainer)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 115.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                    .padding(4.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    peerEngineers.forEach { peer ->
                                        val swapCode = getEngineerSwapCode(peer.name, state.engineers)
                                        val firstName = peer.name.trim().split(Regex("[\\s\\W]+")).firstOrNull()?.uppercase() ?: peer.name.uppercase()
                                        Surface(
                                            onClick = {
                                                updateActiveCellShift(ShiftCode.fromString(swapCode))
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, Color(0xFF94A3B8))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = swapCode,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF78350F)
                                                )
                                                Text(
                                                    text = "($firstName)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Lock Cell Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newSet = lockedCells.toMutableSet()
                            val cellKey = Pair(eng.id, day.toString())
                            if (newSet.contains(cellKey)) newSet.remove(cellKey) else newSet.add(cellKey)
                            lockedCells = newSet
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isLocked,
                        onCheckedChange = { checked ->
                            val newSet = lockedCells.toMutableSet()
                            val cellKey = Pair(eng.id, day.toString())
                            if (checked) newSet.add(cellKey) else newSet.remove(cellKey)
                            lockedCells = newSet
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Cell 🔒 (Immutable in AI Generation)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPreviousMonth) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                            }
                            Text(
                                text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(Locale.US),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = onNextMonth) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    onClick = { searchQuery = "" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Filter: $searchQuery",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear Filter",
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            // Roster Recovery Backups Button
                            IconButton(
                                onClick = {
                                    onLoadBackups()
                                    showBackupsDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Roster Recovery Backups",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("ENGINEER", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .horizontalScroll(scrollState)
                        ) {
                            for (day in 1..daysInMonth) {
                                val date = yearMonth.atDay(day)
                                val dayOfWeekStr = date.dayOfWeek.name.take(2)
                                val isWeekend = date.dayOfWeek.value >= 6

                                Column(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .fillMaxHeight()
                                        .background(if (isWeekend) WeekendLightOrange else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(day.toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isWeekend) Color(0xFF7C2D12) else MaterialTheme.colorScheme.onSurface)
                                    Text(dayOfWeekStr, fontSize = 9.sp, color = if (isWeekend) Color(0xFF7C2D12) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredEngineers) { engineer ->
                            val engRoster = getRosterForEngineer(mutableRoster, engineer)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(engineer.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .horizontalScroll(scrollState)
                                        ) {
                                    for (day in 1..daysInMonth) {
                                        val shift = engRoster[day.toString()]
                                        val isLocked = lockedCells.contains(Pair(engineer.id, day.toString()))

                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .fillMaxHeight()
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .background(if (shift != null) Color(parseColor(shift.bgColorHex)) else Color.Transparent)
                                                .combinedClickable(
                                                    onClick = {
                                                        activeCell = Triple(engineer, day, shift)
                                                        showShiftSheet = true
                                                    },
                                                    onLongClick = {
                                                        val newSet = lockedCells.toMutableSet()
                                                        val cellKey = Pair(engineer.id, day.toString())
                                                        if (newSet.contains(cellKey)) newSet.remove(cellKey) else newSet.add(cellKey)
                                                        lockedCells = newSet
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (shift != null) {
                                                Text(shift.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(parseColor(shift.textColorHex)))
                                            }
                                            if (isLocked) {
                                                Text("🔒", fontSize = 8.sp, modifier = Modifier.align(Alignment.TopEnd).padding(1.dp))
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

        // Floating Action Button for Publishing (Prominent, un-wrapped, easily accessible)
        ExtendedFloatingActionButton(
            onClick = { showPublishConfirmDialog = true },
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Publish", modifier = Modifier.size(18.dp)) },
            text = { Text("Publish Roster", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // Confirmation Dialog before Publishing to Atlas M0
    if (showPublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Publish Roster to Atlas?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are publishing the roster for ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(Locale.US)} (${filteredEngineers.size} engineers).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-Recovery Backup: An automatic snapshot will be saved to MongoDB Atlas & disk before overwriting. If published wrongly, you can restore previous versions at any time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPublishConfirmDialog = false
                        onSaveRoster(mutableRoster)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Publish Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backups / Recovery Dialog
    if (showBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupsDialog = false },
            icon = {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Roster Recovery Backups",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saved snapshots for ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}. Restore any snapshot if changes were published in error.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.isBackupsLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (state.backups.isEmpty()) {
                        Text(
                            text = "No backup snapshots found for this month yet. A snapshot is automatically recorded whenever a roster is published.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        state.backups.forEach { backup ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backup.timestamp,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "By: ${backup.publishedBy} · ${backup.totalEngineers} engineers",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "ID: ${backup.backupId}",
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { backupToRestore = backup },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (backupToRestore != null) {
        val b = backupToRestore!!
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Restore?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to restore the roster snapshot from ${b.timestamp}? This will overwrite the current active roster in MongoDB Atlas and the web portal.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = b.backupId
                        backupToRestore = null
                        showBackupsDialog = false
                        onRestoreBackup(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

