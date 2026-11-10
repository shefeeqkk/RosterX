package com.rosterx



import android.Manifest

import android.content.pm.PackageManager

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

import androidx.fragment.app.FragmentActivity

import com.rosterx.data.local.UserSessionManager
import com.rosterx.data.remote.RosterApiService
import com.rosterx.analytics.AnalyticsManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.rosterx.domain.model.EngineerRole

import com.rosterx.presentation.admin.AdminMainScreen

import com.rosterx.presentation.login.LoginScreen

import com.rosterx.presentation.login.LoginViewModel

import com.rosterx.presentation.portal.EngineerPortalScreen

import com.rosterx.ui.theme.RosterXTheme

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject



@AndroidEntryPoint

class MainActivity : FragmentActivity() {



    @Inject
    lateinit var sessionManager: UserSessionManager

    @Inject
    lateinit var apiService: RosterApiService

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val loginViewModel: LoginViewModel by viewModels()

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

        super.onCreate(savedInstanceState)



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



        setContent {
            val currentUser by sessionManager.currentUser.collectAsState()
            val themeMode by sessionManager.themeMode.collectAsState()
            val themePaletteId by sessionManager.themePaletteId.collectAsState()

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

                                AdminMainScreen()

                            }

                            else -> {

                                EngineerPortalScreen()

                            }

                        }

                    }

                }

            }

        }

    }

}

