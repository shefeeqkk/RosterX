package com.rosterx.presentation.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.R
import com.rosterx.ui.neumorphic.*
import com.rosterx.presentation.components.*
import com.rosterx.presentation.admin.tabs.*
import com.rosterx.presentation.portal.EngineerPortalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminRosterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var targetEngineerSearch by rememberSaveable { mutableStateOf("") }
    var previewAsEngineer by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullScreenRoster = isLandscape && selectedTab == 2

    if (previewAsEngineer) {
        Box(modifier = Modifier.fillMaxSize()) {
            EngineerPortalScreen()
            
            // Floating Switch Back to Admin Button
            if (!isLandscape) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(1.5.dp, Color.White),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .clickable { previewAsEngineer = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admin Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
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
                            Column {
                                Text(
                                    text = "RosterX Admin",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Full Operations Command",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { previewAsEngineer = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Preview App",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                        NavTabItem(1, "Team", Icons.Default.Group),
                        NavTabItem(2, "Rosters", Icons.Default.CalendarMonth),
                        NavTabItem(3, "Broadcast", Icons.Default.Campaign),
                        NavTabItem(4, "Settings", Icons.Default.Settings)
                    )
                }

                FluidNeumorphicBottomNav(
                    items = adminNavItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(if (!isFullScreenRoster) paddingValues else PaddingValues(0.dp))
                .background(NeumorphicTokens.Canvas)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                                onSaveConstraints = viewModel::saveConstraints
                            )
                            1 -> ManageTeamTab(
                                state = state, 
                                onSyncEngineers = viewModel::syncEngineers,
                                onDeleteEngineer = viewModel::deleteEngineer
                            )
                            2 -> MonthRostersTab(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSaveRoster = viewModel::saveRoster,
                                onUpdateShift = viewModel::updateShift,
                                onLoadBackups = viewModel::loadBackups,
                                onRestoreBackup = viewModel::restoreBackup,
                                initialSearchQuery = targetEngineerSearch
                            )
                            3 -> NotificationsTab(
                                state = state,
                                onSendNotification = viewModel::sendNotification,
                                onRefreshNotifications = viewModel::loadNotifications
                            )
                            4 -> ServerManagementTab(
                                state = state,
                                currentBaseUrl = viewModel.getBaseUrl(),
                                onSaveBaseUrl = viewModel::saveBaseUrl,
                                onRefreshDiagnostics = viewModel::loadData,
                                onRefreshLogs = viewModel::refreshLogs,
                                onSignOut = viewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
}
