package com.rosterx.presentation.components

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import java.util.Locale

fun String.toProperCase(): String {
    val clean = this.trim()
    if (clean.isBlank()) return clean
    return clean.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            if (word.length == 1) word.uppercase()
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

@Composable
fun StandardProfileHeaderCard(
    name: String,
    roleTitle: String,
    profilePicture: String?,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
    onEditNameClick: (() -> Unit)? = null
) {
    val handleEdit: () -> Unit = {
        onEditClick?.invoke() ?: onEditNameClick?.invoke() ?: onAvatarClick?.invoke() ?: Unit
    }
    val hasEditAction = onEditClick != null || onEditNameClick != null || onAvatarClick != null

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasEditAction) {
                // Unified Top-Right Edit Pencil Action Button
                IconButton(
                    onClick = handleEdit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile & Security",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Avatar (Clickable to open edit profile if edit action present)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .then(if (hasEditAction) Modifier.clickable { handleEdit() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        name = name.toProperCase(),
                        profilePicture = profilePicture,
                        size = 80.dp,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name Row
                Text(
                    text = name.toProperCase(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Verified Role Badge
                Text(
                    text = roleTitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Unified Edit Profile & Security Modal Dialog
 * Consolidates photo, personal details, birthday, joining date, and password into one elegant popup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEditProfileDialog(
    show: Boolean,
    currentName: String,
    currentPhone: String,
    currentEmail: String,
    currentJoinDate: String,
    currentBirthday: String,
    currentProfilePicture: String?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        email: String,
        joinDate: String,
        birthday: String,
        newProfilePicture: String?, // null = keep unchanged, "" = remove, "data:..." = update
        newPassword: String?
    ) -> Unit
) {
    if (!show) return

    val context = LocalContext.current

    var nameInput by remember(currentName, show) { mutableStateOf(currentName.toProperCase()) }
    var phoneInput by remember(currentPhone, show) { mutableStateOf(currentPhone) }
    var emailInput by remember(currentEmail, show) { mutableStateOf(currentEmail) }
    var joinDateInput by remember(currentJoinDate, show) { mutableStateOf(currentJoinDate) }
    var birthdayInput by remember(currentBirthday, show) { mutableStateOf(currentBirthday) }

    // Photo state: null = unchanged, "" = remove, "data:..." = new image
    var pendingPhotoBase64 by remember(show) { mutableStateOf<String?>(null) }
    val effectivePhoto = when {
        pendingPhotoBase64 != null -> pendingPhotoBase64!!.takeIf { it.isNotBlank() }
        else -> currentProfilePicture
    }

    // Password fields
    var newPassword by remember(show) { mutableStateOf("") }
    var confirmPassword by remember(show) { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val b64 = compressImageUriToBase64(context, uri, maxDimension = 256, quality = 85)
            if (b64 != null) {
                pendingPhotoBase64 = b64
            } else {
                Toast.makeText(context, "Could not process selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDatePicker(initialDate: String, onSelect: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (initialDate.isNotBlank()) {
            try {
                val parts = initialDate.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onSelect(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val isNameValid = nameInput.trim().length in 2..60
    val isEmailValid = emailInput.isBlank() || (emailInput.contains("@") && emailInput.contains("."))
    val isPasswordMatch = newPassword.isEmpty() || newPassword == confirmPassword
    val isPasswordLengthOk = newPassword.isEmpty() || newPassword.length >= 6
    val canSave = isNameValid && isEmailValid && isPasswordMatch && isPasswordLengthOk

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ManageAccounts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Edit Profile & Security",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 1. Photo Avatar & Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserAvatar(
                        name = nameInput.ifBlank { "User" }.toProperCase(),
                        profilePicture = effectivePhoto,
                        size = 72.dp,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (!effectivePhoto.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { pendingPhotoBase64 = "" },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 2. Personal Information Fields
                Text(
                    text = "PERSONAL INFORMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                // Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 60) nameInput = it },
                    label = { Text("Full Name *", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    isError = !isNameValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Contact Phone
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (it.length <= 20) phoneInput = it },
                    label = { Text("Contact Phone", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Email
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    isError = !isEmailValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Joining Date
                OutlinedTextField(
                    value = joinDateInput,
                    onValueChange = { joinDateInput = it },
                    label = { Text("Joining Date (YYYY-MM-DD)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker(joinDateInput) { joinDateInput = it } }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Joining Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Birthday (🎂)
                OutlinedTextField(
                    value = birthdayInput,
                    onValueChange = { birthdayInput = it },
                    label = { Text("Birthday 🎂 (YYYY-MM-DD)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker(birthdayInput) { birthdayInput = it } }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Birthday", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // 3. Security & Password Change
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SECURITY & PASSWORD (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Leave password fields blank to retain your current password.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    isError = !isPasswordLengthOk,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                if (!isPasswordLengthOk) {
                    Text("Password must be at least 6 characters.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                // Confirm New Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    isError = !isPasswordMatch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                if (!isPasswordMatch) {
                    Text("Passwords do not match.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val trimmedName = nameInput.trim().toProperCase()
                            val trimmedPhone = phoneInput.trim()
                            val trimmedEmail = emailInput.trim().lowercase()
                            val trimmedJoinDate = joinDateInput.trim()
                            val trimmedBirthday = birthdayInput.trim()
                            val passToUpdate = newPassword.trim().takeIf { it.isNotBlank() }

                            onSave(
                                trimmedName,
                                trimmedPhone,
                                trimmedEmail,
                                trimmedJoinDate,
                                trimmedBirthday,
                                pendingPhotoBase64,
                                passToUpdate
                            )
                            onDismiss()
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Deprecated legacy dialogs kept for backwards compatibility
@Composable
fun ProfilePhotoActionDialog(
    show: Boolean,
    hasExistingPhoto: Boolean,
    onDismiss: () -> Unit,
    onChooseNewPhoto: () -> Unit,
    onRemovePhotoClick: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Photo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select an action for your profile picture:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        onDismiss()
                        onChooseNewPhoto()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasExistingPhoto) "Choose New Photo" else "Upload Photo")
                }
                if (hasExistingPhoto) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRemovePhotoClick()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Photo")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmRemovePhotoDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Remove Profile Photo?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to remove your profile picture? Your initials monogram will be displayed across the app and web portals instead.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditNameDialog(
    show: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!show) return
    var nameInput by remember(currentName) { mutableStateOf(currentName.toProperCase()) }
    val isValid = nameInput.trim().length in 2..60 && nameInput.trim() != currentName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Full Name", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Update your display name across RosterX operations console, reports, and rosters:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 60) nameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${nameInput.length} / 60 characters",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nameInput.trim().toProperCase()
                    if (trimmed.length in 2..60) {
                        onDismiss()
                        onSave(trimmed)
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

