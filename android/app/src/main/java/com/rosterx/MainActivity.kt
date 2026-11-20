package com.rosterx



import android.Manifest
import android.content.Context
import android.content.Intent

import android.content.pm.PackageManager

import android.net.Uri

import android.os.Build

import android.os.Bundle

import android.widget.Toast

import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.viewModels

import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.togetherWith

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier

import androidx.core.content.ContextCompat

import androidx.core.content.pm.ShortcutInfoCompat

import androidx.core.content.pm.ShortcutManagerCompat

import androidx.core.graphics.drawable.IconCompat

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.fragment.app.FragmentActivity

import com.rosterx.data.local.UserSessionManager
import com.rosterx.data.remote.RosterApiService
import com.rosterx.analytics.AnalyticsManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.rosterx.domain.model.EngineerRole

import com.rosterx.presentation.admin.AdminMainScreen

import com.rosterx.presentation.components.AppUpdateInfo

import com.rosterx.presentation.components.resolveAppUpdateInfo

import com.rosterx.presentation.login.LoginScreen

import com.rosterx.presentation.login.LoginViewModel

import com.rosterx.presentation.portal.EngineerPortalScreen

import com.rosterx.ui.theme.RosterXTheme

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

enum class AppDeepLinkTarget {
    ROSTER,
    NOTIFICATIONS,
    CREDENTIALS,
    PROFILE,
    HOME
}

@AndroidEntryPoint

class MainActivity : FragmentActivity() {



    @Inject
    lateinit var sessionManager: UserSessionManager

    @Inject
    lateinit var apiService: RosterApiService

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val loginViewModel: LoginViewModel by viewModels()

    private val pendingDeepLinkState = mutableStateOf<AppDeepLinkTarget?>(null)

    private fun parseDeepLinkIntent(intent: Intent?): AppDeepLinkTarget? {
        if (intent == null) return null
        val extraTarget = intent.getStringExtra("deep_link_target")?.trim()?.lowercase()
        if (!extraTarget.isNullOrBlank()) {
            return when (extraTarget) {
                "roster", "shifts", "calendar" -> AppDeepLinkTarget.ROSTER
                "notifications", "broadcast", "alerts" -> AppDeepLinkTarget.NOTIFICATIONS
                "credentials", "tracker" -> AppDeepLinkTarget.CREDENTIALS
                "profile", "account", "settings" -> AppDeepLinkTarget.PROFILE
                else -> AppDeepLinkTarget.HOME
            }
        }
        val uri: Uri = intent.data ?: return null
        val path = (uri.path ?: "").trim().lowercase().trimEnd('/')
        return when {
            path == "/open/roster" || path.startsWith("/open/roster/") -> AppDeepLinkTarget.ROSTER
            path == "/open/notifications" || path.startsWith("/open/notifications/") -> AppDeepLinkTarget.NOTIFICATIONS
            path == "/open/credentials" || path.startsWith("/open/credentials/") -> AppDeepLinkTarget.CREDENTIALS
            path == "/open/profile" || path.startsWith("/open/profile/") -> AppDeepLinkTarget.PROFILE
            path.isEmpty() || path == "/open" -> AppDeepLinkTarget.HOME
            else -> AppDeepLinkTarget.HOME
        }
    }

