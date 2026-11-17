package com.rosterx.presentation.admin

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.R
import com.rosterx.presentation.admin.tabs.*
import com.rosterx.presentation.components.*
import com.rosterx.presentation.portal.AccountView
import com.rosterx.presentation.portal.EngineerPortalViewModel
import com.rosterx.presentation.portal.credentials.CredentialTrackerScreen
import com.rosterx.presentation.portal.more.MoreHubView
import com.rosterx.ui.neumorphic.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminRosterViewModel = hiltViewModel(),
    portalViewModel: EngineerPortalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val portalState by portalViewModel.state.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val isManagerReadOnly = currentUser?.role == com.rosterx.domain.model.EngineerRole.MANAGER

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var targetEngineerSearch by rememberSaveable { mutableStateOf("") }

    var isViewingSettings by rememberSaveable { mutableStateOf(false) }
    var isViewingCredentialTracker by rememberSaveable { mutableStateOf(false) }
    var isViewingServerManagement by rememberSaveable { mutableStateOf(false) }
    var previousTab by rememberSaveable { mutableIntStateOf(0) }
    var showUnifiedEditProfileDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isViewingCredentialTracker || isViewingServerManagement || isViewingSettings) {
        if (isViewingCredentialTracker) {
            isViewingCredentialTracker = false
        } else if (isViewingServerManagement) {
            isViewingServerManagement = false
        } else if (isViewingSettings) {
            isViewingSettings = false
            selectedTab = previousTab
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            var completedTasks = 0
            val onTaskFinished = {
                completedTasks++
                if (completedTasks >= 2) {
                    pullRefreshState.endRefresh()
                }
            }
            viewModel.refreshAllData {
                onTaskFinished()
            }
            portalViewModel.refreshAllData {
                onTaskFinished()
            }
        }
    }

    LaunchedEffect(selectedTab, isViewingSettings, isViewingCredentialTracker, isViewingServerManagement) {
        val adminScreenName = if (isViewingCredentialTracker) {
            "Admin Credential Tracker"
        } else if (isViewingServerManagement) {
            "Admin Server Management"
        } else if (isViewingSettings) {
            "Admin Profile & Settings"
        } else {
            when (selectedTab) {
                0 -> "Admin Dashboard"
                1 -> "Admin Manage Team"
                2 -> "Admin Month Rosters"
                3 -> "Admin Notifications"
                4 -> "Admin Operations Hub"
                else -> "Admin Dashboard"
            }
        }
        viewModel.analyticsManager.logScreenView(adminScreenName)
        viewModel.analyticsManager.logEvent("admin_tab_switched", mapOf("tab_name" to adminScreenName))
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullScreenRoster = isLandscape && selectedTab == 2

    Scaffold(
        containerColor = NeumorphicTokens.Canvas,
        topBar = {
            if (!isFullScreenRoster && !isViewingCredentialTracker) {
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
                                    contentDescription = "Back",
                                    tint = NeumorphicTokens.TextMain
                                )
                            }
                        } else if (isViewingServerManagement) {
                            IconButton(
                                onClick = {
                                    isViewingServerManagement = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Operations Hub",
                                    tint = NeumorphicTokens.TextMain
                                )
                            }
                        }
                    },
                    title = {
                        if (isViewingSettings) {
                            Text(
                                text = "Account & Settings",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = NeumorphicTokens.TextMain
                            )
                        } else if (isViewingServerManagement) {
                            Text(
                                text = "Server Management",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = NeumorphicTokens.TextMain
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
                                Column {
                                    Text(
                                        text = if (isManagerReadOnly) "Operations Portal" else "RosterX Admin",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isManagerReadOnly) "Operations Management" else "Full Operations Command",
                                        fontSize = 10.sp,
                                        color = if (isManagerReadOnly) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (!isViewingSettings && !isViewingServerManagement && !isViewingCredentialTracker && selectedTab != 4) {
                            val activeHeaderUser = state.engineers.find { 
                                it.username.equals(currentUser?.username, ignoreCase = true) 
                            } ?: currentUser
                            if (activeHeaderUser != null) {
                                val displayName = (activeHeaderUser.fullName?.takeIf { it.isNotBlank() } ?: activeHeaderUser.name).toProperCase()
                                val firstName = displayName.split(" ")[0]

                                NeumorphicCard(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clickable {
                                            previousTab = selectedTab
                                            isViewingCredentialTracker = false
                                            isViewingServerManagement = false
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
                                            name = displayName,
                                            profilePicture = activeHeaderUser.profilePicture,
                                            size = 24.dp,
                                            backgroundColor = NeumorphicTokens.AccentSalmon.copy(alpha = 0.2f),
                                            textColor = NeumorphicTokens.AccentSalmon,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = firstName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = NeumorphicTokens.TextMain
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NeumorphicTokens.Canvas,
                        titleContentColor = NeumorphicTokens.AccentSalmon
                    )
                )
            }
        },
        bottomBar = {
            val adminNavItems = remember {
                listOf(
                    NavTabItem(0, "Dashboard", Icons.Default.Dashboard),
                    NavTabItem(1, "Register", Icons.Default.PersonAdd),
                    NavTabItem(2, "Rosters", Icons.Default.CalendarMonth),
                    NavTabItem(3, "Broadcast", Icons.Default.Campaign),
                    NavTabItem(4, "More", Icons.Default.MoreHoriz)
                )
            }

            if (!isFullScreenRoster && !isViewingSettings && !isViewingCredentialTracker && !isViewingServerManagement) {
                FluidNeumorphicBottomNav(
                    items = adminNavItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { tab ->
                        if (tab == 0 || tab == 2) {
                            viewModel.resetToCurrentMonth()
                        }
                        selectedTab = tab
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (!isFullScreenRoster && !isViewingCredentialTracker) paddingValues.calculateTopPadding()
                    else WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
                )
                .background(NeumorphicTokens.Canvas)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val activeSubView = if (isViewingCredentialTracker) 1 else if (isViewingServerManagement) 2 else if (isViewingSettings) 3 else 0

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
                    label = "AdminSubViewTransition"
                ) { subView ->
                    when (subView) {
                        1 -> {
                            CredentialTrackerScreen(
                                state = portalState,
                                viewModel = portalViewModel,
                                onBackClick = { isViewingCredentialTracker = false },
                                currentUser = currentUser
                            )
                        }
                        2 -> {
                            ServerManagementTab(
                                state = state,
                                currentBaseUrl = viewModel.getBaseUrl(),
                                currentUser = currentUser,
                                onSaveBaseUrl = viewModel::saveBaseUrl,
                                onRefreshDiagnostics = viewModel::loadData,
                                onRefreshLogs = viewModel::refreshLogs,
                                onUploadProfilePicture = viewModel::uploadProfilePicture,
                                onRemoveProfilePicture = viewModel::removeProfilePicture,
                                onUpdateProfileName = viewModel::updateProfileName,
                                onSignOut = viewModel::logout
                            )
                        }
                        3 -> {
                            val activeAdminEngineer = state.engineers.find {
                                it.username.equals(currentUser?.username, ignoreCase = true)
                            } ?: currentUser ?: portalState.loggedInEngineer
                            AccountView(
                                state = portalState.copy(loggedInEngineer = activeAdminEngineer),
                                onOpenEditProfile = { showUnifiedEditProfileDialog = true },
                                onLogout = viewModel::logout,
                                onSaveBaseUrl = viewModel::saveBaseUrl,
                                onTestConnection = portalViewModel::checkConnection,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetThemePalette = viewModel::setThemePaletteId
                            )
                        }
                        else -> {
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
                                label = "AdminTabTransition"
                            ) { tab ->
                                when (tab) {
                                    0 -> AdminDashboardTab(
                                        state = state,
                                        onRefreshDiagnostics = viewModel::loadData,
                                        onNavigateToMonthRoster = { engQuery ->
                                            if (!engQuery.isNullOrBlank()) {
                                                targetEngineerSearch = engQuery
                                            }
                                            selectedTab = 2
                                        },
                                        onSaveConstraints = viewModel::saveConstraints,
                                        todayCelebrations = portalState.todayCelebrations,
                                        dismissedCelebrationIds = portalState.dismissedCelebrationIds,
                                        onDismissCelebration = portalViewModel::dismissCelebration
                                    )
                                    1 -> ManageTeamTab(
                                        state = state,
                                        onSyncEngineers = viewModel::syncEngineers,
                                        onDeleteEngineer = viewModel::deleteEngineer,
                                        isReadOnly = isManagerReadOnly
                                    )
                                    2 -> MonthRostersTab(
                                        state = state,
                                        onPreviousMonth = viewModel::previousMonth,
                                        onNextMonth = viewModel::nextMonth,
                                        onSaveRoster = viewModel::saveRoster,
                                        onUpdateShift = viewModel::updateShift,
                                        onLoadBackups = viewModel::loadBackups,
                                        onRestoreBackup = viewModel::restoreBackup,
                                        initialSearchQuery = targetEngineerSearch,
                                        isReadOnly = isManagerReadOnly
                                    )
                                    3 -> NotificationsTab(
                                        state = state,
                                        onSendNotification = viewModel::sendNotification,
                                        onRetractNotification = viewModel::retractNotification,
                                        onRefreshNotifications = viewModel::loadNotifications,
                                        onSaveTemplate = viewModel::saveBroadcastTemplate,
                                        onDeleteTemplate = viewModel::deleteBroadcastTemplate
                                    )
                                    4 -> MoreHubView(
                                        state = portalState,
                                        isAdmin = true,
                                        onNavigateToCredentialTracker = { isViewingCredentialTracker = true },
                                        onNavigateToServerManagement = { isViewingServerManagement = true }
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

            if (!isFullScreenRoster && !isViewingSettings && !isViewingCredentialTracker && !isViewingServerManagement) {
                FloatingAiBotButton(
                    onClick = { viewModel.openAssistant() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 96.dp)
                )
            }
        }
    }

    if (showUnifiedEditProfileDialog && currentUser != null) {
        val activeSelf = state.engineers.find {
            it.username.equals(currentUser?.username, ignoreCase = true)
        } ?: currentUser!!
        val screenContext = LocalContext.current
        UnifiedEditProfileDialog(
            show = showUnifiedEditProfileDialog,
            currentName = activeSelf.name,
            currentPhone = activeSelf.phone,
            currentEmail = activeSelf.email,
            currentJoinDate = activeSelf.joinDate,
            currentBirthday = activeSelf.birthday ?: "",
            currentProfilePicture = activeSelf.profilePicture,
            onDismiss = { showUnifiedEditProfileDialog = false },
            onSave = { name, phone, email, joinDate, birthday, photoB64, currPass, newPass ->
                viewModel.updateUserProfile(
                    name = name,
                    phone = phone,
                    email = email,
                    joinDate = joinDate,
                    birthday = birthday,
                    profilePicture = photoB64,
                    currentPassword = currPass,
                    newPassword = newPass,
                    targetUsername = activeSelf.username,
                    onResult = { success, errorMsg ->
                        showUnifiedEditProfileDialog = false
                        android.widget.Toast.makeText(
                            screenContext,
                            if (success) "Profile updated successfully" else (errorMsg ?: "Update failed"),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        )
    }

    RosterXAssistantBottomSheet(
        isOpen = state.isAssistantOpen,
        onDismiss = { viewModel.closeAssistant() },
        messages = state.assistantMessages,
        isThinking = state.isAssistantThinking,
        onSendMessage = { prompt -> viewModel.sendAssistantMessage(prompt) },
        onClearChat = { viewModel.clearAssistantChat() }
    )
}
