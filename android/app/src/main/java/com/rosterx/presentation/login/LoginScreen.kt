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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TargetField {
    NONE,
    USERNAME,
    PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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

    val identifierAutofillNode = remember {
        AutofillNode(
            onFill = { filledUser ->
                identifier = filledUser
                localErrorMsg = null
                isIdentifierError = false
                if (password.isNotBlank()) {
                    triggerAutoLoginIfReady(filledUser, password)
                } else {
                    passwordFocusRequester.requestFocus()
                }
            },
            autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress)
        )
    }

    val passwordAutofillNode = remember {
        AutofillNode(
            onFill = { filledPass ->
                password = filledPass
                localErrorMsg = null
                triggerAutoLoginIfReady(identifier, filledPass)
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
            delay(100)
            keyboardController?.show()
            try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val currentView = activity?.currentFocus ?: activity?.window?.decorView
                if (currentView != null) {
                    imm?.showSoftInput(currentView, InputMethodManager.SHOW_IMPLICIT)
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
                if (!isIdentifierFocused) {
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
                if (!isPasswordFocused) {
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
        AlertDialog(
            onDismissRequest = { 
                showHelpDialog = false
                viewModel.resetOtpState()
                otpCode = ""
                newPassword = ""
                confirmNewPassword = ""
            },
            title = { Text("Password Reset (6-Digit OTP)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.otpMessage != null) {
                        Text(state.otpMessage!!, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!state.otpSent) {
                        Text(
                            "Enter your domain username or registered email address. A 6-digit verification code will be dispatched to your email.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Username or Email") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        Text(
                            "Please enter the 6-digit verification code sent to your email and set your new password.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit OTP Code") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility"
                                    )
                                }
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Retype New Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility"
                                    )
                                }
                            },
                            isError = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword,
                            supportingText = {
                                if (confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword) {
                                    Text("Passwords do not match", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!state.otpSent) {
                            viewModel.sendOtp(identifier)
                        } else {
                            viewModel.verifyOtp(identifier, otpCode, newPassword)
                        }
                    },
                    enabled = if (!state.otpSent) {
                        !state.isLoading && identifier.isNotBlank()
                    } else {
                        !state.isLoading && otpCode.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmNewPassword
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (!state.otpSent) "Send Code" else "Verify & Reset", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showHelpDialog = false
                    viewModel.resetOtpState()
                    otpCode = ""
                    newPassword = ""
                    confirmNewPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct Server URL Connection Dialog (Accessible anytime without logging in)
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Server Connection URL", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set the RosterX Server Base URL. Enter your Cloudflare Public Tunnel URL to access the server on 4G/5G mobile data or any Wi-Fi.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Server Base URL") },
                        placeholder = { Text("e.g. https://xxx.trycloudflare.com/api/") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverUrlInput.isNotBlank()) {
                            viewModel.setBaseUrl(serverUrlInput)
                        }
                        showServerDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1600f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Server Configuration Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            serverUrlInput = viewModel.getBaseUrl()
                            showServerDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Server URL Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Header Area
                Image(
                    painter = painterResource(id = R.drawable.ic_rosterx_logo),
                    contentDescription = "RosterX Logo",
                    modifier = Modifier.size(68.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Welcome to RosterX",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "24/7 Shift Rotation & Real-time Duty Briefings",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Error Notification Banner
                val activeError = localErrorMsg ?: state.errorMessage
                if (activeError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = activeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Form Area: Identifier Field with Autofill & Google Password Manager
                OutlinedTextField(
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
                    label = { Text("Username or Email") },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            tint = if (isIdentifierError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = { requestGooglePasswordManager(targetField = TargetField.USERNAME, force = true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Autofill from Google Password Manager",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (identifier.isNotEmpty()) {
                                IconButton(
                                    onClick = { identifier = ""; localErrorMsg = null },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear text", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    },
                    interactionSource = identifierInteractionSource,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    isError = isIdentifierError,
                    supportingText = {
                        if (isIdentifierError) {
                            Text(
                                "Please enter your username or email",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(identifierFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            identifierAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            isIdentifierFocused = focusState.isFocused
                            autofill?.let {
                                if (focusState.isFocused) {
                                    it.requestAutofillForNode(identifierAutofillNode)
                                } else {
                                    it.cancelAutofillForNode(identifierAutofillNode)
                                }
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password Input Field with Google Password Manager
                OutlinedTextField(
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
                    label = { Text("Password") },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = { requestGooglePasswordManager(targetField = TargetField.PASSWORD, force = true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Autofill from Google Password Manager",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    interactionSource = passwordInteractionSource,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (identifier.isNotBlank() && password.isNotBlank()) {
                                localErrorMsg = null
                                viewModel.login(identifier, password)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            passwordAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            isPasswordFocused = focusState.isFocused
                            autofill?.let {
                                if (focusState.isFocused) {
                                    it.requestAutofillForNode(passwordAutofillNode)
                                } else {
                                    it.cancelAutofillForNode(passwordAutofillNode)
                                }
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sign In Button
                Button(
                    onClick = { 
                        if (identifier.isBlank()) {
                            isIdentifierError = true
                        } else {
                            localErrorMsg = null
                            viewModel.login(identifier, password) 
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !state.isLoading && identifier.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer Options
                TextButton(
                    onClick = { showHelpDialog = true }
                ) {
                    Text(
                        "Forgot Username / Need Help?",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