    private fun syncRoleDynamicShortcuts(role: EngineerRole?) {
        val shortcutId = "shortcut_credentials"
        try {
            if (role == EngineerRole.ADMIN || role == EngineerRole.MANAGER) {
                val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.rosterx.online/open/credentials")).apply {
                    setClass(this@MainActivity, MainActivity::class.java)
                    putExtra("deep_link_target", "credentials")
                }
                val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
                    .setShortLabel(getString(R.string.shortcut_credentials_short))
                    .setLongLabel(getString(R.string.shortcut_credentials_long))
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_credentials))
                    .setIntent(launchIntent)
                    .build()
                ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
            } else {
                ShortcutManagerCompat.removeDynamicShortcuts(this, listOf(shortcutId))
            }
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLinkIntent(intent)?.let { target ->
            pendingDeepLinkState.value = target
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Notifications permitted by user
            registerFcmDeviceToken()
        }
    }

    private fun registerFcmDeviceToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && !task.result.isNullOrBlank()) {
                        val token = task.result
                        sessionManager.setFcmToken(token)
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                if (!sessionManager.getToken().isNullOrBlank()) {
                                    apiService.registerFcmToken(mapOf("token" to token))
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
        } catch (_: Exception) {}
    }



    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        parseDeepLinkIntent(intent)?.let { target ->
            pendingDeepLinkState.value = target
        }



        // Request runtime notification permission on Android 13+ (API 33+)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                registerFcmDeviceToken()
            }
        } else {
            registerFcmDeviceToken()
        }



        // Preload custom Warm Crystal Chime into SoundPool for zero-latency alerts
        com.rosterx.util.RosterXSoundManager.preload(this)

        // Track app session launches for smart widget cadence prompt
        try {
            val widgetPrefs = getSharedPreferences("rosterx_widget_cadence", Context.MODE_PRIVATE)
            val curCount = widgetPrefs.getInt("launch_count", 0)
            val editor = widgetPrefs.edit()
            editor.putInt("launch_count", curCount + 1)
            editor.apply()
        } catch (_: Exception) {}

        setContent {
            val currentUser by sessionManager.currentUser.collectAsState()
            val themeMode by sessionManager.themeMode.collectAsState()
            val themePaletteId by sessionManager.themePaletteId.collectAsState()
            val pendingDeepLink by pendingDeepLinkState
            var appUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
            var isUpdateBannerDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(currentUser?.username, currentUser?.role) {
                syncRoleDynamicShortcuts(currentUser?.role)
                if (currentUser != null) {
                    val info = withContext(Dispatchers.IO) {
                        runCatching { apiService.getServerInfo() }.getOrNull()
                    }
                    appUpdateInfo = resolveAppUpdateInfo(info, BuildConfig.VERSION_NAME)
                }
            }

            val visibleUpdateInfo = appUpdateInfo?.takeIf { it.isMandatory || !isUpdateBannerDismissed }

            RosterXTheme(
                themeMode = themeMode,
                paletteId = themePaletteId
            ) {

                Surface(

                    modifier = Modifier.fillMaxSize(),

                    color = MaterialTheme.colorScheme.background

                ) {

                    val targetScreen = when {
                        currentUser == null -> "login"
                        currentUser?.role == EngineerRole.ADMIN -> "admin"
                        else -> "engineer"
                    }

                    LaunchedEffect(targetScreen, currentUser) {
                        analyticsManager.logScreenView(targetScreen)
                        currentUser?.let { user ->
                            analyticsManager.setUserId(user.username)
                            analyticsManager.setUserRole(user.role.name)
                        }
                    }



                    AnimatedContent(

                        targetState = targetScreen,

                        transitionSpec = {

                            fadeIn() togetherWith fadeOut()

                        },

                        label = "ScreenTransition"

                    ) { screen ->

                        when (screen) {

                            "login" -> {

                                LoginScreen(

                                    viewModel = loginViewModel,

                                    onLoginSuccess = { /* Managed by UserSessionManager state */ }

                                )

                            }

                            "admin" -> {

                                AdminMainScreen(
                                    pendingDeepLink = pendingDeepLink,
                                    onConsumeDeepLink = { pendingDeepLinkState.value = null },
                                    appUpdateInfo = visibleUpdateInfo,
                                    onDismissUpdateBanner = { isUpdateBannerDismissed = true }
                                )

                            }

                            else -> {

                                EngineerPortalScreen(
                                    pendingDeepLink = pendingDeepLink,
                                    onConsumeDeepLink = { pendingDeepLinkState.value = null },
                                    appUpdateInfo = visibleUpdateInfo,
                                    onDismissUpdateBanner = { isUpdateBannerDismissed = true }
                                )

                            }

                        }

                    }

                }

            }

        }

    }

}

