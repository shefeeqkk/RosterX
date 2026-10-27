package com.rosterx.presentation.portal

import android.graphics.Color.parseColor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.BuildConfig
import com.rosterx.R
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.util.NetworkUtils
import com.rosterx.ui.neumorphic.*
import com.rosterx.presentation.components.*
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.ShiftCode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- JS Business Logic Adapters ---

val WeekendLightOrange = Color(0xFFFED7AA).copy(alpha = 0.35f)

fun getWeekOffSortScore(weeklyOffs: List<String>?): Int {
    if (weeklyOffs.isNullOrEmpty()) return 999
    val norm = weeklyOffs.map { it.trim().uppercase().take(3) }
    val has: (String) -> Boolean = { day -> norm.contains(day) }

    return when {
        has("SUN") && has("MON") -> 1
        has("MON") && has("TUE") -> 2
        has("TUE") && has("WED") -> 3
        has("WED") && has("THU") -> 4
        has("THU") && has("FRI") -> 5
        has("FRI") && has("SAT") -> 6
        has("SAT") && has("SUN") -> 7
        else -> 10
    }
}

fun getEngineerOrderWeight(eng: Engineer): Int {
    val nameUpper = eng.name.trim().uppercase()
    return when {
        nameUpper.contains("NANDU") -> 1
        nameUpper.contains("DYANI") -> 2
        nameUpper.contains("SHEFEEQ") -> 3
        nameUpper.contains("HEMANTH") -> 4
        nameUpper.contains("ATUL") -> 5
        nameUpper.contains("SILAMBARASAN") -> 6
        nameUpper.contains("MANOJ") -> 7
        nameUpper.contains("RAJ KUMAR") -> 8
        nameUpper.contains("ROHIT") -> 9
        nameUpper.contains("DHANUSH") -> 10
        else -> 9999
    }
}

fun sortEngineersByWeekOffs(engineers: List<Engineer>): List<Engineer> {
    return engineers.sortedWith(
        compareBy<Engineer> { getWeekOffSortScore(it.defaultWeeklyOffs) }
            .thenBy { getEngineerOrderWeight(it) }
            .thenBy { it.name }
    )
}

fun parseFlexibleDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    val trimmed = dateStr.trim()
    val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy")
    )
    for (fmt in formatters) {
        try {
            return LocalDate.parse(trimmed, fmt)
        } catch (_: Exception) {}
    }
    return null
}

fun filterEngineersByResignation(
    engineers: List<Engineer>, 
    year: Int, 
    month: Int, 
    day: Int = 1,
    roster: Map<String, Map<String, ShiftCode>>? = null
): List<Engineer> {
    val checkDate = try { LocalDate.of(year, month, day) } catch (_: Exception) { LocalDate.now() }
    val monthStart = try { LocalDate.of(year, month, 1) } catch (_: Exception) { LocalDate.now().withDayOfMonth(1) }

    return engineers.filter { eng ->
        when (eng.status) {
            EngineerStatus.ACTIVE -> true
            EngineerStatus.RESIGNED -> {
                // 1. If engineer has recorded shifts in this month's roster, keep them to preserve history
                val hasRecordedShifts = if (roster != null) {
                    val engShifts = getRosterForEngineer(roster, eng)
                    engShifts.values.any { it != ShiftCode.OFF }
                } else false

                if (hasRecordedShifts) {
                    true
                } else {
                    // 2. Evaluate against resignation date
                    val resignDate = parseFlexibleDate(eng.resignationDate)
                    if (resignDate != null) {
                        if (day > 1) {
                            !checkDate.isAfter(resignDate)
                        } else {
                            // In month roster grid, show if resignation was on or after month start
                            !monthStart.isAfter(resignDate)
                        }
                    } else {
                        // Resigned with no exit date specified -> exclude from active/coming month rosters
                        false
                    }
                }
            }
            else -> true
        }
    }
}

fun getRosterForEngineer(roster: Map<String, Map<String, ShiftCode>>, engineer: Engineer?): Map<String, ShiftCode> {
    if (engineer == null) return emptyMap()
    return roster[engineer.username] 
        ?: roster[engineer.id] 
        ?: roster[engineer.name] 
        ?: roster[engineer.name.uppercase()]
        ?: roster[engineer.name.lowercase()]
        ?: roster[engineer.name.split(" ")[0].lowercase()]
        ?: emptyMap()
}

