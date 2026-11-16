package com.rosterx.presentation.login

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.R
import com.rosterx.presentation.components.NeumorphicFormInput
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicRaised
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicFloating
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TargetField {
    NONE,
    USERNAME,
    PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val identifierFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val currentView = LocalView.current
    val activity = context as? Activity
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val autofillManager = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(AutofillManager::class.java)
        } else null
    }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isIdentifierError by remember { mutableStateOf(false) }
    var localErrorMsg by remember { mutableStateOf<String?>(null) }

    val isImeVisible = WindowInsets.isImeVisible
    val currentIsImeVisible by rememberUpdatedState(isImeVisible)

    var isIdentifierFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    val identifierInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }

    var isAutoLoggingIn by remember { mutableStateOf(false) }
    var lastGpmPromptTimestamp by remember { mutableLongStateOf(0L) }

    val triggerAutoLoginIfReady: (String, String) -> Unit = { user, pass ->
        if (user.isNotBlank() && pass.isNotBlank() && !isAutoLoggingIn && !state.isLoading) {
            isAutoLoggingIn = true
            keyboardController?.hide()
            viewModel.login(user, pass)
        }
    }

    var lastAutofillUser by remember { mutableStateOf("") }
    var lastAutofillPass by remember { mutableStateOf("") }

    val identifierAutofillNode = remember {
        AutofillNode(
            onFill = { filledUser ->
                identifier = filledUser
                lastAutofillUser = filledUser
                localErrorMsg = null
                isIdentifierError = false
                if (lastAutofillPass.isNotBlank()) {
                    triggerAutoLoginIfReady(filledUser, lastAutofillPass)
                } else if (password.isNotBlank()) {
                    triggerAutoLoginIfReady(filledUser, password)
                } else {
                    passwordFocusRequester.requestFocus()
                }
            },
            autofillTypes = listOf(AutofillType.Username)
        )
    }

    val passwordAutofillNode = remember {
        AutofillNode(
            onFill = { filledPass ->
                password = filledPass
                lastAutofillPass = filledPass
                localErrorMsg = null
                val activeUser = if (lastAutofillUser.isNotBlank()) lastAutofillUser else identifier
                if (activeUser.isNotBlank()) {
                    triggerAutoLoginIfReady(activeUser, filledPass)
                }
            },
            autofillTypes = listOf(AutofillType.Password)
        )
    }

    LaunchedEffect(Unit) {
        autofillTree += identifierAutofillNode
        autofillTree += passwordAutofillNode
    }

    fun showKeyboard(target: TargetField) {
        coroutineScope.launch {
            when (target) {
                TargetField.USERNAME -> identifierFocusRequester.requestFocus()
                TargetField.PASSWORD -> passwordFocusRequester.requestFocus()
                TargetField.NONE -> return@launch
            }
            delay(150)
            keyboardController?.show()
            try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val currentFocusedView = activity?.currentFocus ?: activity?.window?.decorView
                if (currentFocusedView != null) {
                    imm?.showSoftInput(currentFocusedView, InputMethodManager.SHOW_IMPLICIT)
                }
            } catch (_: Throwable) {}
        }
    }

    fun requestGooglePasswordManager(targetField: TargetField = TargetField.NONE, force: Boolean = false) {
        if (activity == null || isAutoLoggingIn || state.isLoading) return
        val now = System.currentTimeMillis()
        if (!force && now - lastGpmPromptTimestamp < 1200L) return
        lastGpmPromptTimestamp = now

        coroutineScope.launch {
            viewModel.googlePasswordManager.getSavedCredentials(
                activity = activity,
                onDismissed = {
                    lastGpmPromptTimestamp = System.currentTimeMillis()
                    showKeyboard(targetField)
                }
            ) { savedUser, savedPass ->
                if (savedUser.isNotBlank()) {
                    identifier = savedUser
                    localErrorMsg = null
                    isIdentifierError = false
                }
                if (savedPass.isNotBlank()) {
                    password = savedPass
                    localErrorMsg = null
                }
                if (savedUser.isNotBlank() && savedPass.isNotBlank()) {
                    isAutoLoggingIn = true
                    keyboardController?.hide()
                    viewModel.login(savedUser, savedPass)
                }
            }
        }
    }

    LaunchedEffect(state.isLoading, state.errorMessage) {
        if (!state.isLoading && state.errorMessage != null) {
            isAutoLoggingIn = false
        }
    }

    LaunchedEffect(identifierInteractionSource) {
        identifierInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                if (!currentIsImeVisible) {
                    requestGooglePasswordManager(targetField = TargetField.USERNAME)
                } else {
                    keyboardController?.show()
                }
            }
        }
    }

    LaunchedEffect(passwordInteractionSource) {
        passwordInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                if (!currentIsImeVisible) {
                    requestGooglePasswordManager(targetField = TargetField.PASSWORD)
                } else {
                    keyboardController?.show()
                }
            }
        }
    }

    // 1-Tap Google Password Manager sheet prompt on screen launch & state cleanup
    var hasAttemptedCredentialFetch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!state.loginSuccess) {
            viewModel.resetLoginState()
        }
        if (!hasAttemptedCredentialFetch && activity != null && identifier.isBlank()) {
            hasAttemptedCredentialFetch = true
            requestGooglePasswordManager(targetField = TargetField.NONE, force = true)
        }
    }

    // Google Password Manager Save Prompt on successful login
    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            keyboardController?.hide()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val autofillManager = context.getSystemService(AutofillManager::class.java)
                    autofillManager?.commit()
                } catch (_: Throwable) {}
            }
            delay(250)
            val creds = state.lastSuccessCredentials
            val eng = state.loggedInEngineer
            if (activity != null && creds != null) {
                val saveUser = eng?.username?.ifBlank { creds.first } ?: creds.first
                // Launch Google Password Manager bottom sheet prompt and await user's choice
                viewModel.googlePasswordManager.saveCredentials(activity, saveUser, creds.second)
            }
            if (eng != null) {
                viewModel.completeLogin(eng)
            }
            onLoginSuccess()
        }
    }

    // OTP Password Reset Dialog State
    var showHelpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var resetPasswordVisible by remember { mutableStateOf(false) }

    // Server Configuration Dialog State (Allows 4G/5G mobile connection setup)
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(viewModel.getBaseUrl()) }

    // Webpage-compatible OTP Reset Dialog with Retype Password & Eye Toggle
    if (showHelpDialog) {
        Dialog(
            onDismissRequest = { 
                showHelpDialog = false
                viewModel.resetOtpState()
                otpCode = ""
                newPassword = ""
                confirmNewPassword = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 390.dp)
                    .padding(vertical = 16.dp)
                    .neumorphicFloating(shape = RoundedCornerShape(24.dp))
                    .background(NeumorphicTokens.Surface, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Password Reset (OTP)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = NeumorphicTokens.TextMain
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .neumorphicRaised(shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    showHelpDialog = false
                                    viewModel.resetOtpState()
                                    otpCode = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                },
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

                    if (state.otpMessage != null) {
                        Text(state.otpMessage!!, color = Color(0xFF10B981), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, color = Color(0xFFEF4444), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!state.otpSent) {
                        Text(
                            text = "Enter your domain username or email address. A 6-digit verification code will be dispatched.",
                            fontSize = 11.5.sp,
                            color = NeumorphicTokens.TextMuted,
                            lineHeight = 16.sp
                        )
                        NeumorphicFormInput(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = "Username or Email",
                            placeholder = "e.g. johnd or user@tejasnetworks.com",
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Enter the 6-digit verification code sent to your email and set your new password.",
                            fontSize = 11.5.sp,
                            color = NeumorphicTokens.TextMuted,
                            lineHeight = 16.sp
                        )
                        NeumorphicFormInput(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = "6-Digit OTP Code",
                            placeholder = "123456",
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        NeumorphicFormInput(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            placeholder = "Min 6 characters",
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (resetPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { resetPasswordVisible = !resetPasswordVisible }
                                )
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val mismatch = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword
                        NeumorphicFormInput(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = "Retype New Password",
                            placeholder = "Confirm password",
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (resetPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { resetPasswordVisible = !resetPasswordVisible }
                                )
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            isError = mismatch,
                            errorMessage = if (mismatch) "Passwords do not match" else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                .clickable {
                                    showHelpDialog = false
                                    viewModel.resetOtpState()
                                    otpCode = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicTokens.TextMuted)
                        }

                        val canProceed = if (!state.otpSent) {
                            !state.isLoading && identifier.isNotBlank()
                        } else {
                            !state.isLoading && otpCode.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmNewPassword
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.4f)
                                .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                .background(
                                    if (canProceed) NeumorphicTokens.AccentSalmon else NeumorphicTokens.AccentSalmon.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = canProceed) {
                                    if (!state.otpSent) {
                                        viewModel.sendOtp(identifier)
                                    } else {
                                        viewModel.verifyOtp(identifier, otpCode, newPassword)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (!state.otpSent) "Send Code" else "Verify & Reset",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Server URL Connection Dialog (Accessible anytime without logging in)
    if (showServerDialog) {
        Dialog(
            onDismissRequest = { showServerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 390.dp)
                    .padding(vertical = 16.dp)
                    .neumorphicFloating(shape = RoundedCornerShape(24.dp))
                    .background(NeumorphicTokens.Surface, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Server Connection URL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = NeumorphicTokens.TextMain
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .neumorphicRaised(shape = RoundedCornerShape(8.dp))
                                .clickable { showServerDialog = false },
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

                    Text(
                        text = "Set the RosterX Server Base URL. Enter your Cloudflare Public Tunnel URL to access the server on 4G/5G mobile data or Wi-Fi.",
                        fontSize = 11.5.sp,
                        color = NeumorphicTokens.TextMuted,
                        lineHeight = 16.sp
                    )

                    NeumorphicFormInput(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = "Server Base URL",
                        placeholder = "e.g. https://xxx.trycloudflare.com/api/",
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                .clickable { showServerDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicTokens.TextMuted)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.4f)
                                .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                .background(NeumorphicTokens.AccentSalmon, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (serverUrlInput.isNotBlank()) {
                                        viewModel.setBaseUrl(serverUrlInput)
                                    }
                                    showServerDialog = false
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save & Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicTokens.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 380.dp)
                .padding(vertical = 16.dp)
                .neumorphicFloating(shape = RoundedCornerShape(26.dp))
                .background(NeumorphicTokens.Surface, RoundedCornerShape(26.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Server Configuration Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .neumorphicRaised(shape = RoundedCornerShape(8.dp))
                            .clickable {
                                serverUrlInput = viewModel.getBaseUrl()
                                showServerDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server URL Settings",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Header Area
                Image(
                    painter = painterResource(id = R.drawable.ic_rosterx_logo),
                    contentDescription = "RosterX Logo",
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Welcome to RosterX",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeumorphicTokens.TextMain
                )

                Text(
                    text = "Shift Rotation & Duty Briefings",
                    fontSize = 11.5.sp,
                    color = NeumorphicTokens.TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                )

                // Error Notification Banner
                val activeError = localErrorMsg ?: state.errorMessage
                if (activeError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .neumorphicInset(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                            .background(Color(0xFFEF4444).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = activeError,
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Form Area: Identifier Field with Autofill & Google Password Manager
                NeumorphicFormInput(
                    value = identifier,
                    onValueChange = { 
                        identifier = it
                        localErrorMsg = null
                        if (isIdentifierError && it.isNotBlank()) {
                            isIdentifierError = false
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                autofillManager?.notifyValueChanged(
                                    currentView,
                                    identifierAutofillNode.id,
                                    AutofillValue.forText(it)
                                )
                            } catch (_: Throwable) {}
                        }
                    },
                    label = "Username",
                    placeholder = "Domain username or email",
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            tint = if (isIdentifierError) Color(0xFFEF4444) else NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Autofill from Google Password Manager",
                                tint = NeumorphicTokens.AccentSalmon,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { requestGooglePasswordManager(targetField = TargetField.USERNAME, force = true) }
                            )
                            if (identifier.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear text",
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { identifier = ""; localErrorMsg = null }
                                )
                            }
                        }
                    },
                    interactionSource = identifierInteractionSource,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    focusRequester = identifierFocusRequester,
                    onGloballyPositioned = { coordinates ->
                        identifierAutofillNode.boundingBox = coordinates.boundsInWindow()
                    },
                    onFocusChanged = { focusState ->
                        isIdentifierFocused = focusState.isFocused
                        autofill?.let {
                            if (focusState.isFocused) {
                                it.requestAutofillForNode(identifierAutofillNode)
                            } else {
                                it.cancelAutofillForNode(identifierAutofillNode)
                            }
                        }
                    },
                    isError = isIdentifierError,
                    errorMessage = if (isIdentifierError) "Please enter your username" else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input Field with Google Password Manager
                NeumorphicFormInput(
                    value = password,
                    onValueChange = { 
                        password = it
                        localErrorMsg = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                autofillManager?.notifyValueChanged(
                                    currentView,
                                    passwordAutofillNode.id,
                                    AutofillValue.forText(it)
                                )
                            } catch (_: Throwable) {}
                        }
                    },
                    label = "Password",
                    placeholder = "Enter your password",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            tint = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Autofill from Google Password Manager",
                                tint = NeumorphicTokens.AccentSalmon,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { requestGooglePasswordManager(targetField = TargetField.PASSWORD, force = true) }
                            )
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { passwordVisible = !passwordVisible }
                            )
                        }
                    },
                    interactionSource = passwordInteractionSource,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (identifier.isNotBlank() && password.isNotBlank()) {
                                localErrorMsg = null
                                viewModel.login(identifier, password)
                            }
                        }
                    ),
                    focusRequester = passwordFocusRequester,
                    onGloballyPositioned = { coordinates ->
                        passwordAutofillNode.boundingBox = coordinates.boundsInWindow()
                    },
                    onFocusChanged = { focusState ->
                        isPasswordFocused = focusState.isFocused
                        autofill?.let {
                            if (focusState.isFocused) {
                                it.requestAutofillForNode(passwordAutofillNode)
                            } else {
                                it.cancelAutofillForNode(passwordAutofillNode)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Forgot password Link (Right-aligned immediately below password)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        color = NeumorphicTokens.AccentSalmon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showHelpDialog = true }
                    )
                }

                // Sign In Button
                val canSubmit = !state.isLoading && identifier.isNotBlank() && password.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .neumorphicRaised(shape = RoundedCornerShape(14.dp))
                        .background(
                            if (canSubmit) NeumorphicTokens.AccentSalmon else NeumorphicTokens.AccentSalmon.copy(alpha = 0.45f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = canSubmit) { 
                            if (identifier.isBlank()) {
                                isIdentifierError = true
                            } else {
                                localErrorMsg = null
                                viewModel.login(identifier, password) 
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
