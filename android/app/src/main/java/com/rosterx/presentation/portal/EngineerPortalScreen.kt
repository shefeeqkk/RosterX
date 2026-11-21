package com.rosterx.presentation.portal

import com.rosterx.presentation.portal.credentials.CredentialTrackerScreen
import com.rosterx.presentation.portal.more.MoreHubView
import com.rosterx.presentation.portal.more.MyTeamView
import com.rosterx.presentation.components.RealtimeSwipeableTabPager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import android.graphics.Color.parseColor
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.rosterx.widget.RosterXAppWidgetProvider

import android.widget.Toast

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.Canvas

import androidx.compose.animation.core.Spring

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.runtime.*

import androidx.compose.runtime.saveable.rememberSaveable

import android.content.res.Configuration

import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.BlurMaskFilter

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

import com.rosterx.data.local.AppThemeMode
import com.rosterx.data.remote.isExpiredCelebration
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.domain.model.isManagementAccount
import com.rosterx.domain.model.isEligibleForShiftRoster
import com.rosterx.domain.model.isEligibleForMonthRoster
import com.rosterx.domain.model.parseResignationDate
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.admin.tabs.NotificationsTab
import com.rosterx.ui.neumorphic.BrandPalettes
import com.rosterx.ui.neumorphic.DefaultPalette
import com.rosterx.ui.theme.LocalIsDarkTheme
import com.rosterx.ui.theme.LocalThemeAccent
import com.rosterx.ui.theme.LocalThemeMode
import com.rosterx.ui.theme.LocalThemePalette

import com.rosterx.util.NetworkUtils

import com.rosterx.ui.neumorphic.*

import com.rosterx.presentation.components.*

import com.rosterx.domain.model.Engineer

