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
import com.rosterx.domain.model.EngineerRole
import com.rosterx.presentation.admin.AdminMainScreen
import com.rosterx.presentation.login.LoginScreen
import com.rosterx.presentation.login.LoginViewModel
import com.rosterx.presentation.portal.EngineerPortalScreen
import com.rosterx.ui.theme.RosterXTheme
import com.rosterx.util.BiometricPromptManager
import com.rosterx.util.BiometricPromptManager.BiometricResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var sessionManager: UserSessionManager

    private val loginViewModel: LoginViewModel by viewModels()

    private val biometricPromptManager by lazy {
        BiometricPromptManager(this)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Notifications permitted by user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val currentUser by sessionManager.currentUser.collectAsState()
            var activeBiometricUser by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                biometricPromptManager.promptResults.collect { result ->
                    when (result) {
                        is BiometricResult.AuthenticationSuccess -> {
                            Toast.makeText(this@MainActivity, "Biometric authentication successful!", Toast.LENGTH_SHORT).show()
                            loginViewModel.loginWithBiometrics(activeBiometricUser)
                        }
                        is BiometricResult.AuthenticationError -> {
                            if (result.error != "Use Password" && result.error != "Cancel") {
                                Toast.makeText(this@MainActivity, result.error, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is BiometricResult.AuthenticationFailed -> {
                            Toast.makeText(this@MainActivity, "Biometric authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }

            RosterXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val targetScreen = when {
                        currentUser == null -> "login"
                        currentUser?.role == EngineerRole.ADMIN -> "admin"
                        else -> "engineer"
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
                                    onLoginSuccess = { /* Managed by UserSessionManager state */ },
                                    onBiometricClick = { userIdentifier ->
                                        activeBiometricUser = userIdentifier
                                        biometricPromptManager.showBiometricPrompt(
                                            title = "Authenticate to log in",
                                            subtitle = "Logging in as $userIdentifier"
                                        )
                                    }
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