val ALL_STANDARD_SHIFTS = listOf(
    ShiftCode.FS to "07:30 AM – 04:30 PM",
    ShiftCode.FG to "08:30 AM – 05:30 PM",
    ShiftCode.G to "09:30 AM – 06:30 PM",
    ShiftCode.SG to "11:00 AM – 08:00 PM",
    ShiftCode.SS to "01:00 PM – 10:00 PM",
    ShiftCode.NS to "09:00 PM – 07:30 AM",
    ShiftCode.OFF to "Rest Day",
    ShiftCode.L to "Approved Leave"
)

fun getShiftChronologicalOrder(shift: ShiftCode): Int {
    return when (shift) {
        is ShiftCode.FS -> 1
        is ShiftCode.FG -> 2
        is ShiftCode.G -> 3
        is ShiftCode.SG -> 4
        is ShiftCode.SS -> 5
        is ShiftCode.NS -> 6
        is ShiftCode.OFF -> 7
        is ShiftCode.L -> 8
        else -> if (shift.code.startsWith("SW")) 7 else 9
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineerPortalScreen(
    viewModel: EngineerPortalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullScreenRoster = isLandscape && selectedTab == 3

    Scaffold(
        topBar = {
            if (!isFullScreenRoster) {
                TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rosterx_logo),
                            contentDescription = "RosterX Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "RosterX",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = NeumorphicTokens.AccentSalmon
                        )
                    }
                },
                actions = {
                    if (state.loggedInEngineer != null) {
                        NeumorphicCard(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { selectedTab = 4 },
                            shape = CircleShape,
                            style = NeumorphicElevationStyle.RaisedSubtle,
                            surfaceColor = NeumorphicTokens.Surface,
                            contentPadding = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(NeumorphicTokens.AccentGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.loggedInEngineer!!.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.loggedInEngineer!!.name.split(" ")[0],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeumorphicTokens.TextMain
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
        },
        floatingActionButton = {
            if (selectedTab == 1 && state.selectedDate != LocalDate.now()) {
                Box(
                    modifier = Modifier
                        .neumorphicRaised(
                            shape = RoundedCornerShape(16.dp),
                            elevation = NeumorphicTokens.ElevationRaised
                        )
                        .background(
                            NeumorphicTokens.AccentSalmon.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.selectDate(LocalDate.now()) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jump to Today", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            val portalNavItems = remember {
                listOf(
                    NavTabItem(0, "Dashboard", Icons.Default.Dashboard),
                    NavTabItem(1, "Team Shifts", Icons.Default.Group),
                    NavTabItem(2, "My Calendar", Icons.Default.CalendarMonth),
                    NavTabItem(3, "Month Roster", Icons.Default.GridView),
                    NavTabItem(4, "Settings", Icons.Default.Settings)
                )
            }

            if (!isFullScreenRoster) {
                FluidNeumorphicBottomNav(
                    items = portalNavItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(if (!isFullScreenRoster) paddingValues else PaddingValues(0.dp))
            .background(NeumorphicTokens.Canvas)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null && state.engineers.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Retry")
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(animationSpec = tween(250)) { it } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(250)) { -it } + fadeOut()
                        } else {
                            slideInHorizontally(animationSpec = tween(250)) { -it } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(250)) { it } + fadeOut()
                        }
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> DashboardView(
                            state = state,
                            onDismissNotification = viewModel::dismissNotification,
                            onClearAllNotifications = viewModel::clearAllNotifications
                        )
                        1 -> TeamShiftsView(state, viewModel::previousDay, viewModel::nextDay, onJumpToToday = { viewModel.selectDate(LocalDate.now()) })
                        2 -> MyCalendarView(state, viewModel::selectDate, viewModel::previousMonth, viewModel::nextMonth, onNavigateToTeam = { selectedTab = 1 })
                        3 -> MonthRosterView(state, viewModel::previousMonth, viewModel::nextMonth, viewModel::updateSearchQuery)
                        4 -> AccountView(
                            state = state, 
                            onChangePassword = viewModel::changePassword, 
                            onLogout = viewModel::logout,
                            onSaveBaseUrl = viewModel::saveBaseUrl,
                            onTestConnection = viewModel::checkConnection
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardView(
    state: PortalState,
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {}
) {
    val engineer = state.loggedInEngineer
    val roster = getRosterForEngineer(state.liveRoster, engineer)
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val todayShift = roster[today.dayOfMonth.toString()]
    val tomorrowShift = roster[tomorrow.dayOfMonth.toString()]

    var dayShiftsCount = 0
    var nightShiftsCount = 0
    var restDaysCount = 0

    roster.values.forEach { shift ->
        when (shift) {
            is ShiftCode.NS -> nightShiftsCount++
            is ShiftCode.OFF -> restDaysCount++
            is ShiftCode.L -> restDaysCount++
            else -> if (shift.code.startsWith("SW")) restDaysCount++ else dayShiftsCount++
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Welcome Banner
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 20.dp
            ) {
                Column {
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                        color = NeumorphicTokens.AccentSalmon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Welcome, ${engineer?.name ?: "Engineer"}! \uD83D\uDC4B",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "24/7 operational shift overview and live verified team duty briefing.",
                        fontSize = 13.sp,
                        color = NeumorphicTokens.TextMuted
                    )
                }
            }
        }

        // Today & Tomorrow Shift Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VibrantShiftCard(
                    title = "TODAY'S SHIFT (${today.format(DateTimeFormatter.ofPattern("MMM dd"))})",
                    shift = todayShift,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                VibrantShiftCard(
                    title = "TOMORROW'S SHIFT (${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd"))})",
                    shift = tomorrowShift,
                    accentColor = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Duty & Rest Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VibrantMetricsCard(
                    title = "MONTHLY DUTIES",
                    metrics = listOf("DAY SHIFTS" to dayShiftsCount, "NIGHT SHIFTS" to nightShiftsCount),
                    bottomText = "Total Assigned: ${dayShiftsCount + nightShiftsCount} days",
                    accentColor = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
                VibrantMetricsCard(
                    title = "SCHEDULED REST DAYS",
                    metrics = listOf("Weekly Offs" to restDaysCount),
                    bottomText = "Strict fatigue compliance",
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Operational Broadcast Notifications Card (Positioned below dashboard metrics)
        val activeNotifs = state.notifications.mapIndexed { idx, n ->
            val id = n.id ?: "notif_$idx"
            id to n
        }.filter { (id, _) -> id !in state.dismissedNotificationIds }

        if (activeNotifs.isNotEmpty()) {
            val engName = engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Engineer"
            val engUsername = engineer?.username ?: engName.lowercase().replace(" ", "")

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "OPERATIONAL NOTIFICATIONS (${activeNotifs.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }

                    TextButton(
                        onClick = onClearAllNotifications,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.ClearAll,
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(activeNotifs) { (notifId, notif) ->
                val pColor = when (notif.priority?.lowercase()) {
                    "urgent" -> Color(0xFFEF4444)
                    "shift_update" -> Color(0xFFF59E0B)
                    "broadcast" -> Color(0xFF8B5CF6)
                    else -> Color(0xFF3B82F6)
                }

                val resolvedMsg = (notif.rawMessage ?: "")
                    .replace(Regex("\\{\\s*(?:Their\\s+name|their\\s+name|name)\\s*\\}", RegexOption.IGNORE_CASE), engName)
                    .replace(Regex("\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), engUsername)
                    .replace(Regex("@\\s*\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), "@$engUsername")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = pColor.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, pColor.copy(alpha = 0.40f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = pColor.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = (notif.priority ?: "INFO").uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = pColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = notif.title ?: "Broadcast Announcement",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Dismiss / Clear Button
                            IconButton(
                                onClick = { onDismissNotification(notifId) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear Notification",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = resolvedMsg,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "By ${notif.sender ?: "Operations Desk"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (!notif.createdAt.isNullOrBlank()) {
                                Text(
                                    text = notif.createdAt,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VibrantShiftCard(title: String, shift: ShiftCode?, accentColor: Color, modifier: Modifier = Modifier) {
    NeumorphicCard(
        modifier = modifier.height(135.dp),
        shape = RoundedCornerShape(22.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .neumorphicInset(
                            shape = RoundedCornerShape(14.dp),
                            surfaceColor = NeumorphicTokens.Surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shift?.code ?: "--",
                        color = if (shift != null) Color(parseColor(shift.textColorHex)) else NeumorphicTokens.TextMuted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = shift?.name ?: "Rest Day", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = NeumorphicTokens.TextMain)
                    Text(
                        text = if (shift != null && shift !is ShiftCode.OFF) "Active Duty" else "Rest Day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun VibrantMetricsCard(title: String, metrics: List<Pair<String, Int>>, bottomText: String, accentColor: Color, modifier: Modifier = Modifier) {
    NeumorphicCard(
        modifier = modifier.height(135.dp),
        shape = RoundedCornerShape(22.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                metrics.forEach { (label, count) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                        Text(
                            text = count.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }
            }
            Text(
                text = bottomText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphicTokens.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TeamShiftsView(
    state: PortalState, 
    onPreviousDay: () -> Unit, 
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit
) {
    val activeEngineers = remember(state.engineers, state.selectedDate, state.liveRoster) {
        val sorted = sortEngineersByWeekOffs(state.engineers)
        filterEngineersByResignation(sorted, state.selectedDate.year, state.selectedDate.monthValue, state.selectedDate.dayOfMonth, state.liveRoster)
    }

    val today = LocalDate.now()
    val dateTag = when (state.selectedDate) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        today.plusDays(1) -> "TOMORROW"
        else -> null
    }

    val sortedShiftGroups = remember(activeEngineers, state.liveRoster, state.selectedDate) {
        val groups = LinkedHashMap<ShiftCode, MutableList<Engineer>>()
        activeEngineers.forEach { engineer ->
            val engRoster = getRosterForEngineer(state.liveRoster, engineer)
            val shift = engRoster[state.selectedDate.dayOfMonth.toString()] ?: ShiftCode.OFF
            groups.getOrPut(shift) { mutableListOf() }.add(engineer)
        }
        groups.entries.sortedBy { getShiftChronologicalOrder(it.key) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", tint = NeumorphicTokens.TextMain)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dateTag != null) {
                        Text(
                            text = dateTag,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(NeumorphicTokens.AccentSalmon, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = NeumorphicTokens.TextMain
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNextDay) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = NeumorphicTokens.TextMain)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedShiftGroups) { (shift, engineersInShift) ->
                val shiftTiming = ALL_STANDARD_SHIFTS.find { it.first.code == shift.code }?.second ?: "Rest / Duty"
                
                NeumorphicCard(
                    shape = RoundedCornerShape(18.dp),
                    style = NeumorphicElevationStyle.Raised,
                    surfaceColor = NeumorphicTokens.Surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .neumorphicInset(
                                            shape = RoundedCornerShape(10.dp),
                                            surfaceColor = NeumorphicTokens.Surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shift.code,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(parseColor(shift.textColorHex))
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = shift.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = NeumorphicTokens.TextMain
                                    )
                                    Text(
                                        text = shiftTiming,
                                        fontSize = 11.sp,
                                        color = NeumorphicTokens.TextMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeumorphicTokens.AccentSalmon.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${engineersInShift.size} Eng",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = NeumorphicTokens.SurfaceContrast
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            engineersInShift.forEach { engineer ->
                                val isUser = state.loggedInEngineer?.id == engineer.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isUser) NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f) else NeumorphicTokens.SurfaceContrast.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(NeumorphicTokens.AccentSalmon.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = engineer.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = NeumorphicTokens.AccentSalmon
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = engineer.name,
                                            fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isUser) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMain
                                        )
                                        if (isUser) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Star, 
                                                contentDescription = "You", 
                                                tint = Color(0xFFF59E0B), 
                                                modifier = Modifier.size(12.dp)
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
}

@Composable
fun MyCalendarView(
    state: PortalState, 
    onDateSelected: (LocalDate) -> Unit, 
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNavigateToTeam: () -> Unit
) {
    val yearMonth = YearMonth.of(state.gridYear, state.gridMonth)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val engineer = state.loggedInEngineer
    val roster = getRosterForEngineer(state.gridRoster, engineer)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Month Navigation Arrows
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = NeumorphicTokens.TextMain)
                }
                Text(
                    text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeumorphicTokens.TextMain
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = NeumorphicTokens.TextMain)
                }
            }
        }

        // Calendar Grid wrapped in AnimatedContent for smooth month transitions
        item {
            AnimatedContent(
                targetState = yearMonth,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                },
                label = "CalendarMonthTransition"
            ) { currentYM ->
                val currentDaysCount = currentYM.lengthOfMonth()
                val currentFirstDayOfWeek = currentYM.atDay(1).dayOfWeek.value

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    items(daysOfWeek) { day ->
                        Text(
                            text = day,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    items(currentFirstDayOfWeek - 1) {
                        Spacer(modifier = Modifier.size(40.dp))
                    }

                    items((1..currentDaysCount).toList()) { day ->
                        val date = LocalDate.of(currentYM.year, currentYM.monthValue, day)
                        val shift = roster[day.toString()]
                        val isSelected = state.selectedDate == date
                        val isWeekend = date.dayOfWeek.value >= 6

                        val cellBgColor = when {
                            shift != null -> Color(parseColor(shift.bgColorHex))
                            isWeekend -> WeekendLightOrange
                            isSelected -> NeumorphicTokens.AccentSalmon.copy(alpha = 0.25f)
                            else -> NeumorphicTokens.SurfaceContrast
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBgColor)
                                .border(
                                    1.5.dp,
                                    if (isSelected) NeumorphicTokens.AccentSalmon else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onDateSelected(date)
                                    onNavigateToTeam()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (shift != null) Color(parseColor(shift.textColorHex)) else NeumorphicTokens.TextMain
                                )
                                if (shift != null) {
                                    Text(
                                        text = shift.code,
                                        fontSize = 10.sp,
                                        color = Color(parseColor(shift.textColorHex)),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Shift Legend / Shift Ledger
        item {
            NeumorphicCard(
                shape = RoundedCornerShape(22.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "SHIFT LEGEND & TIMINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.AccentSalmon
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = NeumorphicTokens.SurfaceContrast
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ALL_STANDARD_SHIFTS.forEach { (shift, timing) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(parseColor(shift.bgColorHex))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shift.code,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = Color(parseColor(shift.textColorHex))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = shift.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                }
                                Text(
                                    text = timing,
                                    fontSize = 11.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
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
fun MonthRosterView(
    state: PortalState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    val yearMonth = YearMonth.of(state.gridYear, state.gridMonth)
    val daysInMonth = yearMonth.lengthOfMonth()
    val scrollState = rememberScrollState()

    val filteredEngineers = remember(state.engineers, state.searchQuery, state.gridYear, state.gridMonth, state.gridRoster) {
        val sorted = sortEngineersByWeekOffs(state.engineers)
        val active = filterEngineersByResignation(sorted, state.gridYear, state.gridMonth, 1, state.gridRoster)
        if (state.searchQuery.isBlank()) active
        else active.filter { 
            it.name.contains(state.searchQuery, ignoreCase = true) ||
            it.username.contains(state.searchQuery, ignoreCase = true)
        }
    }

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

                    // Search Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(140.dp)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Filter...", 
                                        fontSize = 12.sp, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = onSearchQueryChanged,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (state.searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onSearchQueryChanged("") }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.isGridLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Corner Sticky Header
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

                        // Dates Dual-Header
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
                                    Text(
                                        text = day.toString(), 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 12.sp,
                                        color = if (isWeekend) Color(0xFF7C2D12) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dayOfWeekStr, 
                                        fontSize = 9.sp, 
                                        fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isWeekend) Color(0xFF7C2D12) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Stats Headers
                            val statHeaders = listOf(
                                Triple("WRK", "Working", Color(0xFF10B981)),
                                Triple("NS", "Night", Color(0xFF818CF8)),
                                Triple("OFF", "Rest", Color(0xFFFBBF24))
                            )
                            statHeaders.forEach { (title, subtitle, bgColor) ->
                                Column(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = title, 
                                        fontWeight = FontWeight.ExtraBold, 
                                        fontSize = 12.sp,
                                        color = bgColor
                                    )
                                    Text(
                                        text = subtitle, 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Engineer Matrix Rows
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredEngineers) { engineer ->
                            val isUser = state.loggedInEngineer?.id == engineer.id
                            val engRoster = getRosterForEngineer(state.gridRoster, engineer)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .fillMaxHeight()
                                        .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = engineer.name,
                                            fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isUser) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .horizontalScroll(scrollState)
                                ) {
                                    var wrkCount = 0
                                    var nsCount = 0
                                    var offCount = 0

                                    for (day in 1..daysInMonth) {
                                        val shift = engRoster[day.toString()]
                                        
                                        // Calculate stats
                                        if (shift != null) {
                                            if (shift.code == "NS") nsCount++
                                            else if (shift.code == "OFF" || shift.code == "CO" || shift.code == "AL" || shift.code == "SL" || shift.code == "ML") offCount++
                                            else if (shift.code.isNotEmpty()) wrkCount++
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .fillMaxHeight()
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .background(
                                                    color = if (shift != null) Color(parseColor(shift.bgColorHex)) else Color.Transparent
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (shift != null) {
                                                Text(
                                                    text = shift.code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(parseColor(shift.textColorHex))
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Render Stats Cells
                                    val statCells = listOf(
                                        Pair(wrkCount, Color(0xFF10B981)),
                                        Pair(nsCount, Color(0xFF818CF8)),
                                        Pair(offCount, Color(0xFFFBBF24))
                                    )
                                    statCells.forEach { (count, color) ->
                                        Box(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Daily Coverage Footers (Day & Night)
                        item {
                            val dailyDayCounts = IntArray(daysInMonth + 1)
                            val dailyNightCounts = IntArray(daysInMonth + 1)

                            val minDayStaff = state.constraints.daytimeOverlapMin.coerceAtLeast(1)
                            val targetNight = state.constraints.nightHeadcount.coerceAtLeast(1)

                            for (day in 1..daysInMonth) {
                                var dCount = 0
                                var nCount = 0
                                for (eng in filteredEngineers) {
                                    // APPRENTICE RULE: Apprentice strictly excluded from Day and Night desk coverage!
                                    if (eng.role == EngineerRole.APPRENTICE) continue

                                    val engRoster = getRosterForEngineer(state.gridRoster, eng)
                                    val shift = engRoster[day.toString()]
                                    val code = shift?.code?.trim()?.uppercase() ?: ""
                                    if (code.isNotEmpty()) {
                                        if (code == "NS") {
                                            nCount++
                                        } else if (code != "OFF" && !code.startsWith("SW") && code != "L" && code != "CO" && code != "AL" && code != "SL" && code != "ML") {
                                            dCount++
                                        }
                                    }
                                }
                                dailyDayCounts[day] = dCount
                                dailyNightCounts[day] = nCount
                            }

                            Column {
                                // Day Coverage Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column {
                                            Text("DAY COVERAGE", fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("≥$minDayStaff (Appr. Excl.)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .horizontalScroll(scrollState)
                                    ) {
                                        for (day in 1..daysInMonth) {
                                            val dVal = dailyDayCounts[day]
                                            val dayMet = dVal >= minDayStaff
                                            val dayTextColor = if (dayMet) Color(0xFF047857) else Color(0xFFB45309)
                                            val dayBgColor = if (dayMet) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)

                                            Box(
                                                modifier = Modifier
                                                    .width(42.dp)
                                                    .fillMaxHeight()
                                                    .background(dayBgColor)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dVal.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = dayTextColor
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.width((48 * 3).dp).fillMaxHeight().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
                                    }
                                }

                                // Night Coverage Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column {
                                            Text("NIGHT COVERAGE", fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("=$targetNight NS (Appr. Excl.)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .horizontalScroll(scrollState)
                                    ) {
                                        for (day in 1..daysInMonth) {
                                            val nVal = dailyNightCounts[day]
                                            val (nightTextColor, nightBgColor) = when {
                                                nVal == 0 -> Pair(Color(0xFFB91C1C), Color(0xFFFEE2E2)) // Gap
                                                nVal != targetNight -> Pair(Color(0xFFB45309), Color(0xFFFEF3C7)) // Over/under warning
                                                else -> Pair(Color(0xFF047857), Color(0xFFD1FAE5)) // Target met
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(42.dp)
                                                    .fillMaxHeight()
                                                    .background(nightBgColor)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = nVal.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = nightTextColor
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.width((48 * 3).dp).fillMaxHeight().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
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

@Composable
fun AccountView(
    state: PortalState,
    onChangePassword: (String) -> Unit,
    onLogout: () -> Unit,
    onSaveBaseUrl: (String) -> Unit = {},
    onTestConnection: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val engineer = state.loggedInEngineer

    var showPasswordChange by remember { mutableStateOf(false) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (engineer?.name ?: "U").take(2).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = engineer?.name ?: "Unknown User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = (engineer?.role?.name ?: "ENGINEER").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Account Details (with Phone Number per Engineer!)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ACCOUNT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    ProfileDetailRow("Domain Username", engineer?.username ?: "--")
                    ProfileDetailRow("Registered Email", if (!engineer?.email.isNullOrBlank()) engineer!!.email else "--")
                    ProfileDetailRow("Contact Phone", if (!engineer?.phone.isNullOrBlank()) engineer!!.phone else "--")
                    ProfileDetailRow("Weekly Offs", if (!engineer?.defaultWeeklyOffs.isNullOrEmpty()) engineer!!.defaultWeeklyOffs.joinToString(", ") else "None")
                }
            }
        }

        // Server & Public Gateway Configuration Card (Accessible to All Logged In Accounts)
        item {
            var showGatewaySettings by remember { mutableStateOf(true) }
            var urlInput by remember(state.currentBaseUrl) { mutableStateOf(state.currentBaseUrl) }
            val activeNetworkName = remember(state.isConnected) { NetworkUtils.getActiveNetworkDescription(context) }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGatewaySettings = !showGatewaySettings },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SERVER & PUBLIC GATEWAY SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(
                            imageVector = if (showGatewaySettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showGatewaySettings) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            // Status and Active Network
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Connection Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (state.isConnected) "ONLINE (HTTP 200)" else "OFFLINE",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Active Network", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeNetworkName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Current Active Endpoint with Copy
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Active Gateway URL:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = state.currentBaseUrl,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(state.currentBaseUrl))
                                            Toast.makeText(context, "Copied URL to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Server Base URL", fontSize = 12.sp) },
                                placeholder = { Text("https://xxx.trycloudflare.com/api/", fontSize = 12.sp) },
                                trailingIcon = {
                                    if (urlInput.isNotBlank()) {
                                        IconButton(onClick = { urlInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val renderUrl = "https://rosterx.onrender.com/api/"
                                        urlInput = renderUrl
                                        onSaveBaseUrl(renderUrl)
                                        Toast.makeText(context, "Switched to Render Cloud", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Render", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val assetUrl = context.assets.open("server_url.txt").bufferedReader().use { it.readText().trim() }
                                            if (assetUrl.isNotBlank()) {
                                                var clean = assetUrl
                                                if (!clean.endsWith("/")) clean += "/"
                                                if (!clean.endsWith("/api/")) clean += "api/"
                                                urlInput = clean
                                                onSaveBaseUrl(clean)
                                                Toast.makeText(context, "Switched to Cloudflare Tunnel", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Default asset not found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cloudflare", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Button(
                                onClick = {
                                    onSaveBaseUrl(urlInput)
                                    Toast.makeText(context, "Saved & Reconnecting...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & Connect Custom URL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Change Password Section
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordChange = !showPasswordChange },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SECURITY & PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(
                            imageVector = if (showPasswordChange) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showPasswordChange) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            if (state.successMessage != null) {
                                Text(state.successMessage!!, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = currentPass,
                                onValueChange = { currentPass = it },
                                label = { Text("Current Password", fontSize = 12.sp) },
                                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                label = { Text("New Password", fontSize = 12.sp) },
                                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it },
                                label = { Text("Confirm New Password", fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { passVisible = !passVisible }) {
                                        Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                    }
                                },
                                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = { 
                                    if (newPass == confirmPass) {
                                        onChangePassword(newPass)
                                    }
                                },
                                enabled = newPass.isNotBlank() && newPass == confirmPass,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Update Password", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }



        // Application Specs & Build Version Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ABOUT ROSTERX & SPECS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    ProfileDetailRow("Application Name", "Project RosterX")
                    ProfileDetailRow("Android App Version", "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                    ProfileDetailRow("Backend Server Version", "v1.7.3")
                    ProfileDetailRow("Mobile Architecture", "Native Android (Jetpack Compose)")
                    ProfileDetailRow("Database Engine", "MongoDB Atlas Cluster0")
                    ProfileDetailRow("Public Edge Gateway", "Render Cloud & Cloudflare")
                }
            }
        }

        // Sign Out Button
        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", modifier = Modifier.padding(end = 8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}