import com.rosterx.domain.model.ShiftCode

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.LazyRow

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
    eng.displayOrder?.let { return it }
    val digits = Regex("\\d+").find(eng.id)?.value?.toIntOrNull()
    return digits ?: Int.MAX_VALUE
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
    val targetYearMonth = try { java.time.YearMonth.of(year, month) } catch (_: Exception) { java.time.YearMonth.now() }

    return engineers.filter { eng ->
        if (eng.isManagementAccount) return@filter false
        if (eng.status == EngineerStatus.RESIGNED) {
            val resignDate = parseResignationDate(eng.resignationDate) ?: parseFlexibleDate(eng.resignationDate)
            if (resignDate == null) return@filter false
            val resignYearMonth = java.time.YearMonth.from(resignDate)
            // Strict rule: after resignation month, exclude completely
            if (targetYearMonth.isAfter(resignYearMonth)) return@filter false
            if (day > 1 && checkDate.isAfter(resignDate)) return@filter false
        }
        true
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



@Composable
fun Photorealistic3DBell(
    modifier: Modifier = Modifier,
    clapperAngle: Float = 0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // 1. Top Hanger Ring
        val ringRadius = w * 0.12f
        val ringCenterY = h * 0.13f
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFE082),
                    Color(0xFFD97706),
                    Color(0xFF78350F)
                ),
                startY = ringCenterY - ringRadius,
                endY = ringCenterY + ringRadius
            ),
            radius = ringRadius,
            center = Offset(cx, ringCenterY),
            style = Stroke(width = w * 0.06f)
        )

        // 2. Clapper (Spherical bell hammer swinging at bottom)
        val clapperRad = w * 0.11f
        val clapperRadAngle = Math.toRadians(clapperAngle.toDouble())
        val clapperOffsetX = (Math.sin(clapperRadAngle) * (h * 0.18f)).toFloat()
        val clapperY = h * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9C4),
                    Color(0xFFF59E0B),
                    Color(0xFF92400E),
                    Color(0xFF451A03)
                ),
                center = Offset(cx + clapperOffsetX - clapperRad * 0.3f, clapperY - clapperRad * 0.3f),
                radius = clapperRad * 1.2f
            ),
            radius = clapperRad,
            center = Offset(cx + clapperOffsetX, clapperY)
        )

        // 3. Bell Body (Dome with flared rim)
        val bellPath = Path().apply {
            moveTo(cx - w * 0.16f, h * 0.22f)
            // Left curve downwards to flare
            cubicTo(
                cx - w * 0.18f, h * 0.40f,
                cx - w * 0.26f, h * 0.62f,
                cx - w * 0.44f, h * 0.77f
            )
            // Left rim flare lip
            quadraticBezierTo(
                cx - w * 0.46f, h * 0.80f,
                cx - w * 0.42f, h * 0.82f
            )
            // Bottom rim curve towards right
            cubicTo(
                cx - w * 0.20f, h * 0.85f,
                cx + w * 0.20f, h * 0.85f,
                cx + w * 0.42f, h * 0.82f
            )
            // Right rim flare lip
            quadraticBezierTo(
                cx + w * 0.46f, h * 0.80f,
                cx + w * 0.44f, h * 0.77f
            )
            // Right curve upwards to crown
            cubicTo(
                cx + w * 0.26f, h * 0.62f,
                cx + w * 0.18f, h * 0.40f,
                cx + w * 0.16f, h * 0.22f
            )
            // Top crown cap
            quadraticBezierTo(
                cx, h * 0.18f,
                cx - w * 0.16f, h * 0.22f
            )
            close()
        }

        // Draw 3D Metallic Golden Body Gradient
        drawPath(
            path = bellPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF78350F), // Bronze deep edge
                    Color(0xFFB45309), // Warm shadow
                    Color(0xFFF59E0B), // Rich golden base
                    Color(0xFFFEF08A), // Specular golden highlight ridge
                    Color(0xFFFBBF24), // Vibrant amber gold
                    Color(0xFFD97706), // Mid tone
                    Color(0xFF78350F)  // Right bronze shadow
                ),
                start = Offset(cx - w * 0.46f, h * 0.5f),
                end = Offset(cx + w * 0.46f, h * 0.5f)
            )
        )

        // 4. Bottom Rim Lip 3D Thickness Band
        val rimBandPath = Path().apply {
            moveTo(cx - w * 0.42f, h * 0.77f)
            cubicTo(
                cx - w * 0.20f, h * 0.81f,
                cx + w * 0.20f, h * 0.81f,
                cx + w * 0.42f, h * 0.77f
            )
            quadraticBezierTo(
                cx + w * 0.44f, h * 0.80f,
                cx + w * 0.42f, h * 0.82f
            )
            cubicTo(
                cx + w * 0.20f, h * 0.86f,
                cx - w * 0.20f, h * 0.86f,
                cx - w * 0.42f, h * 0.82f
            )
            close()
        }
        drawPath(
            path = rimBandPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFEF3C7),
                    Color(0xFFB45309)
                ),
                startY = h * 0.76f,
                endY = h * 0.86f
            )
        )

        // 5. Specular Vertical Curved Glint Highlight (Gloss effect)
        val glintPath = Path().apply {
            moveTo(cx - w * 0.08f, h * 0.25f)
            cubicTo(
                cx - w * 0.12f, h * 0.42f,
                cx - w * 0.18f, h * 0.60f,
                cx - w * 0.28f, h * 0.73f
            )
        }
        drawPath(
            path = glintPath,
            color = Color.White.copy(alpha = 0.55f),
            style = Stroke(
                width = w * 0.055f,
                cap = StrokeCap.Round
            )
        )

        // 6. Top Crown Specular Shimmer
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = w * 0.04f,
            center = Offset(cx - w * 0.05f, h * 0.22f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

@Composable

fun EngineerPortalScreen(

    viewModel: EngineerPortalViewModel = hiltViewModel(),
    pendingDeepLink: com.rosterx.AppDeepLinkTarget? = null,
    onConsumeDeepLink: () -> Unit = {},
    appUpdateInfo: AppUpdateInfo? = null,
    onDismissUpdateBanner: () -> Unit = {}

) {

    val state by viewModel.state.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedTab) { 5 }
    val coroutineScope = rememberCoroutineScope()
    val boundaryOverscrollAnim = remember { Animatable(0f) }
    var boundaryOverscrollPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState.settledPage) {
        if (selectedTab != pagerState.settledPage) {
            selectedTab = pagerState.settledPage
        }
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    val continuousTabPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    var isViewingSettings by rememberSaveable { mutableStateOf(false) }
    var isViewingCredentialTracker by rememberSaveable { mutableStateOf(false) }
    var isViewingMyTeam by rememberSaveable { mutableStateOf(false) }
    var previousTab by rememberSaveable { mutableIntStateOf(0) }
    var showUnifiedEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickNotificationsSheet by rememberSaveable { mutableStateOf(false) }
    var showCelebrationDialog by rememberSaveable { mutableStateOf(false) }
    var activeCelebrationType by rememberSaveable { mutableStateOf(CelebrationType.BIRTHDAY) }
    var activeCelebrationYears by rememberSaveable { mutableIntStateOf(1) }
    var celebrationDialogTargetUser by rememberSaveable { mutableStateOf("") }
    var celebrationDialogTargetName by rememberSaveable { mutableStateOf("") }
    var celebrationDialogType by rememberSaveable { mutableStateOf(CelebrationType.BIRTHDAY) }
    var celebrationDialogYears by rememberSaveable { mutableIntStateOf(1) }
    var celebrationDialogPhone by rememberSaveable { mutableStateOf("") }
    var isCelebrationSelf by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = isViewingCredentialTracker || isViewingMyTeam || isViewingSettings) {
        if (isViewingCredentialTracker) {
            isViewingCredentialTracker = false
        } else if (isViewingMyTeam) {
            isViewingMyTeam = false
        } else if (isViewingSettings) {
            isViewingSettings = false
            selectedTab = previousTab
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refreshAllData {
                pullRefreshState.endRefresh()
            }
        }
    }

    val portalContext = LocalContext.current
    LaunchedEffect(state.loggedInEngineer) {
        val eng = state.loggedInEngineer ?: return@LaunchedEffect
        val today = LocalDate.now()
        val todayMd = String.format("%02d-%02d", today.monthValue, today.dayOfMonth)
        val todayStr = today.toString()
        val uname = eng.username.ifBlank { eng.email.substringBefore("@") }

        val bday = (eng.birthday ?: "").trim().replace("/", "-")
        val jDate = (eng.joinDate ?: "").trim().replace("/", "-")

        val isBirthday = bday.endsWith(todayMd) || (bday.length >= 5 && bday.takeLast(5) == todayMd)
        var isAnniversary = false
        var years = 1
        if (jDate.endsWith(todayMd) || (jDate.length >= 5 && jDate.takeLast(5) == todayMd)) {
            try {
                if (jDate.length >= 4 && jDate.take(4).all { it.isDigit() }) {
                    val jYear = jDate.take(4).toInt()
                    val diff = today.year - jYear
                    if (diff >= 1) {
                        isAnniversary = true
                        years = diff
                    }
                }
            } catch (_: Exception) {}
        }

        if (isBirthday || isAnniversary) {
            val prefs = portalContext.getSharedPreferences("rosterx_celebrations", android.content.Context.MODE_PRIVATE)
            val prefKey = "seen_${uname}_${todayStr}"
            val alreadySeen = prefs.getBoolean(prefKey, false)
            if (isBirthday && isAnniversary) {
                activeCelebrationType = CelebrationType.BOTH
                activeCelebrationYears = years
            } else if (isBirthday) {
                activeCelebrationType = CelebrationType.BIRTHDAY
                activeCelebrationYears = 0
            } else {
                activeCelebrationType = CelebrationType.ANNIVERSARY
                activeCelebrationYears = years
            }
            val properName = (eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name).toProperCase()
            celebrationDialogTargetUser = uname
            celebrationDialogTargetName = properName
            celebrationDialogType = activeCelebrationType
            celebrationDialogYears = activeCelebrationYears
            celebrationDialogPhone = eng.phone.orEmpty()
            isCelebrationSelf = true
            if (!alreadySeen) {
                showCelebrationDialog = true
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isManager = state.loggedInEngineer?.role == EngineerRole.MANAGER

    LaunchedEffect(pendingDeepLink, isManager) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        when (target) {
            com.rosterx.AppDeepLinkTarget.ROSTER -> {
                isViewingSettings = false
                isViewingCredentialTracker = false
                isViewingMyTeam = false
                selectedTab = if (isManager) 2 else 3
            }
            com.rosterx.AppDeepLinkTarget.NOTIFICATIONS -> {
                isViewingSettings = false
                isViewingCredentialTracker = false
                isViewingMyTeam = false
                if (isManager) {
                    selectedTab = 3
                } else {
                    showQuickNotificationsSheet = true
                }
            }
            com.rosterx.AppDeepLinkTarget.CREDENTIALS -> {
                isViewingSettings = false
                isViewingMyTeam = false
                isViewingCredentialTracker = true
            }
            com.rosterx.AppDeepLinkTarget.PROFILE -> {
                isViewingCredentialTracker = false
                isViewingMyTeam = false
                previousTab = selectedTab
                isViewingSettings = true
            }
            com.rosterx.AppDeepLinkTarget.HOME -> {
                isViewingSettings = false
                isViewingCredentialTracker = false
                isViewingMyTeam = false
                selectedTab = 0
            }
        }
        onConsumeDeepLink()
    }
    val isFullScreenRoster = isLandscape && ((isManager && selectedTab == 2) || (!isManager && selectedTab == 3))

    LaunchedEffect(selectedTab, isViewingSettings, isViewingCredentialTracker, isViewingMyTeam, isManager) {
        val screenName = if (isViewingCredentialTracker) {
            "Credential Tracker"
        } else if (isViewingMyTeam) {
            "My Team Directory"
        } else if (isViewingSettings) {
            "Profile & Settings"
        } else if (isManager) {
            when (selectedTab) {
                0 -> "Manager Dashboard"
                1 -> "Team Shifts"
                2 -> "Month Roster"
                3 -> "Broadcast Console"
                4 -> "Operations Hub"
                else -> "Manager Dashboard"
            }
        } else {
            when (selectedTab) {
                0 -> "Dashboard"
                1 -> "Team Shifts"
                2 -> "My Calendar"
                3 -> "Month Roster"
                4 -> "Operations Hub"
                else -> "Dashboard"
            }
        }
        viewModel.analyticsManager.logScreenView(screenName)
        viewModel.analyticsManager.logEvent("tab_switched", mapOf(
            "tab_name" to screenName,
            "role" to (state.loggedInEngineer?.role?.name ?: "UNKNOWN")
        ))
    }



    val showPortalChrome = !isFullScreenRoster && !isViewingSettings && !isViewingCredentialTracker && !isViewingMyTeam
    val portalNavItems = remember(isManager) {
        if (isManager) {
            listOf(
                NavTabItem(0, "Dashboard", Icons.Default.Dashboard),
                NavTabItem(1, "Team Shifts", Icons.Default.Group),
                NavTabItem(2, "Month Roster", Icons.Default.GridView),
                NavTabItem(3, "Broadcast", Icons.Default.Campaign),
                NavTabItem(4, "More", Icons.Default.MoreHoriz)
            )
        } else {
            listOf(
                NavTabItem(0, "Dashboard", Icons.Default.Dashboard),
                NavTabItem(1, "Team Shifts", Icons.Default.Group),
                NavTabItem(2, "My Calendar", Icons.Default.CalendarMonth),
                NavTabItem(3, "Month Roster", Icons.Default.GridView),
                NavTabItem(4, "More", Icons.Default.MoreHoriz)
            )
        }
    }

    Scaffold(
        containerColor = NeumorphicTokens.Canvas,
        topBar = {

            if (!isFullScreenRoster && !isViewingCredentialTracker && !isViewingMyTeam) {
                if (isLandscape) {
                    // Ultra-slim 42dp Landscape Top Bar
                    val isManagementAccountUser = state.loggedInEngineer?.isManagementAccount == true ||
                        state.loggedInEngineer?.role == EngineerRole.ADMIN ||
                        state.loggedInEngineer?.role == EngineerRole.MANAGER
                    val unreadCount = if (isManagementAccountUser) 0 else state.notifications.count {
                        !it.isExpiredCelebration() && (it.id ?: "") !in state.dismissedNotificationIds
                    }
                    Surface(
                        color = NeumorphicTokens.Canvas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                                .height(42.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isViewingSettings) {
                                    IconButton(
                                        onClick = {
                                            isViewingSettings = false
                                            selectedTab = previousTab
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Tabs",
                                            tint = NeumorphicTokens.AccentSalmon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Profile & Settings",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = NeumorphicTokens.AccentSalmon
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_rosterx_logo),
                                        contentDescription = "RosterX Logo",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "RosterX",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = NeumorphicTokens.AccentSalmon
                                    )
                                }
                            }

                            if (!isViewingSettings) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!isManagementAccountUser) {
                                        Box {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showQuickNotificationsSheet = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Photorealistic3DBell(
                                                    modifier = Modifier.size(21.dp),
                                                    clapperAngle = 0f
                                                )
                                            }
                                            if (unreadCount > 0) {
                                                val isSingleDigit = unreadCount < 10
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFDC2626),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                                                    shadowElevation = 2.dp,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 2.dp, y = (-1).dp)
                                                        .height(15.dp)
                                                        .defaultMinSize(minWidth = if (isSingleDigit) 15.dp else 20.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(horizontal = if (isSingleDigit) 0.dp else 4.dp)
                                                            .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                            color = Color.White,
                                                            style = TextStyle(
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                                textAlign = TextAlign.Center
                                                            ),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (state.loggedInEngineer != null && selectedTab != 4) {
                                        NeumorphicCard(
                                            modifier = Modifier.clickable {
                                                previousTab = selectedTab
                                                isViewingCredentialTracker = false
                                                isViewingMyTeam = false
                                                isViewingSettings = true
                                            },
                                            shape = CircleShape,
                                            style = NeumorphicElevationStyle.RaisedSubtle,
                                            surfaceColor = NeumorphicTokens.Surface,
                                            contentPadding = 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UserAvatar(
                                                    name = state.loggedInEngineer!!.name,
                                                    profilePicture = state.loggedInEngineer!!.profilePicture,
                                                    size = 20.dp,
                                                    backgroundColor = NeumorphicTokens.AccentSalmon.copy(alpha = 0.2f),
                                                    textColor = NeumorphicTokens.AccentSalmon,
                                                    fontSize = 9.sp
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = state.loggedInEngineer!!.name.toProperCase().split(" ")[0],
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = NeumorphicTokens.TextMain
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                TopAppBar(
                navigationIcon = {
                    if (isViewingSettings) {
                        IconButton(
                            onClick = {
                                isViewingSettings = false
                                selectedTab = previousTab
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Tabs",
                                tint = NeumorphicTokens.AccentSalmon
                            )
                        }
                    }
                },
                title = { 
                    if (isViewingSettings) {
                        Text(
                            text = "Profile & Settings",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = NeumorphicTokens.AccentSalmon
                        )
                    } else {
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
                    }
                },

                actions = {
                    if (!isViewingSettings) {
                    val isManagementAccountUser = state.loggedInEngineer?.isManagementAccount == true ||
                        state.loggedInEngineer?.role == EngineerRole.ADMIN ||
                        state.loggedInEngineer?.role == EngineerRole.MANAGER
                    val unreadCount = if (isManagementAccountUser) 0 else state.notifications.count {
                        !it.isExpiredCelebration() && (it.id ?: "") !in state.dismissedNotificationIds
                    }

                    // Natural lively bell ringing animation with clapper physics & rest cadence
                    val bellInfiniteTransition = rememberInfiniteTransition(label = "bell_wiggle")
                    val bellRotation by if (unreadCount > 0) {
                        bellInfiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = keyframes {
                                    durationMillis = 2400
                                    0f at 0
                                    -18f at 80 using FastOutSlowInEasing
                                    16f at 180 using FastOutSlowInEasing
                                    -14f at 280 using FastOutSlowInEasing
                                    10f at 380 using FastOutSlowInEasing
                                    -6f at 480 using FastOutSlowInEasing
                                    3f at 580 using FastOutSlowInEasing
                                    0f at 680 using FastOutSlowInEasing
                                    0f at 2400
                                },
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "bell_rotation"
                        )
                    } else {
                        remember { mutableStateOf(0f) }
                    }

                    val clapperRotation by if (unreadCount > 0) {
                        bellInfiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = keyframes {
                                    durationMillis = 2400
                                    0f at 0
                                    12f at 100 using FastOutSlowInEasing
                                    -12f at 200 using FastOutSlowInEasing
                                    9f at 300 using FastOutSlowInEasing
                                    -6f at 400 using FastOutSlowInEasing
                                    3f at 500 using FastOutSlowInEasing
                                    0f at 680 using FastOutSlowInEasing
                                    0f at 2400
                                },
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "clapper_rotation"
                        )
                    } else {
                        remember { mutableStateOf(0f) }
                    }

                    // 3D Notification Bell (Duty engineers only - excluded for Admin & Manager)
                    if (!isManagementAccountUser) {
                        Box(modifier = Modifier.padding(end = 6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { showQuickNotificationsSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Photorealistic3DBell(
                                    modifier = Modifier
                                        .size(27.dp)
                                        .graphicsLayer {
                                            rotationZ = bellRotation
                                            transformOrigin = TransformOrigin(0.5f, 0.12f)
                                        },
                                    clapperAngle = clapperRotation
                                )
                            }
                            if (unreadCount > 0) {
                                val isSingleDigit = unreadCount < 10
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDC2626), // Vibrant Crimson
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-2).dp)
                                        .height(18.dp)
                                        .defaultMinSize(minWidth = if (isSingleDigit) 18.dp else 24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = if (isSingleDigit) 0.dp else 5.dp)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                            color = Color.White,
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                textAlign = TextAlign.Center
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.loggedInEngineer != null && !isViewingMyTeam && !isViewingCredentialTracker && selectedTab != 4) {
                        NeumorphicCard(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    previousTab = selectedTab
                                    isViewingCredentialTracker = false
                                    isViewingMyTeam = false
                                    isViewingSettings = true
                                },
                            shape = CircleShape,
                            style = NeumorphicElevationStyle.RaisedSubtle,
                            surfaceColor = NeumorphicTokens.Surface,
                            contentPadding = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = state.loggedInEngineer!!.name,
                                    profilePicture = state.loggedInEngineer!!.profilePicture,
                                    size = 24.dp,
                                    backgroundColor = NeumorphicTokens.AccentSalmon.copy(alpha = 0.2f),
                                    textColor = NeumorphicTokens.AccentSalmon,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = state.loggedInEngineer!!.name.toProperCase().split(" ")[0],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeumorphicTokens.TextMain
                                )
                            }
                        }
                    }
                    } // end if (!isViewingSettings)
                },

                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = NeumorphicTokens.Canvas,

                    titleContentColor = NeumorphicTokens.TextMain

                )

            )
                }

            }

        },

        floatingActionButton = {

            if (!isViewingSettings && selectedTab == 1 && state.selectedDate != LocalDate.now()) {

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

            if (!isLandscape && showPortalChrome) {
                FluidNeumorphicBottomNav(
                    items = portalNavItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { tab ->
                        if ((isManager && (tab == 1 || tab == 2)) || (!isManager && (tab == 1 || tab == 2))) {
                            viewModel.resetToCurrentDateAndMonth()
                        }
                        selectedTab = tab
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tab)
                        }
                    },
                    fractionalPosition = continuousTabPosition,
                    boundaryOverscrollOffsetPx = boundaryOverscrollPx,
                    isDragging = pagerState.isScrollInProgress || boundaryOverscrollPx != 0f
                )
            }

        }

    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (!isFullScreenRoster && !isViewingCredentialTracker && !isViewingMyTeam) paddingValues.calculateTopPadding() else WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
                )
                .background(NeumorphicTokens.Canvas)
        ) {
            if (isLandscape && showPortalChrome) {
                com.rosterx.presentation.components.FluidNeumorphicNavRail(
                    items = portalNavItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { tab ->
                        if ((isManager && (tab == 1 || tab == 2)) || (!isManager && (tab == 1 || tab == 2))) {
                            viewModel.resetToCurrentDateAndMonth()
                        }
                        selectedTab = tab
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isLandscape && !isFullScreenRoster) {
                            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                        } else {
                            Modifier
                        }
                    )
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {

            if (state.isLoading && state.engineers.isEmpty()) {

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

                val openSelfCelebration = {
                    val eng = state.loggedInEngineer
                    val myUname = eng?.username?.ifBlank { eng.email.substringBefore("@") }.orEmpty()
                    val myName = (eng?.fullName?.takeIf { it.isNotBlank() } ?: eng?.name.orEmpty()).toProperCase()
                    celebrationDialogTargetUser = myUname
                    celebrationDialogTargetName = myName
                    celebrationDialogType = activeCelebrationType
                    celebrationDialogYears = activeCelebrationYears
                    celebrationDialogPhone = eng?.phone.orEmpty()
                    isCelebrationSelf = true
                    showCelebrationDialog = true
                }

                val activeSubView = if (isViewingCredentialTracker) 1 else if (isViewingMyTeam) 2 else if (isViewingSettings) 3 else 0

                AnimatedContent(
                    targetState = activeSubView,
                    transitionSpec = {
                        if (targetState != 0 && initialState == 0) {
                            slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(animationSpec = tween(200))
                        } else if (targetState == 0 && initialState != 0) {
                            slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(200)) togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        }
                    },
                    label = "ScreenTransition"
                ) { subView ->
                    if (subView == 1) {
                        CredentialTrackerScreen(
                            state = state,
                            viewModel = viewModel,
                            onBackClick = { isViewingCredentialTracker = false }
                        )
                    } else if (subView == 2) {
                        MyTeamView(
                            engineers = state.engineers,
                            onBackClick = { isViewingMyTeam = false }
                        )
                    } else if (subView == 3) {
                        AccountView(
                            state = state,
                            onOpenEditProfile = { showUnifiedEditProfileDialog = true },
                            onLogout = viewModel::logout,
                            onSaveBaseUrl = viewModel::saveBaseUrl,
                            onTestConnection = viewModel::checkConnection,
                            onSetThemeMode = viewModel::setThemeMode,
                            onSetThemePalette = viewModel::setThemePaletteId,
                            onOpenCelebrationWishes = openSelfCelebration
                        )
                    } else {
                        val isRosterTab = (isManager && selectedTab == 2) || (!isManager && selectedTab == 3)
                        RealtimeSwipeableTabPager(
                            pagerState = pagerState,
                            isRosterTab = isRosterTab,
                            isSuspended = isViewingSettings || isViewingCredentialTracker || isViewingMyTeam || showCelebrationDialog || showQuickNotificationsSheet,
                            overscrollAnim = boundaryOverscrollAnim,
                            onBoundaryOverscroll = { boundaryOverscrollPx = it },
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            if (isManager) {
                                when (tab) {
                                    0 -> Column(modifier = Modifier.fillMaxSize()) {
                                        if (appUpdateInfo != null) {
                                            AppUpdateBannerCard(
                                                updateInfo = appUpdateInfo,
                                                onDismiss = onDismissUpdateBanner,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            DashboardView(
                                                state = state,
                                                onDismissNotification = viewModel::dismissNotification,
                                                onClearAllNotifications = viewModel::clearAllNotifications,
                                                onRefreshDiagnostics = viewModel::refreshDiagnostics,
                                                onOpenCelebrationWishes = openSelfCelebration,
                                                onDismissCelebration = viewModel::dismissCelebration
                                            )
                                        }
                                    }
                                    1 -> TeamShiftsView(state, viewModel::previousDay, viewModel::nextDay, onJumpToToday = { viewModel.selectDate(LocalDate.now()) })
                                    2 -> MonthRosterView(state, viewModel::previousMonth, viewModel::nextMonth, viewModel::updateSearchQuery)
                                    3 -> NotificationsTab(
                                        state = AdminRosterState(
                                            engineers = state.engineers,
                                            broadcastTemplates = state.broadcastTemplates,
                                            isSendingNotification = state.isSendingBroadcast,
                                            notifications = state.broadcastHistory
                                        ),
                                        onSendNotification = { title, message, target, priority, sendEmail, sendInApp, sendPush, onComplete ->
                                            viewModel.sendBroadcast(title, message, target, priority, sendEmail, sendInApp, sendPush, onComplete)
                                        },
                                        onRetractNotification = { notifId, onComplete ->
                                            viewModel.retractBroadcast(notifId, onComplete)
                                        },
                                        onRefreshNotifications = viewModel::loadBroadcastHistory,
                                        onSaveTemplate = viewModel::saveBroadcastTemplate,
                                        onDeleteTemplate = viewModel::deleteBroadcastTemplate
                                    )
                                    4 -> MoreHubView(
                                        state = state,
                                        isAdmin = false,
                                        onNavigateToCredentialTracker = { isViewingCredentialTracker = true },
                                        onNavigateToMyTeam = { isViewingMyTeam = true }
                                    )
                                    else -> DashboardView(
                                        state = state,
                                        onDismissNotification = viewModel::dismissNotification,
                                        onClearAllNotifications = viewModel::clearAllNotifications,
                                        onRefreshDiagnostics = viewModel::refreshDiagnostics,
                                        onOpenCelebrationWishes = openSelfCelebration,
                                        onDismissCelebration = viewModel::dismissCelebration
                                    )
                                }
                            } else {
                                when (tab) {
                                    0 -> Column(modifier = Modifier.fillMaxSize()) {
                                        if (appUpdateInfo != null) {
                                            AppUpdateBannerCard(
                                                updateInfo = appUpdateInfo,
                                                onDismiss = onDismissUpdateBanner,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            DashboardView(
                                                state = state,
                                                onDismissNotification = viewModel::dismissNotification,
                                                onClearAllNotifications = viewModel::clearAllNotifications,
                                                onRefreshDiagnostics = viewModel::refreshDiagnostics,
                                                onOpenCelebrationWishes = openSelfCelebration,
                                                onDismissCelebration = viewModel::dismissCelebration
                                            )
                                        }
                                    }
                                    1 -> TeamShiftsView(state, viewModel::previousDay, viewModel::nextDay, onJumpToToday = { viewModel.selectDate(LocalDate.now()) })
                                    2 -> MyCalendarView(
                                        state = state,
                                        onDateSelected = viewModel::selectDate,
                                        onPreviousMonth = viewModel::previousMonth,
                                        onNextMonth = viewModel::nextMonth,
                                        onNavigateToTeam = { selectedTab = 1 },
                                        onSyncRecorded = viewModel::recordCalendarSync,
                                        onSyncCleared = viewModel::clearCalendarSync,
                                        onCalendarAccountsDiscovered = viewModel::syncDeviceCalendarAccounts
                                    )
                                    3 -> MonthRosterView(state, viewModel::previousMonth, viewModel::nextMonth, viewModel::updateSearchQuery)
                                    4 -> MoreHubView(
                                        state = state,
                                        isAdmin = false,
                                        onNavigateToCredentialTracker = { isViewingCredentialTracker = true },
                                        onNavigateToMyTeam = { isViewingMyTeam = true }
                                    )
                                    else -> DashboardView(
                                        state = state,
                                        onDismissNotification = viewModel::dismissNotification,
                                        onClearAllNotifications = viewModel::clearAllNotifications,
                                        onRefreshDiagnostics = viewModel::refreshDiagnostics,
                                        onOpenCelebrationWishes = openSelfCelebration,
                                        onDismissCelebration = viewModel::dismissCelebration
                                    )
                                }
                            }
                        }
                    }
                }

            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (showPortalChrome && selectedTab == 0) {
                com.rosterx.presentation.components.FloatingAiBotButton(
                    onClick = { viewModel.openAssistant() },
                    size = if (isLandscape) 44.dp else 52.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = if (isLandscape) 14.dp else 16.dp,
                            bottom = if (isLandscape) 14.dp else 96.dp
                        )
                )
            }

            }
        }

    }

    com.rosterx.presentation.components.RosterXAssistantBottomSheet(
        isOpen = state.isAssistantOpen,
        onDismiss = { viewModel.closeAssistant() },
        messages = state.assistantMessages,
        isThinking = state.isAssistantThinking,
        onSendMessage = { prompt -> viewModel.sendAssistantMessage(prompt) },
        onClearChat = { viewModel.clearAssistantChat() }
    )

    if (showUnifiedEditProfileDialog && state.loggedInEngineer != null) {
        val eng = state.loggedInEngineer!!
        val screenContext = androidx.compose.ui.platform.LocalContext.current
        com.rosterx.presentation.components.UnifiedEditProfileDialog(
            show = showUnifiedEditProfileDialog,
            currentName = eng.name,
            currentPhone = eng.phone,
            currentEmail = eng.email,
            currentJoinDate = eng.joinDate,
            currentBirthday = eng.birthday ?: "",
            currentProfilePicture = eng.profilePicture,
            onDismiss = { showUnifiedEditProfileDialog = false },
            onRemovePhoto = { onResult ->
                viewModel.removeProfilePicture(
                    targetUsername = eng.username,
                    onResult = onResult
                )
            },
            onSave = { name, phone, email, joinDate, birthday, photoB64, currPass, newPass ->
                viewModel.updateUserProfile(
                    name = name,
                    phone = phone,
                    email = email,
                    joinDate = joinDate,
                    birthday = birthday,
                    profilePicture = photoB64,
                    currentPassword = currPass,
                    newPassword = newPass
                ) { success, err ->
                    android.widget.Toast.makeText(
                        screenContext,
                        if (success) "Profile updated successfully!" else (err ?: "Update failed"),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    if (success && !newPass.isNullOrBlank()) {
                        val activity = screenContext as? android.app.Activity
                        if (activity != null) {
                            viewModel.saveGpmCredentials(activity, eng.username, newPass)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            try {
                                val autofillManager = screenContext.getSystemService(android.view.autofill.AutofillManager::class.java)
                                autofillManager?.commit()
                            } catch (_: Throwable) {}
                        }
                    }
                }
            }
        )
    }

    if (showQuickNotificationsSheet) {
        com.rosterx.presentation.components.QuickNotificationsBottomSheet(
            show = showQuickNotificationsSheet,
            notifications = state.notifications,
            dismissedNotificationIds = state.dismissedNotificationIds,
            onDismiss = { showQuickNotificationsSheet = false },
            onDismissNotification = viewModel::dismissNotification,
            onClearAllNotifications = viewModel::clearAllNotifications,
            loggedInEngineer = state.loggedInEngineer,
            engineers = state.engineers,
            onOpenCelebrationWishes = { type, user, name, years, phone ->
                showQuickNotificationsSheet = false
                celebrationDialogTargetUser = user
                celebrationDialogTargetName = name
                celebrationDialogType = type
                celebrationDialogYears = years
                celebrationDialogPhone = phone.orEmpty()
                val myUname = state.loggedInEngineer?.username.orEmpty()
                val myEmailPrefix = state.loggedInEngineer?.email?.substringBefore("@").orEmpty()
                isCelebrationSelf = user.isNotBlank() && (user.equals(myUname, ignoreCase = true) || user.equals(myEmailPrefix, ignoreCase = true))
                showCelebrationDialog = true
            },
            onNavigateToCredentialTracker = {
                showQuickNotificationsSheet = false
                isViewingSettings = false
                isViewingMyTeam = false
                isViewingCredentialTracker = true
            },
            onNavigateToMonthRoster = {
                showQuickNotificationsSheet = false
                isViewingSettings = false
                isViewingMyTeam = false
                isViewingCredentialTracker = false
                selectedTab = if (isManager) 2 else 3
            }
        )
    }

    if (showCelebrationDialog) {
        val eng = state.loggedInEngineer
        val targetName = celebrationDialogTargetName.ifBlank {
            (eng?.fullName?.takeIf { it.isNotBlank() } ?: eng?.name ?: "Engineer").toProperCase()
        }
        val targetUser = celebrationDialogTargetUser.ifBlank {
            eng?.username.orEmpty()
        }
        CelebrationWishesDialog(
            show = showCelebrationDialog,
            celebrationType = celebrationDialogType,
            userName = targetName,
            anniversaryYears = celebrationDialogYears,
            isSelf = isCelebrationSelf,
            celebrantPhone = celebrationDialogPhone,
            onDismiss = {
                if (targetUser.isNotBlank()) {
                    val todayStr = LocalDate.now().toString()
                    val prefs = portalContext.getSharedPreferences("rosterx_celebrations", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("seen_${targetUser}_${todayStr}", true).apply()
                }
                showCelebrationDialog = false
            }
        )
    }

}

data class PortalTimersData(
    val shiftLabel: String,
    val shiftTime: String,
    val isShiftRunning: Boolean,
    val restLabel: String,
    val restValue: String
)

private fun computePortalTimers(
    roster: Map<String, ShiftCode>,
    today: LocalDate,
    currentTimeSec: Long,
    isManagement: Boolean = false
): PortalTimersData {
    if (isManagement) {
        return PortalTimersData(
            shiftLabel = "Operational Duty:",
            shiftTime = "Supervisory Access",
            isShiftRunning = false,
            restLabel = "Schedule Status:",
            restValue = "Non-Roster Operations"
        )
    }

    val todayShift = roster[today.dayOfMonth.toString()]
    val todayCode = todayShift?.code?.uppercase() ?: "OFF"

    data class ShiftWindow(val start: Long, val end: Long)
    val windows = mapOf(
        "FS" to ShiftWindow(27000L, 59400L),       // 07:30 - 16:30
        "FG" to ShiftWindow(30600L, 63000L),       // 08:30 - 17:30
        "G"  to ShiftWindow(34200L, 66600L),       // 09:30 - 18:30
        "SG" to ShiftWindow(39600L, 72000L),       // 11:00 - 20:00
        "SS" to ShiftWindow(46800L, 79200L),       // 13:00 - 22:00
        "NS" to ShiftWindow(75600L, 113400L)       // 21:00 - 07:30 (+1)
    )

    fun formatDelta(sec: Long): String {
        val safeSec = sec.coerceAtLeast(0)
        val h = safeSec / 3600
        val m = (safeSec % 3600) / 60
        val s = safeSec % 60
        return String.format(Locale.US, "%02dh %02dm %02ds", h, m, s)
    }

    var shiftLabel = "Next Shift Starts in:"
    var shiftTime = "No upcoming duty scheduled"
    var isRunning = false

    val w = windows[todayCode]
    if (w != null) {
        var adjCur = currentTimeSec
        if (todayCode == "NS" && currentTimeSec < 27000L) {
            adjCur += 86400L
        }

        if (adjCur in w.start until w.end) {
            shiftLabel = "Shift Ends in:"
            shiftTime = formatDelta(w.end - adjCur)
            isRunning = true
        } else if (adjCur < w.start) {
            shiftLabel = "Duty Starts in:"
            shiftTime = formatDelta(w.start - adjCur)
        }
    }

    // If duty for today has finished or today is OFF, calculate time until next duty starts
    if (shiftTime == "No upcoming duty scheduled" || (w != null && currentTimeSec >= w.end && todayCode != "NS")) {
        var nextDutyCode: String? = null
        var daysAhead = 0

        for (d in (today.dayOfMonth + 1)..today.lengthOfMonth()) {
            val candidate = roster[d.toString()]?.code?.uppercase() ?: ""
            if (windows.containsKey(candidate)) {
                nextDutyCode = candidate
                daysAhead = d - today.dayOfMonth
                break
            }
        }

        if (nextDutyCode != null && windows.containsKey(nextDutyCode)) {
            val nextWin = windows[nextDutyCode]!!
            val secondsUntilMidnight = (86400L - currentTimeSec).coerceAtLeast(0)
            val intermediateSeconds = (daysAhead - 1) * 86400L
            val totalSec = secondsUntilMidnight + intermediateSeconds + nextWin.start
            shiftLabel = "Next Shift Starts in:"
            shiftTime = formatDelta(totalSec)
        }
    }

    // Dynamic Upcoming Rest Day Calculation
    val todayIsRest = todayCode == "OFF" || todayCode.startsWith("SW") || todayCode == "L"
    var restLabel = "Next Rest Day:"
    var restValue = "Scheduled Next Cycle"

    if (todayIsRest) {
        restLabel = "Rest Day:"
        restValue = "Today (Full Rest)"
    } else {
        var restDaysAway: Int? = null
        var restDateFormatted: String? = null

        for (d in (today.dayOfMonth + 1)..today.lengthOfMonth()) {
            val s = roster[d.toString()]?.code?.uppercase() ?: ""
            if (s == "OFF" || s.startsWith("SW") || s == "L") {
                restDaysAway = d - today.dayOfMonth
                val dObj = today.withDayOfMonth(d)
                val dFmt = dObj.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                restDateFormatted = dFmt
                break
            }
        }

        if (restDaysAway == 1) {
            restLabel = "Next Rest Day:"
            restValue = "Tomorrow ($restDateFormatted)"
        } else if (restDaysAway == 2) {
            restLabel = "Next Rest Day:"
            restValue = "In 2 Days ($restDateFormatted)"
        } else if (restDaysAway != null) {
            restLabel = "Next Rest Day:"
            restValue = "In $restDaysAway Days ($restDateFormatted)"
        }
    }

    return PortalTimersData(
        shiftLabel = shiftLabel,
        shiftTime = shiftTime,
        isShiftRunning = isRunning,
        restLabel = restLabel,
        restValue = restValue
    )
}

data class ManagerStaffDutyInfo(
    val engineer: Engineer,
    val shift: ShiftCode,
    val isActiveNow: Boolean
)

@Composable
fun DutyGroupCard(
    title: String,
    timeRange: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    staff: List<ManagerStaffDutyInfo>,
    isCurrentWindow: Boolean
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .neumorphicInset(
                                shape = RoundedCornerShape(10.dp),
                                surfaceColor = NeumorphicTokens.Surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(end = 4.dp)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeRange,
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCurrentWindow) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "CURRENT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        val countText = if (staff.size == 1) "1 Engineer" else "${staff.size} Engineers"
                        Text(
                            text = countText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            HorizontalDivider(
                color = NeumorphicTokens.SurfaceContrast,
                thickness = 1.dp
            )

            if (staff.isEmpty()) {
                Text(
                    text = "No engineers assigned to this window",
                    fontSize = 12.sp,
                    color = NeumorphicTokens.TextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    staff.forEach { info ->
                        val eng = info.engineer
                        val shift = info.shift
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (info.isActiveNow) Color(0xFF10B981).copy(alpha = 0.08f) else NeumorphicTokens.SurfaceContrast.copy(alpha = 0.4f),
                            border = if (info.isActiveNow) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    UserAvatar(
                                        name = eng.name,
                                        profilePicture = eng.profilePicture,
                                        size = 28.dp,
                                        backgroundColor = accentColor.copy(alpha = 0.2f),
                                        textColor = accentColor,
                                        fontSize = 11.sp
                                    )
                                    Column {
                                        Text(
                                            text = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NeumorphicTokens.TextMain
                                        )
                                        Text(
                                            text = eng.role.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 10.sp,
                                            color = NeumorphicTokens.TextMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (info.isActiveNow) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(end = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Text(
                                                    text = "ACTIVE NOW",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .neumorphicInset(
                                                shape = RoundedCornerShape(8.dp),
                                                surfaceColor = Color(parseColor(shift.bgColorHex))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shift.code,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = Color(parseColor(shift.textColorHex))
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

@Composable
fun ManagerHealthCard(
    state: PortalState,
    onRefreshDiagnostics: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "healthRefresh")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "healthSpin"
    )

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            .size(32.dp)
                            .neumorphicInset(
                                shape = RoundedCornerShape(10.dp),
                                surfaceColor = NeumorphicTokens.Surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Cloud Database",
                            tint = NeumorphicTokens.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SYSTEM & DATABASE HEALTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "MongoDB Atlas & Render Service",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .neumorphicInset(
                            shape = CircleShape,
                            elevation = NeumorphicTokens.ElevationInset,
                            surfaceColor = NeumorphicTokens.Surface
                        )
                        .clickable(enabled = !state.isRefreshingHealth) { onRefreshDiagnostics() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Health",
                        tint = NeumorphicTokens.Accent,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = if (state.isRefreshingHealth) rotationAngle else 0f
                            }
                    )
                }
            }

            // Connection Status Banner
            val isRefreshing = state.isRefreshingHealth
            val statusColor = when {
                isRefreshing -> Color(0xFFF59E0B)
                state.isConnected -> Color(0xFF10B981)
                else -> Color(0xFFEF4444)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = when {
                                isRefreshing -> "Pinging Atlas M0 Cluster..."
                                state.isConnected -> "MongoDB Atlas Connected"
                                else -> "Cluster Offline / Reconnecting"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    if (!isRefreshing && state.latencyMs > 0) {
                        Text(
                            text = "${state.latencyMs} ms",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Health Details 2x2 Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "CLUSTER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.clusterName.ifEmpty { "Cluster0" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "DATABASE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.databaseName.ifEmpty { "rosterx" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "INFRASTRUCTURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Render / Atlas M0",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "SYNC STATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                state.isRefreshingHealth -> "Pinging..."
                                state.isLoading -> "Refreshing..."
                                else -> "Synchronized"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (state.isRefreshingHealth || state.isLoading) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerRadarSection(
    state: PortalState,
    today: LocalDate
) {
    val activeEngineers = state.engineers.filter { it.isEligibleForShiftRoster(today) }
    val dayKey = today.dayOfMonth.toString()
    val yesterday = today.minusDays(1)
    val yesterdayDayKey = yesterday.dayOfMonth.toString()

    val nowTime = java.time.LocalTime.now()
    val nowMin = nowTime.hour * 60 + nowTime.minute
    val isPostMidnightMorning = nowMin < 450 // 00:00 to 07:30 AM

    // Operational shift windows per NOC specifications:
    // Morning shifts: 07:30 AM to 08:00 PM (450..1200)
    val isMorningActive = nowMin in 450..1200
    // Afternoon shift: 01:00 PM to 10:00 PM (780..1320)
    val isAfternoonActive = nowMin in 780..1320
    // Night Shift: 09:00 PM to next day 07:30 AM (>= 1260 || < 450)
    val isNightActive = nowMin >= 1260 || nowMin < 450

    val morningStaff = mutableListOf<ManagerStaffDutyInfo>()
    val afternoonStaff = mutableListOf<ManagerStaffDutyInfo>()
    val nightStaff = mutableListOf<ManagerStaffDutyInfo>()
    val offStaff = mutableListOf<ManagerStaffDutyInfo>()
    val overnightActiveEngIds = mutableSetOf<String>()

    val effectiveYesterdayRoster = if (state.yesterdayRoster.isNotEmpty()) state.yesterdayRoster else state.liveRoster

    // 1. Overnight Shift Rollover: If between 00:00 and 07:30 AM, yesterday's NS engineer is actively on duty!
    if (isPostMidnightMorning) {
        activeEngineers.forEach { eng ->
            val yRoster = getRosterForEngineer(effectiveYesterdayRoster, eng)
            val yShift = yRoster[yesterdayDayKey] ?: ShiftCode.OFF
            val yCode = yShift.code.trim().uppercase()
            if (yCode == "NS") {
                overnightActiveEngIds.add(eng.id)
                overnightActiveEngIds.add(eng.name.lowercase())
                nightStaff.add(ManagerStaffDutyInfo(eng, yShift, isActiveNow = true))
            }
        }
    }

    // 2. Add today's scheduled engineers
    activeEngineers.forEach { eng ->
        val isOvernightActive = eng.id in overnightActiveEngIds || eng.name.lowercase() in overnightActiveEngIds

        val engRoster = getRosterForEngineer(state.liveRoster, eng)
        val shift = engRoster[dayKey] ?: ShiftCode.OFF
        val code = shift.code.trim().uppercase()

        val isEngActive = when (code) {
            "FS" -> nowMin in 450..990
            "FG" -> nowMin in 510..1050
            "G"  -> nowMin in 570..1110
            "SG" -> nowMin in 660..1200
            "SS" -> nowMin in 780..1320
            "NS" -> nowMin >= 1260 // Today's NS is only active after 21:00 PM tonight!
            else -> false
        }

        when {
            code in listOf("FS", "FG", "G", "SG") -> {
                morningStaff.add(ManagerStaffDutyInfo(eng, shift, isActiveNow = isEngActive))
            }
            code == "SS" -> {
                afternoonStaff.add(ManagerStaffDutyInfo(eng, shift, isActiveNow = isEngActive))
            }
            code == "NS" -> {
                val alreadyInNight = nightStaff.any {
                    it.engineer.id == eng.id || it.engineer.name.equals(eng.name, ignoreCase = true)
                }
                if (!alreadyInNight) {
                    nightStaff.add(ManagerStaffDutyInfo(eng, shift, isActiveNow = isEngActive))
                }
            }
            else -> {
                if (!isOvernightActive) {
                    offStaff.add(ManagerStaffDutyInfo(eng, shift, isActiveNow = false))
                }
            }
        }
    }

    val totalActiveNow = morningStaff.count { it.isActiveNow } +
            afternoonStaff.count { it.isActiveNow } +
            nightStaff.count { it.isActiveNow }
    val totalScheduledDuty = morningStaff.size + afternoonStaff.size + nightStaff.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NeumorphicTokens.Accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = NeumorphicTokens.Accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "TODAY'S SHIFT RADAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeumorphicTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
            ) {
                Text(
                    text = "$totalActiveNow ACTIVE NOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF047857),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "ON DUTY TODAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                    Text(
                        text = "$totalScheduledDuty Engineers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeumorphicTokens.SurfaceContrast.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "OFF / LEAVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                    Text(
                        text = "${offStaff.size} Engineers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                }
            }
        }

        DutyGroupCard(
            title = "Morning & General Shift",
            timeRange = "07:30 AM – 08:00 PM",
            icon = Icons.Default.WbSunny,
            accentColor = Color(0xFFF59E0B),
            staff = morningStaff,
            isCurrentWindow = isMorningActive
        )

        DutyGroupCard(
            title = "Afternoon Shift",
            timeRange = "01:00 PM – 10:00 PM",
            icon = Icons.Default.Timer,
            accentColor = Color(0xFF06B6D4),
            staff = afternoonStaff,
            isCurrentWindow = isAfternoonActive
        )

        DutyGroupCard(
            title = "Night Shift",
            timeRange = "09:00 PM – 07:30 AM (+1)",
            icon = Icons.Default.Star,
            accentColor = Color(0xFF8B5CF6),
            staff = nightStaff,
            isCurrentWindow = isNightActive
        )

        if (offStaff.isNotEmpty()) {
            DutyGroupCard(
                title = "Scheduled Off & Leave",
                timeRange = "Rest Period",
                icon = Icons.Default.Today,
                accentColor = Color(0xFF64748B),
                staff = offStaff,
                isCurrentWindow = false
            )
        }
    }
}

@Composable
fun ManagerDashboardView(
    state: PortalState,
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onRefreshDiagnostics: () -> Unit = {},
    onOpenCelebrationWishes: () -> Unit = {},
    onDismissCelebration: (String) -> Unit = {}
) {
    val engineer = state.loggedInEngineer
    val today = LocalDate.now()
    val todayMd = String.format("%02d-%02d", today.monthValue, today.dayOfMonth)
    val bday = (engineer?.birthday ?: "").trim().replace("/", "-")
    val jDate = (engineer?.joinDate ?: "").trim().replace("/", "-")
    val isBirthdayToday = bday.endsWith(todayMd) || (bday.length >= 5 && bday.takeLast(5) == todayMd)
    var isAnniversaryToday = false
    var anniversaryYears = 0
    if (jDate.endsWith(todayMd) || (jDate.length >= 5 && jDate.takeLast(5) == todayMd)) {
        try {
            if (jDate.length >= 4 && jDate.take(4).all { it.isDigit() }) {
                val jYear = jDate.take(4).toInt()
                val diff = today.year - jYear
                if (diff >= 1) {
                    isAnniversaryToday = true
                    anniversaryYears = diff
                }
            }
        } catch (_: Exception) {}
    }

    val activeTeamCelebrations = remember(state.todayCelebrations, state.dismissedCelebrationIds, engineer?.username) {
        state.todayCelebrations.filter { celeb ->
            celeb.username != engineer?.username && !state.dismissedCelebrationIds.contains(celeb.username)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (isLandscape) 12.dp else 16.dp,
            end = if (isLandscape) 12.dp else 16.dp,
            top = if (isLandscape) 6.dp else 8.dp,
            bottom = if (isLandscape) 24.dp else 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
    ) {
        // 1. Sleek Compact Welcome Card (Operation Manager / Operation Hub stripped)
        item {
            val engDisplayName = (engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Manager").toProperCase()
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                            color = NeumorphicTokens.Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Welcome, $engDisplayName! \uD83D\uDC4B",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "Live telemetry & team shift radar",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }

                    UserAvatar(
                        name = engDisplayName,
                        profilePicture = engineer?.profilePicture,
                        size = 46.dp,
                        backgroundColor = NeumorphicTokens.Accent.copy(alpha = 0.18f),
                        textColor = NeumorphicTokens.Accent,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Special Celebration Banner (Birthday / Work Anniversary)
        if (isBirthdayToday || isAnniversaryToday) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCelebrationWishes() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isBirthdayToday) "🎂" else "🏆", fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBirthdayToday && isAnniversaryToday) "Happy Birthday & Anniversary! 🎉"
                                       else if (isBirthdayToday) "Happy Birthday! 🎂"
                                       else "Happy ${anniversaryYears}y Work Anniversary! 🏆",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to view your animated celebration wishes",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Team Celebrations Section (Colleagues Celebrating Today)
        if (activeTeamCelebrations.isNotEmpty()) {
            item(key = "manager_team_celebrations_section") {
                com.rosterx.presentation.components.TeamCelebrationsSection(
                    celebrations = activeTeamCelebrations,
                    onDismiss = onDismissCelebration
                )
            }
        }

        if (isLandscape) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ManagerRadarSection(
                            state = state,
                            today = today
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ManagerHealthCard(
                            state = state,
                            onRefreshDiagnostics = onRefreshDiagnostics
                        )
                    }
                }
            }
        } else {
            // 2. Today's Shift Radar (FIRST!)
            item {
                ManagerRadarSection(
                    state = state,
                    today = today
                )
            }

            // 3. System & Database Health Card (AFTER Shift Radar!)
            item {
                ManagerHealthCard(
                    state = state,
                    onRefreshDiagnostics = onRefreshDiagnostics
                )
            }
        }
    }
}

@Composable
fun DashboardView(
    state: PortalState,
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onRefreshDiagnostics: () -> Unit = {},
    onOpenCelebrationWishes: () -> Unit = {},
    onDismissCelebration: (String) -> Unit = {}
) {
    if (state.loggedInEngineer?.role == EngineerRole.MANAGER) {
        ManagerDashboardView(
            state = state,
            onDismissNotification = onDismissNotification,
            onClearAllNotifications = onClearAllNotifications,
            onRefreshDiagnostics = onRefreshDiagnostics,
            onOpenCelebrationWishes = onOpenCelebrationWishes,
            onDismissCelebration = onDismissCelebration
        )
        return
    }

    val engineer = state.loggedInEngineer
    val context = LocalContext.current
    val roster = getRosterForEngineer(state.liveRoster, engineer)
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayMd = String.format("%02d-%02d", today.monthValue, today.dayOfMonth)
    val bday = (engineer?.birthday ?: "").trim().replace("/", "-")
    val jDate = (engineer?.joinDate ?: "").trim().replace("/", "-")
    val isBirthdayToday = bday.endsWith(todayMd) || (bday.length >= 5 && bday.takeLast(5) == todayMd)
    var isAnniversaryToday = false
    var anniversaryYears = 0
    if (jDate.endsWith(todayMd) || (jDate.length >= 5 && jDate.takeLast(5) == todayMd)) {
        try {
            if (jDate.length >= 4 && jDate.take(4).all { it.isDigit() }) {
                val jYear = jDate.take(4).toInt()
                val diff = today.year - jYear
                if (diff >= 1) {
                    isAnniversaryToday = true
                    anniversaryYears = diff
                }
            }
        } catch (_: Exception) {}
    }

    var currentTimeSec by remember {
        mutableStateOf(java.time.LocalTime.now().toSecondOfDay().toLong())
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            currentTimeSec = java.time.LocalTime.now().toSecondOfDay().toLong()
        }
    }

    val isManagement = state.loggedInEngineer?.isManagementAccount == true ||
        state.loggedInEngineer?.role == EngineerRole.ADMIN ||
        state.loggedInEngineer?.role == EngineerRole.MANAGER

    val timers = remember(roster, today, currentTimeSec, isManagement) {
        computePortalTimers(roster, today, currentTimeSec, isManagement)
    }

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

    val activeTeamCelebrations = remember(state.todayCelebrations, state.dismissedCelebrationIds, engineer?.username) {
        state.todayCelebrations.filter { celeb ->
            celeb.username != engineer?.username && !state.dismissedCelebrationIds.contains(celeb.username)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // 2-Pane Responsive Landscape Dashboard: Left Welcome + Celebrations, Right 2x2 Shift & Metrics Grid
        val engDisplayName = (engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Engineer").toProperCase()
        val isDark = LocalIsDarkTheme.current
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    style = NeumorphicElevationStyle.Raised,
                    surfaceColor = NeumorphicTokens.Surface,
                    contentPadding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = today.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                                    color = NeumorphicTokens.AccentSalmon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Welcome, $engDisplayName! \uD83D\uDC4B",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeumorphicTokens.TextMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            UserAvatar(
                                name = engDisplayName,
                                profilePicture = engineer?.profilePicture,
                                size = 40.dp,
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                textColor = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 15.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (timers.isShiftRunning) Color(0xFF06B6D4).copy(alpha = 0.12f) else NeumorphicTokens.SurfaceContrast,
                            border = BorderStroke(1.dp, if (timers.isShiftRunning) Color(0xFF06B6D4) else Color(0xFFCBD5E1).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (timers.isShiftRunning) Color(0xFF0891B2) else NeumorphicTokens.StatusCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = timers.shiftLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTokens.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = timers.shiftTime,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timers.isShiftRunning) Color(0xFF0891B2) else NeumorphicTokens.TextMain,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7).copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFFCD34D).copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = timers.restLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                )
                                Text(
                                    text = timers.restValue,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFEF08A) else Color(0xFF78350F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (isBirthdayToday || isAnniversaryToday) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCelebrationWishes() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (isBirthdayToday) "🎂" else "🏆", fontSize = 18.sp)
                            Text(
                                text = if (isBirthdayToday && isAnniversaryToday) "Happy Birthday & Anniversary! 🎉"
                                else if (isBirthdayToday) "Happy Birthday! Tap for wishes"
                                else "Happy ${anniversaryYears}y Anniversary!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (activeTeamCelebrations.isNotEmpty()) {
                    com.rosterx.presentation.components.TeamCelebrationsSection(
                        celebrations = activeTeamCelebrations,
                        onDismiss = onDismissCelebration
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 52.dp, start = 2.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        bottomText = "${timers.restLabel} ${timers.restValue}",
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Welcome Banner
        item {
            val engDisplayName = (engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Engineer").toProperCase()
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 20.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                                color = NeumorphicTokens.AccentSalmon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Welcome, $engDisplayName! \uD83D\uDC4B",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeumorphicTokens.TextMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Operational shift overview and live verified team duty briefing.",
                                fontSize = 12.5.sp,
                                color = NeumorphicTokens.TextMuted
                            )
                        }

                        UserAvatar(
                            name = engDisplayName,
                            profilePicture = engineer?.profilePicture,
                            size = 52.dp,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Shift Timer Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (timers.isShiftRunning) Color(0xFF06B6D4).copy(alpha = 0.12f) else NeumorphicTokens.SurfaceContrast,
                            border = BorderStroke(1.dp, if (timers.isShiftRunning) Color(0xFF06B6D4) else Color(0xFFCBD5E1).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (timers.isShiftRunning) Color(0xFF0891B2) else NeumorphicTokens.StatusCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = timers.shiftLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTokens.TextMuted
                                )
                                Text(
                                    text = timers.shiftTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timers.isShiftRunning) Color(0xFF0891B2) else NeumorphicTokens.TextMain
                                )
                            }
                        }

                        // Upcoming Rest Day Badge
                        val isDark = LocalIsDarkTheme.current
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7).copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFFCD34D).copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = timers.restLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                )
                                Text(
                                    text = timers.restValue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFEF08A) else Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Special Celebration Banner (Birthday / Work Anniversary)
        if (isBirthdayToday || isAnniversaryToday) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCelebrationWishes() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isBirthdayToday) "🎂" else "🏆", fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBirthdayToday && isAnniversaryToday) "Happy Birthday & Anniversary! 🎉"
                                       else if (isBirthdayToday) "Happy Birthday! 🎂"
                                       else "Happy ${anniversaryYears}y Work Anniversary! 🏆",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to view your animated celebration greeting",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Team Celebrations Section (Colleagues Celebrating Today)
        if (activeTeamCelebrations.isNotEmpty()) {
            item(key = "team_celebrations_section") {
                com.rosterx.presentation.components.TeamCelebrationsSection(
                    celebrations = activeTeamCelebrations,
                    onDismiss = onDismissCelebration
                )
            }
        }

        // Smart Home Screen Widget Discovery Banner
        item(key = "widget_discovery_banner") {
            val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
            val provider = remember { ComponentName(context, RosterXAppWidgetProvider::class.java) }
            var isDismissedThisSession by remember { mutableStateOf(false) }

            val shouldShowWidgetBanner = remember(isDismissedThisSession) {
                if (isDismissedThisSession) return@remember false
                try {
                    val hasWidget = appWidgetManager.getAppWidgetIds(provider).isNotEmpty()
                    if (hasWidget) return@remember false

                    val prefs = context.getSharedPreferences("rosterx_widget_cadence", Context.MODE_PRIVATE)
                    val snoozeUntil = prefs.getLong("widget_snooze_until", 0L)
                    val now = System.currentTimeMillis()
                    if (now < snoozeUntil) return@remember false

                    val launchCount = prefs.getInt("launch_count", 0)
                    launchCount % 4 == 0 || launchCount == 1
                } catch (_: Exception) {
                    false
                }
            }

            if (shouldShowWidgetBanner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    style = NeumorphicElevationStyle.Raised,
                    surfaceColor = NeumorphicTokens.Surface,
                    contentPadding = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LocalThemeAccent.current.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = LocalThemeAccent.current,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Glance at Shifts Instantly! 📱",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                    Text(
                                        text = "Add the RosterX widget for live duty updates & rest day countdowns.",
                                        fontSize = 11.5.sp,
                                        color = NeumorphicTokens.TextMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isDismissedThisSession = true
                                    val prefs = context.getSharedPreferences("rosterx_widget_cadence", Context.MODE_PRIVATE)
                                    val editor = prefs.edit()
                                    editor.putLong("widget_snooze_until", System.currentTimeMillis() + 7L * 86400000L)
                                    editor.apply()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss for 7 days",
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(context, "Please add the widget from your launcher's widget menu.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalThemeAccent.current),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Widget to Home Screen", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
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

                    bottomText = "${timers.restLabel} ${timers.restValue}",

                    accentColor = Color(0xFFF59E0B),

                    modifier = Modifier.weight(1f)

                )

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

                            surfaceColor = if (shift != null) Color(parseColor(shift.bgColorHex)) else NeumorphicTokens.Surface

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

                        text = if (shift != null && shift !is ShiftCode.OFF && shift !is ShiftCode.SW && !shift.code.startsWith("SW")) "Active Duty" else "Rest Day",

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

            val isDark = LocalIsDarkTheme.current
            Text(
                text = bottomText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (title == "SCHEDULED REST DAYS") {
                    if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                } else NeumorphicTokens.TextMuted,
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
    val context = LocalContext.current

    val activeEngineers = remember(state.engineers, state.selectedDate, state.liveRoster) {
        val nonManagement = state.engineers.filter { !it.isManagementAccount }
        val sorted = sortEngineersByWeekOffs(nonManagement)
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



    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.fillMaxSize()) {

        Surface(

            color = MaterialTheme.colorScheme.surfaceVariant,

            modifier = Modifier.fillMaxWidth()

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 12.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                IconButton(
                    onClick = onPreviousDay,
                    modifier = if (isLandscape) Modifier.size(32.dp) else Modifier
                ) {

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

                        fontSize = if (isLandscape) 13.5.sp else 15.sp,

                        color = NeumorphicTokens.TextMain

                    )

                }



                Row(verticalAlignment = Alignment.CenterVertically) {

                    IconButton(
                        onClick = onNextDay,
                        modifier = if (isLandscape) Modifier.size(32.dp) else Modifier
                    ) {

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = NeumorphicTokens.TextMain)

                    }

                }

            }

        }



        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (isLandscape) 12.dp else 16.dp,
                end = if (isLandscape) 12.dp else 16.dp,
                top = 8.dp,
                bottom = if (isLandscape) 56.dp else 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            val groupedRows = if (isLandscape) sortedShiftGroups.chunked(2) else sortedShiftGroups.map { listOf(it) }
            items(groupedRows) { rowGroups ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowGroups.forEach { (shift, engineersInShift) ->
                        Box(modifier = Modifier.weight(1f)) {
                val shiftTiming = when {
                    shift is ShiftCode.SW || shift.code.startsWith("SW") -> "Rest Day"
                    shift is ShiftCode.OFF -> "Rest Day"
                    else -> ALL_STANDARD_SHIFTS.find { it.first.code == shift.code }?.second ?: "Rest Day"
                }

                

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

                                            surfaceColor = Color(parseColor(shift.bgColorHex))

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
                                val rawPhone = engineer.phone.trim()
                                val hasPhone = rawPhone.isNotBlank()
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

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isUser) NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f) else NeumorphicTokens.SurfaceContrast.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            profilePicture = engineer.profilePicture,
                                            name = engineer.name,
                                            size = 28.dp
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = properName,
                                                fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Medium,
                                                fontSize = 13.5.sp,
                                                color = if (isUser) NeumorphicTokens.AccentSalmon else NeumorphicTokens.TextMain,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Action Buttons: Call & WhatsApp
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Call Button
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
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
                                                            } catch (_: Exception) {
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // WhatsApp Button
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
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
                                                    modifier = Modifier.size(16.dp)
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
                    if (isLandscape && rowGroups.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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

    onNavigateToTeam: () -> Unit,

    onSyncRecorded: ((month: String, count: Int, target: String) -> Unit)? = null,
    onSyncCleared: ((month: String) -> Unit)? = null,
    onCalendarAccountsDiscovered: ((List<com.rosterx.util.DeviceCalendar>) -> Unit)? = null
) {

    val yearMonth = YearMonth.of(state.gridYear, state.gridMonth)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val engineer = state.loggedInEngineer
    val roster = getRosterForEngineer(state.gridRoster, engineer)

    val today = LocalDate.now()
    val themeAccent = LocalThemeAccent.current

    // Infinite breathing transition for Current Day glow & animated border
    val infiniteTransition = rememberInfiniteTransition(label = "TodayGlow")
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowProgress"
    )
    val glowAlpha = 0.35f + 0.60f * glowProgress
    val glowRadius = (3.0f + 5.0f * glowProgress).dp
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showCalendarSyncDialog by remember { mutableStateOf(false) }
    val myMonthShiftCodes = remember(roster) {
        roster.mapNotNull { (dayStr, shiftCode) ->
            dayStr.toIntOrNull()?.let { it to shiftCode.code }
        }.toMap()
    }
    val syncPreview = remember(myMonthShiftCodes) {
        com.rosterx.util.ShiftCalendarExporter.computePreview(myMonthShiftCodes)
    }
    val canSyncPersonalCalendar = remember(syncPreview, engineer) {
        engineer != null && !engineer.isManagementAccount && syncPreview.totalCount > 0
    }

    if (showCalendarSyncDialog && engineer != null) {
        com.rosterx.presentation.components.CalendarSyncDialog(
            engineerName = engineer.fullName.ifBlank { engineer.name },
            username = engineer.username,
            year = state.gridYear,
            month = state.gridMonth,
            dayShiftCodes = myMonthShiftCodes,
            onDismiss = { showCalendarSyncDialog = false },
            onSyncRecorded = onSyncRecorded,
            onSyncCleared = onSyncCleared,
            onCalendarAccountsDiscovered = onCalendarAccountsDiscovered
        )
    }

    if (isLandscape) {
        // 2-Pane Responsive Landscape Layout: Left full-month compact grid + Right Sync & Legend
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Pane: Compact Month Header + Full-Month Calendar Grid (all weeks fit without scrolling)
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = NeumorphicTokens.TextMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMain
                    )
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = NeumorphicTokens.TextMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Weekday Labels
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Responsive Month Matrix distributing all 5-6 weeks evenly across remaining height
                AnimatedContent(
                    targetState = yearMonth,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "LandscapeCalendarTransition"
                ) { currentYM ->
                    val currentDaysCount = currentYM.lengthOfMonth()
                    val leadingBlanks = currentYM.atDay(1).dayOfWeek.value - 1
                    val totalSlots = leadingBlanks + currentDaysCount
                    val weekCount = ((totalSlots + 6) / 7).coerceAtLeast(1)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (week in 0 until weekCount) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val slotIdx = week * 7 + col
                                    val day = slotIdx - leadingBlanks + 1
                                    if (day in 1..currentDaysCount) {
                                        val date = LocalDate.of(currentYM.year, currentYM.monthValue, day)
                                        val isToday = (currentYM.year == today.year && currentYM.monthValue == today.monthValue && day == today.dayOfMonth)
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
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(cellBgColor)
                                                .border(
                                                    width = when {
                                                        isToday -> 2.dp
                                                        isSelected -> 1.5.dp
                                                        else -> 0.dp
                                                    },
                                                    color = when {
                                                        isToday -> themeAccent.copy(alpha = glowAlpha)
                                                        isSelected -> NeumorphicTokens.AccentSalmon
                                                        else -> Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(7.dp)
                                                )
                                                .clickable {
                                                    onDateSelected(date)
                                                    onNavigateToTeam()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = day.toString(),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 13.sp,
                                                        color = if (shift != null) Color(parseColor(shift.textColorHex)) else NeumorphicTokens.TextMain
                                                    )
                                                    if (isToday) {
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(themeAccent.copy(alpha = glowAlpha))
                                                        )
                                                    }
                                                }
                                                if (shift != null) {
                                                    Text(
                                                        text = shift.code,
                                                        fontSize = 9.sp,
                                                        lineHeight = 10.sp,
                                                        color = Color(parseColor(shift.textColorHex)),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane: Sync to Calendar Action + Scrollable Shift Legend
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 58.dp, start = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canSyncPersonalCalendar && engineer != null) {
                    NeumorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCalendarSyncDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        style = NeumorphicElevationStyle.RaisedSubtle,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "Sync to Calendar",
                                tint = themeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sync to Calendar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeAccent
                            )
                        }
                    }
                }

                NeumorphicCard(
                    shape = RoundedCornerShape(18.dp),
                    style = NeumorphicElevationStyle.RaisedSubtle,
                    surfaceColor = NeumorphicTokens.Surface,
                    contentPadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "SHIFT LEGEND & TIMINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.AccentSalmon
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = NeumorphicTokens.SurfaceContrast
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ALL_STANDARD_SHIFTS.forEach { (shift, timing) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .width(30.dp)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(parseColor(shift.bgColorHex))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shift.code,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = Color(parseColor(shift.textColorHex))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = shift.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeumorphicTokens.TextMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = timing,
                                        fontSize = 10.sp,
                                        color = NeumorphicTokens.TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header with Month Navigation Arrows + Personal Calendar Smart Sync
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                if (canSyncPersonalCalendar && engineer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NeumorphicCard(
                            modifier = Modifier.clickable { showCalendarSyncDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            style = NeumorphicElevationStyle.RaisedSubtle,
                            surfaceColor = NeumorphicTokens.Surface,
                            contentPadding = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = "Sync to Calendar",
                                    tint = themeAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Sync to Calendar",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent
                                )
                            }
                        }
                    }
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
                        val isToday = (currentYM.year == today.year && currentYM.monthValue == today.monthValue && day == today.dayOfMonth)
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
                                .then(
                                    if (isToday) {
                                        Modifier.drawBehind {
                                            val blurPx = glowRadius.toPx()
                                            drawIntoCanvas { canvas ->
                                                val paint = android.graphics.Paint().apply {
                                                    isAntiAlias = true
                                                    color = themeAccent.copy(alpha = glowAlpha * 0.5f).toArgb()
                                                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                                                }
                                                canvas.nativeCanvas.drawRoundRect(
                                                    -blurPx * 0.3f,
                                                    -blurPx * 0.3f,
                                                    size.width + blurPx * 0.3f,
                                                    size.height + blurPx * 0.3f,
                                                    10.dp.toPx(),
                                                    10.dp.toPx(),
                                                    paint
                                                )
                                            }
                                        }
                                    } else Modifier
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBgColor)
                                .border(
                                    width = when {
                                        isToday -> 2.5.dp
                                        isSelected -> 1.8.dp
                                        else -> 0.dp
                                    },
                                    color = when {
                                        isToday -> themeAccent.copy(alpha = glowAlpha)
                                        isSelected -> NeumorphicTokens.AccentSalmon
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onDateSelected(date)
                                    onNavigateToTeam()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isToday) 14.sp else 13.sp,
                                        color = if (shift != null) Color(parseColor(shift.textColorHex)) else NeumorphicTokens.TextMain
                                    )
                                    if (isToday) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(themeAccent.copy(alpha = glowAlpha))
                                        )
                                    }
                                }

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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

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
        val nonManagement = state.engineers.filter { !it.isManagementAccount }
        val sorted = sortEngineersByWeekOffs(nonManagement)
        val active = filterEngineersByResignation(sorted, state.gridYear, state.gridMonth, 1, state.gridRoster)
        if (state.searchQuery.isBlank()) active
        else active.filter { 
            it.name.contains(state.searchQuery, ignoreCase = true) ||
            it.username.contains(state.searchQuery, ignoreCase = true)
        }
    }

    // Natural Boundary & Outer Swipe: Inside days 1..31, horizontal drag scrolls the table.
    // At Day 1 (scrollState == 0), right-swipe beyond Day 1 smoothly transitions to Tab 2 (Calendar).
    // At Day 31 (scrollState >= maxValue), left-swipe beyond Day 31 smoothly transitions to Tab 4 (More).
    val gridNestedScrollConnection = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val isAtStart = scrollState.value == 0
                val isAtEnd = scrollState.value >= scrollState.maxValue

                // If user swipes right at Day 1 (isAtStart && available.x > 0), let pager switch to Calendar
                if (isAtStart && available.x > 0f) {
                    return Offset.Zero
                }
                // If user swipes left at Day 31 (isAtEnd && available.x < 0), let pager switch to More
                if (isAtEnd && available.x < 0f) {
                    return Offset.Zero
                }

                // Inside the table, isolate horizontal scrolling so it doesn't accidentally trigger pager
                return Offset(available.x, 0f)
            }
        }
    }



    Column(modifier = Modifier.fillMaxSize()) {

        val configuration = LocalConfiguration.current

        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE



        Surface(

            color = MaterialTheme.colorScheme.surfaceVariant,

            modifier = Modifier.fillMaxWidth()

        ) {

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isLandscape) 2.dp else 12.dp)) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        IconButton(

                            onClick = onPreviousMonth,

                            modifier = Modifier.size(if (isLandscape) 30.dp else 48.dp)

                        ) {

                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp))

                        }

                        Text(

                            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(Locale.US),

                            fontWeight = FontWeight.ExtraBold,

                            fontSize = if (isLandscape) 13.sp else 15.sp,

                            color = MaterialTheme.colorScheme.primary

                        )

                        IconButton(

                            onClick = onNextMonth,

                            modifier = Modifier.size(if (isLandscape) 30.dp else 48.dp)

                        ) {

                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp))

                        }

                    }



                    // Search Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(140.dp)
                            .height(if (isLandscape) 30.dp else 38.dp)
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
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val filterTextStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = onSearchQueryChanged,
                                    singleLine = true,
                                    textStyle = filterTextStyle,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (state.searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Filter...",
                                                    style = filterTextStyle.copy(
                                                        fontWeight = FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
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
                            .height(if (isLandscape) 34.dp else 48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .nestedScroll(gridNestedScrollConnection)
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



                    // Engineer Matrix Rows (Full height with 96dp bottom padding for complete visibility)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(gridNestedScrollConnection),
                        contentPadding = PaddingValues(bottom = if (isLandscape) 72.dp else 96.dp)
                    ) {

                        items(filteredEngineers) { engineer ->

                            val isUser = state.loggedInEngineer?.id == engineer.id

                            val engRoster = getRosterForEngineer(state.gridRoster, engineer)



                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .height(if (isLandscape) 32.dp else 40.dp)

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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        UserAvatar(
                                            name = engineer.name,
                                            profilePicture = engineer.profilePicture,
                                            size = 20.dp,
                                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            textColor = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp
                                        )

                                        Text(

                                            text = engineer.name.toProperCase(),

                                            fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Medium,

                                            fontSize = 11.sp,

                                            maxLines = 1,

                                            overflow = TextOverflow.Ellipsis,

                                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,

                                            modifier = Modifier.weight(1f, fill = false)

                                        )

                                        if (isUser) {

                                            Spacer(modifier = Modifier.width(2.dp))

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

                                        

                                        // Calculate stats as per NOC operational standards:
                                        // - Working consists of FS, FG, G, SG, SS, NS
                                        // - Night only counts NS
                                        // - OFF counts OFF, L and all SW* offs
                                        if (shift != null) {
                                            val c = shift.code.trim().uppercase()
                                            if (c in listOf("FS", "FG", "G", "SG", "SS", "NS")) wrkCount++
                                            if (c == "NS") nsCount++
                                            if (c == "OFF" || c == "L" || c.startsWith("SW") || c == "CO" || c == "AL" || c == "SL" || c == "ML") offCount++
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

                                        .height(if (isLandscape) 36.dp else 44.dp)

                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .width(120.dp)

                                            .fillMaxHeight()

                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)

                                            .padding(horizontal = 6.dp, vertical = 2.dp),

                                        contentAlignment = Alignment.CenterStart

                                    ) {

                                        Column(

                                            modifier = Modifier.fillMaxHeight(),

                                            verticalArrangement = Arrangement.Center

                                        ) {

                                            Text(

                                                text = "DAY COVERAGE",

                                                fontWeight = FontWeight.ExtraBold,

                                                fontSize = 9.sp,

                                                lineHeight = 11.sp,

                                                color = MaterialTheme.colorScheme.primary,

                                                maxLines = 1,

                                                overflow = TextOverflow.Ellipsis

                                            )

                                            Text(

                                                text = "Target: ≥$minDayStaff (Excl. Appr)",

                                                fontSize = 8.sp,

                                                fontWeight = FontWeight.Bold,

                                                lineHeight = 10.sp,

                                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                                                maxLines = 1,

                                                overflow = TextOverflow.Ellipsis

                                            )

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

                                            val dayTextColor = if (dayMet) Color(0xFF0369A1) else Color(0xFFB45309)

                                            val dayBgColor = if (dayMet) Color(0xFFE0F2FE) else Color(0xFFFEF3C7)



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

                                        .height(if (isLandscape) 36.dp else 44.dp)

                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .width(120.dp)

                                            .fillMaxHeight()

                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)

                                            .padding(horizontal = 6.dp, vertical = 2.dp),

                                        contentAlignment = Alignment.CenterStart

                                    ) {

                                        Column(

                                            modifier = Modifier.fillMaxHeight(),

                                            verticalArrangement = Arrangement.Center

                                        ) {

                                            Text(

                                                text = "NIGHT COVERAGE",

                                                fontWeight = FontWeight.ExtraBold,

                                                fontSize = 9.sp,

                                                lineHeight = 11.sp,

                                                color = MaterialTheme.colorScheme.primary,

                                                maxLines = 1,

                                                overflow = TextOverflow.Ellipsis

                                            )

                                            Text(

                                                text = "Target: $targetNight NS (Excl. Appr)",

                                                fontSize = 8.sp,

                                                fontWeight = FontWeight.Bold,

                                                lineHeight = 10.sp,

                                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                                                maxLines = 1,

                                                overflow = TextOverflow.Ellipsis

                                            )

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

                                                else -> Pair(Color(0xFF4338CA), Color(0xFFEEF2FF)) // Target met (Soft Indigo)

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
                                Spacer(modifier = Modifier.height(16.dp))
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
    onOpenEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSaveBaseUrl: (String) -> Unit = {},
    onTestConnection: () -> Unit = {},
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetThemePalette: (String) -> Unit = {},
    onChangePassword: ((String) -> Unit)? = null,
    onUploadProfilePicture: ((String, String?, ((Boolean, String?) -> Unit)?) -> Unit)? = null,
    onRemoveProfilePicture: ((String?, ((Boolean, String?) -> Unit)?) -> Unit)? = null,
    onUpdateProfileName: ((String, String?, ((Boolean, String?) -> Unit)?) -> Unit)? = null,
    onOpenCelebrationWishes: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val engineer = state.loggedInEngineer
    val activeAccent = LocalThemeAccent.current

    val today = LocalDate.now()
    val todayMd = String.format("%02d-%02d", today.monthValue, today.dayOfMonth)
    val bday = (engineer?.birthday ?: "").trim().replace("/", "-")
    val jDate = (engineer?.joinDate ?: "").trim().replace("/", "-")
    val isBirthdayToday = bday.endsWith(todayMd) || (bday.length >= 5 && bday.takeLast(5) == todayMd)
    var isAnniversaryToday = false
    var anniversaryYears = 0
    if (jDate.endsWith(todayMd) || (jDate.length >= 5 && jDate.takeLast(5) == todayMd)) {
        try {
            if (jDate.length >= 4 && jDate.take(4).all { it.isDigit() }) {
                val jYear = jDate.take(4).toInt()
                val diff = today.year - jYear
                if (diff >= 1) {
                    isAnniversaryToday = true
                    anniversaryYears = diff
                }
            }
        } catch (_: Exception) {}
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (isLandscape) 12.dp else 16.dp,
            end = if (isLandscape) 12.dp else 16.dp,
            top = if (isLandscape) 6.dp else 8.dp,
            bottom = if (isLandscape) 24.dp else 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
    ) {
        // User Profile Header (Single top-right pencil icon standard)
        if (isLandscape) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.rosterx.presentation.components.StandardProfileHeaderCard(
                            name = (engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Unknown User"),
                            roleTitle = engineer?.role?.name ?: "ENGINEER",
                            profilePicture = engineer?.profilePicture,
                            username = engineer?.username ?: "",
                            onEditClick = onOpenEditProfile
                        )
                        com.rosterx.presentation.components.TenureAndBirthdayRow(
                            joinDate = engineer?.joinDate,
                            birthday = engineer?.birthday
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (engineer?.role != com.rosterx.domain.model.EngineerRole.ADMIN &&
                            engineer?.role != com.rosterx.domain.model.EngineerRole.MANAGER) {
                            com.rosterx.presentation.components.WeeklyOffsCard(
                                weeklyOffs = engineer?.defaultWeeklyOffs ?: emptyList()
                            )
                        }
                        com.rosterx.presentation.components.AccountDetailsCard(
                            username = engineer?.username ?: "--",
                            email = engineer?.email ?: "--",
                            phone = engineer?.phone ?: "--"
                        )
                    }
                }
            }
        } else {
        item {
            com.rosterx.presentation.components.StandardProfileHeaderCard(
                name = (engineer?.fullName?.takeIf { it.isNotBlank() } ?: engineer?.name ?: "Unknown User"),
                roleTitle = engineer?.role?.name ?: "ENGINEER",
                profilePicture = engineer?.profilePicture,
                username = engineer?.username ?: "",
                onEditClick = onOpenEditProfile
            )
        }

        // Special Celebration Banner (Birthday / Work Anniversary)
        if (isBirthdayToday || isAnniversaryToday) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCelebrationWishes() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isBirthdayToday) "🎂" else "🏆", fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBirthdayToday && isAnniversaryToday) "Happy Birthday & Anniversary! 🎉"
                                       else if (isBirthdayToday) "Happy Birthday! 🎂"
                                       else "Happy ${anniversaryYears}y Work Anniversary! 🏆",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to open your animated celebration wishes",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Dynamic Tenure & Birthday in Days Row
        item {
            com.rosterx.presentation.components.TenureAndBirthdayRow(
                joinDate = engineer?.joinDate,
                birthday = engineer?.birthday
            )
        }

        // Weekly Offs Pill Card (Mon - Sun) — Only relevant for shift personnel (Engineers & Apprentices)
        if (engineer?.role != com.rosterx.domain.model.EngineerRole.ADMIN &&
            engineer?.role != com.rosterx.domain.model.EngineerRole.MANAGER) {
            item {
                com.rosterx.presentation.components.WeeklyOffsCard(
                    weeklyOffs = engineer?.defaultWeeklyOffs ?: emptyList()
                )
            }
        }

        // Account Details Card (Domain, Email with Copy, Phone with Dialer)
        item {
            com.rosterx.presentation.components.AccountDetailsCard(
                username = engineer?.username ?: "--",
                email = engineer?.email ?: "--",
                phone = engineer?.phone ?: "--"
            )
        }
        }

        // Appearance & Brand Theme Selection Card
        item {
            AppearanceThemeCard(
                onSetThemeMode = onSetThemeMode,
                onSetThemePalette = onSetThemePalette
            )
        }

        // Home Screen Widget Card (1-Tap Pinning & Active Status)
        item {
            val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
            val provider = remember { ComponentName(context, RosterXAppWidgetProvider::class.java) }
            val isPinned = remember {
                try {
                    appWidgetManager.getAppWidgetIds(provider).isNotEmpty()
                } catch (_: Exception) { false }
            }

            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "HOME SCREEN WIDGET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent
                            )
                        }

                        if (isPinned) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isPinned)
                            "Your RosterX Home Screen widget is actively displaying shift duties and rest countdowns."
                        else
                            "Glance at your current shift, next shift timings, and upcoming rest days right from your Android Home Screen.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (!isPinned && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Button(
                            onClick = {
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(context, "Please add the widget from your launcher's widget menu.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccent)
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Home Screen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }



        // Server & Public Gateway Configuration Card (Accessible to All Logged In Accounts)

        item {

            var showGatewaySettings by remember { mutableStateOf(false) }
            val activeNetworkName = remember(state.isConnected) { NetworkUtils.getActiveNetworkDescription(context) }



            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 0.dp
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
                            Icon(Icons.Default.Dns, contentDescription = null, tint = activeAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SERVER & CLOUD INFRASTRUCTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = activeAccent)
                        }
                        Icon(
                            imageVector = if (showGatewaySettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = activeAccent
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



                            // Production Gateway with Copy
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Production Gateway (Custom Domain):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = state.currentBaseUrl,
                                            fontSize = 13.sp,
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

                            // Infrastructure Specs Badges
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cloud Infrastructure", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Cloudflare Edge + Render Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Security Protocol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Enforced TLS 1.3 / SSL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Database Engine", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("MongoDB Atlas Cluster0", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // Single Action Button: Refresh / Test Connection
                            OutlinedButton(
                                onClick = {
                                    val prodUrl = "https://api.rosterx.online/"
                                    onSaveBaseUrl(prodUrl)
                                    Toast.makeText(context, "Connected to api.rosterx.online", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection & Reconnect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                        }

                    }

                }

            }

        }



        // Application Specs & Build Version Card
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 0.dp
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
                                tint = activeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ABOUT ROSTERX & SPECS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = activeAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }



                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)



                    ProfileDetailRow("Application Name", "RosterX")

                    ProfileDetailRow("Android App Version", "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")

                    ProfileDetailRow("Backend Server Version", state.serverVersion.ifBlank { "v${BuildConfig.VERSION_NAME}" })

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
fun AppearanceThemeCard(
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetThemePalette: (String) -> Unit
) {
    val currentMode = LocalThemeMode.current
    val currentPalette = LocalThemePalette.current
    val isDark = LocalIsDarkTheme.current
    val activeAccent = LocalThemeAccent.current

    var isExpanded by remember { mutableStateOf(false) }

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "APPEARANCE & THEME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccent
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isExpanded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = activeAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${currentMode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${currentPalette.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = activeAccent
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 1. Theme Mode: System, Light, Dark
                    Text(
                        text = "THEME MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple(AppThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
                            Triple(AppThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                            Triple(AppThemeMode.DARK, "Dark", Icons.Default.DarkMode)
                        )

                        modes.forEach { (mode, label, icon) ->
                            val isSelected = currentMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) activeAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) BorderStroke(1.5.dp, activeAccent) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetThemeMode(mode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) activeAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 2. Color Theme Palette Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACCENT COLOR PALETTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentPalette.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccent
                        )
                    }

                    // Grid of 6 palettes (2 rows x 3 columns)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandPalettes.chunked(3).forEach { rowPalettes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPalettes.forEach { palette ->
                                    val isSelected = currentPalette.id == palette.id
                                    val paletteColor = palette.resolve(isDark)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) paletteColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        border = if (isSelected) BorderStroke(1.5.dp, paletteColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSetThemePalette(palette.id) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(paletteColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = palette.name,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) paletteColor else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
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

