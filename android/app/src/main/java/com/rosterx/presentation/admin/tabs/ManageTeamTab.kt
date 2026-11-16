package com.rosterx.presentation.admin.tabs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.components.UserAvatar
import com.rosterx.presentation.components.compressImageUriToBase64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Locale
import com.rosterx.presentation.components.NeumorphicFormInput
import com.rosterx.ui.neumorphic.neumorphicRaised
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicFloating
import com.rosterx.ui.neumorphic.NeumorphicTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeamTab(
    state: AdminRosterState,
    onSyncEngineers: (List<Engineer>, (Boolean, String) -> Unit) -> Unit,
    onDeleteEngineer: ((Engineer, (Boolean, String) -> Unit) -> Unit)? = null,
    isReadOnly: Boolean = false
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingEngineer by remember { mutableStateOf<Engineer?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var localEngineersList by remember(state.engineers) {
        mutableStateOf(state.engineers)
    }

    val sortedAndFiltered = remember(localEngineersList, searchQuery) {
        val sorted = localEngineersList.sortedWith(
            compareBy<Engineer> {
                when (it.role) {
                    EngineerRole.ADMIN -> 0
                    EngineerRole.MANAGER -> 1
                    EngineerRole.ENGINEER -> 2
                    EngineerRole.APPRENTICE -> 3
                }
            }.thenBy { it.name.uppercase() }
        )
        if (searchQuery.isBlank()) sorted
        else sorted.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.role.name.contains(searchQuery, ignoreCase = true)
        }
    }

    // Modern Neumorphic Dialog for Register Account & Edit Details
    if (showEditDialog) {
        var nameInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.name ?: "") }
        var usernameInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.username ?: "") }
        var emailInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.email ?: "") }
        var phoneInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.phone ?: "") }
        var passwordInput by remember(editingEngineer) { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var joinDateInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.joinDate ?: "2026-01-01") }
        var birthdayInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.birthday ?: "") }
        var roleInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.role ?: EngineerRole.ENGINEER) }
        var statusInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.status ?: EngineerStatus.ACTIVE) }
        var profilePictureInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.profilePicture) }
        val allDays = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        var selectedOffs by remember(editingEngineer) {
            mutableStateOf(editingEngineer?.defaultWeeklyOffs?.map { it.uppercase() }?.toSet() ?: setOf("SUNDAY", "MONDAY"))
        }

        val teamPhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val b64 = compressImageUriToBase64(context, uri, maxDimension = 256, quality = 85)
                if (b64 != null) {
                    profilePictureInput = b64
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

        Dialog(
            onDismissRequest = {
                if (!isSaving) {
                    editingEngineer = null
                    showEditDialog = false
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 12.dp)
                    .neumorphicFloating(shape = RoundedCornerShape(24.dp))
                    .background(NeumorphicTokens.Surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar with camera badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            UserAvatar(
                                name = nameInput.ifBlank { "User" },
                                profilePicture = profilePictureInput,
                                size = 44.dp
                            )
                            if (!isReadOnly) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(NeumorphicTokens.AccentSalmon)
                                        .clickable {
                                            teamPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Change photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isReadOnly) "Account profile" else if (editingEngineer == null) "Register new account" else "Edit account",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = NeumorphicTokens.TextMain
                            )
                            Text(
                                text = if (editingEngineer == null) "Create system access" else (nameInput.ifBlank { "Update credentials" }),
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .neumorphicRaised(shape = RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!isSaving) {
                                        editingEngineer = null
                                        showEditDialog = false
                                    }
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

                    // Row 1 (Dual-Column 50/50): Full Name * | Username *
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeumorphicFormInput(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (editingEngineer == null && usernameInput.isBlank() && emailInput.isBlank()) {
                                    val first = it.trim().split(" ").firstOrNull()?.lowercase() ?: ""
                                    if (first.isNotBlank()) usernameInput = first
                                }
                            },
                            label = "Full name *",
                            placeholder = "e.g. John Doe",
                            enabled = !isReadOnly && !isSaving,
                            modifier = Modifier.weight(1f)
                        )

                        NeumorphicFormInput(
                            value = usernameInput,
                            onValueChange = { usernameInput = it.lowercase().trim() },
                            label = "Username *",
                            placeholder = "e.g. johnd",
                            enabled = !isReadOnly && !isSaving,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2 (Single-Column 100%): Corporate Email *
                    NeumorphicFormInput(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            if (editingEngineer == null && it.contains("@")) {
                                usernameInput = it.split("@")[0].trim().lowercase()
                            }
                        },
                        label = "Corporate email *",
                        placeholder = "e.g. user@tejasnetworks.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isReadOnly && !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row 3 (Dual-Column 50/50): Phone | Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeumorphicFormInput(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = "Phone",
                            placeholder = "+91 98765 43210",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = !isReadOnly && !isSaving,
                            modifier = Modifier.weight(1f)
                        )

                        NeumorphicFormInput(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = if (editingEngineer == null) "Password *" else "New password",
                            placeholder = if (editingEngineer == null) "Required" else "Leave blank",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = NeumorphicTokens.TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { passwordVisible = !passwordVisible }
                                )
                            },
                            enabled = !isReadOnly && !isSaving,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 4 (Dual-Column 50/50): Join date | Birthday
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeumorphicFormInput(
                            value = joinDateInput,
                            onValueChange = { joinDateInput = it },
                            label = "Join date",
                            placeholder = "YYYY-MM-DD",
                            readOnly = true,
                            onClick = if (!isReadOnly && !isSaving) {
                                { openDatePicker(joinDateInput) { joinDateInput = it } }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Join Date",
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        NeumorphicFormInput(
                            value = birthdayInput,
                            onValueChange = { birthdayInput = it },
                            label = "Birthday",
                            placeholder = "YYYY-MM-DD",
                            readOnly = true,
                            onClick = if (!isReadOnly && !isSaving) {
                                { openDatePicker(birthdayInput) { birthdayInput = it } }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Cake,
                                    contentDescription = "Pick Birthday",
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 5: Role & Access Level (100%)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Role & access",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicInset(shape = RoundedCornerShape(12.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                EngineerRole.ENGINEER to "Engineer",
                                EngineerRole.MANAGER to "Manager",
                                EngineerRole.ADMIN to "Admin",
                                EngineerRole.APPRENTICE to "Apprent."
                            ).forEach { (roleOption, label) ->
                                val isSelected = roleInput == roleOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .neumorphicRaised(shape = RoundedCornerShape(9.dp), elevation = NeumorphicTokens.ElevationRaised)
                                                    .background(NeumorphicTokens.AccentSalmon, RoundedCornerShape(9.dp))
                                            } else {
                                                Modifier.clickable { if (!isReadOnly && !isSaving) roleInput = roleOption }
                                            }
                                        )
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else NeumorphicTokens.TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Weekly Offs (if role is Engineer or Apprentice)
                    if (roleInput == EngineerRole.ENGINEER || roleInput == EngineerRole.APPRENTICE) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Weekly offs (2 days)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allDays.forEach { day ->
                                    val isSelected = selectedOffs.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .neumorphicRaised(shape = RoundedCornerShape(8.dp), elevation = NeumorphicTokens.ElevationRaised)
                                                        .background(NeumorphicTokens.AccentSalmon, RoundedCornerShape(8.dp))
                                                } else {
                                                    Modifier.neumorphicInset(shape = RoundedCornerShape(8.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                                }
                                            )
                                            .clickable {
                                                if (!isReadOnly && !isSaving) {
                                                    selectedOffs = if (isSelected) selectedOffs - day else selectedOffs + day
                                                }
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Employment Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Employment status",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                EngineerStatus.ACTIVE to "Active",
                                EngineerStatus.RESIGNED to "Resigned"
                            ).forEach { (statusOption, label) ->
                                val isSelected = statusInput == statusOption
                                val activeColor = if (statusOption == EngineerStatus.ACTIVE) Color(0xFF10B981) else Color(0xFFEF4444)
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .neumorphicRaised(shape = RoundedCornerShape(8.dp), elevation = NeumorphicTokens.ElevationRaised)
                                                    .background(activeColor, RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier
                                                    .neumorphicInset(shape = RoundedCornerShape(8.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                                    .clickable { if (!isReadOnly && !isSaving) statusInput = statusOption }
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else NeumorphicTokens.TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isReadOnly && editingEngineer != null && onDeleteEngineer != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isSaving) { showDeleteConfirmDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSaving) {
                                    editingEngineer = null
                                    showEditDialog = false
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isReadOnly) "Close" else "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                        }

                        if (!isReadOnly) {
                            val canSave = nameInput.isNotBlank() && emailInput.isNotBlank() && !isSaving
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .neumorphicRaised(shape = RoundedCornerShape(12.dp))
                                    .background(
                                        if (canSave) NeumorphicTokens.AccentSalmon else NeumorphicTokens.AccentSalmon.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = canSave) {
                                        val target = editingEngineer
                                        val cleanUsername = if (usernameInput.isNotBlank()) {
                                            usernameInput.trim().lowercase()
                                        } else if (target != null && target.username.isNotBlank()) {
                                            target.username.trim().lowercase()
                                        } else if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                            emailInput.split("@")[0].trim().lowercase()
                                        } else {
                                            nameInput.split(" ").firstOrNull()?.lowercase()?.trim() ?: "user"
                                        }

                                        if (target == null && passwordInput.isBlank()) {
                                            Toast.makeText(context, "Please set an initial password for this account", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }

                                        isSaving = true
                                        val origUsername = if (target != null && target.username.isNotBlank() && !target.username.equals(cleanUsername, ignoreCase = true)) {
                                            target.username.trim().lowercase()
                                        } else null

                                        val newEng = Engineer(
                                            id = cleanUsername,
                                            name = nameInput.trim(),
                                            fullName = nameInput.trim(),
                                            username = cleanUsername,
                                            email = emailInput.trim(),
                                            phone = phoneInput.trim(),
                                            role = roleInput,
                                            status = statusInput,
                                            defaultWeeklyOffs = if (roleInput == EngineerRole.ENGINEER || roleInput == EngineerRole.APPRENTICE) selectedOffs.toList() else emptyList(),
                                            joinDate = joinDateInput.ifBlank { "2026-01-01" },
                                            birthday = birthdayInput.ifBlank { null },
                                            password = passwordInput.trim().ifBlank { null },
                                            originalUsername = origUsername,
                                            profilePicture = profilePictureInput
                                        )

                                        val newList = if (target == null) {
                                            val existingIdx = localEngineersList.indexOfFirst {
                                                (it.username.isNotBlank() && it.username.equals(newEng.username, ignoreCase = true)) ||
                                                it.name.equals(newEng.name, ignoreCase = true)
                                            }
                                            if (existingIdx >= 0) {
                                                localEngineersList.mapIndexed { idx, item -> if (idx == existingIdx) newEng else item }
                                            } else {
                                                localEngineersList + newEng
                                            }
                                        } else {
                                            localEngineersList.map {
                                                val isMatch = (target.username.isNotBlank() && it.username.equals(target.username, ignoreCase = true)) ||
                                                              (target.id.isNotBlank() && (it.id.equals(target.id, ignoreCase = true) || it.username.equals(target.id, ignoreCase = true))) ||
                                                              it.name.equals(target.name, ignoreCase = true)
                                                if (isMatch) newEng else it
                                            }
                                        }

                                        onSyncEngineers(newList) { success, message ->
                                            isSaving = false
                                            if (success) {
                                                localEngineersList = newList
                                                editingEngineer = null
                                                showEditDialog = false
                                                Toast.makeText(
                                                    context,
                                                    if (target == null) "Account registered successfully" else "Account updated successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    if (message.isNotBlank()) message else "Failed to save account",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text("Saving...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = if (editingEngineer == null) Icons.Default.PersonAdd else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (editingEngineer == null) "Create account" else "Save changes",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && editingEngineer != null) {
        val targetEng = editingEngineer!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = NeumorphicTokens.Canvas,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${targetEng.name}\" (@${targetEng.username})?\n\nThis will remove them from the database, directory, and future schedules.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = NeumorphicTokens.TextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEngineer?.invoke(targetEng) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                localEngineersList = localEngineersList.filter { it.id != targetEng.id && it.username != targetEng.username }
                                showDeleteConfirmDialog = false
                                showEditDialog = false
                                editingEngineer = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = NeumorphicTokens.TextMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = NeumorphicTokens.Canvas,
        floatingActionButton = {
            if (!isReadOnly) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingEngineer = null
                        showEditDialog = true
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Register Account", fontWeight = FontWeight.Bold) },
                    containerColor = NeumorphicTokens.AccentSalmon,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Modern Neumorphic Toolbar & Search
            Surface(
                color = NeumorphicTokens.Surface,
                border = BorderStroke(1.dp, NeumorphicTokens.SurfaceContrast),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REGISTERED ACCOUNTS (${localEngineersList.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = NeumorphicTokens.AccentSalmon
                        )
                        Text(
                            text = "Engineers, Managers & Supervisors",
                            fontSize = 10.sp,
                            color = NeumorphicTokens.TextMuted
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeumorphicTokens.Canvas,
                        border = BorderStroke(1.dp, NeumorphicTokens.SurfaceContrast),
                        modifier = Modifier
                            .width(170.dp)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...", fontSize = 11.sp, color = NeumorphicTokens.TextMuted) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = NeumorphicTokens.TextMain),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Registered Accounts List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedAndFiltered) { engineer ->
                    val (roleLabel, roleBgColor, roleTextColor) = when (engineer.role) {
                        EngineerRole.ADMIN -> Triple("ADMIN", Color(0xFFF3E8FF), Color(0xFF7E22CE))
                        EngineerRole.MANAGER -> Triple("MANAGER", Color(0xFFE0E7FF), Color(0xFF4338CA))
                        EngineerRole.APPRENTICE -> Triple("APPRENTICE", Color(0xFFFEF3C7), Color(0xFFB45309))
                        else -> Triple("ENGINEER", Color(0xFFE0F2FE), Color(0xFF0284C7))
                    }

                    val (statusLabel, statusBgColor, statusTextColor) = if (engineer.status == EngineerStatus.ACTIVE) {
                        Triple("ACTIVE", Color(0xFFECFDF5), Color(0xFF059669))
                    } else {
                        Triple("RESIGNED", Color(0xFFFEF2F2), Color(0xFFDC2626))
                    }

                    val monogram = if (engineer.name.contains(" ")) {
                        val parts = engineer.name.split(" ").filter { it.isNotBlank() }
                        (parts.first().take(1) + parts.last().take(1)).uppercase()
                    } else {
                        engineer.name.take(2).uppercase()
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeumorphicTokens.Surface,
                        border = BorderStroke(1.dp, NeumorphicTokens.SurfaceContrast),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingEngineer = engineer
                                showEditDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            UserAvatar(
                                name = engineer.name,
                                profilePicture = engineer.profilePicture,
                                size = 44.dp,
                                backgroundColor = NeumorphicTokens.AccentSalmon.copy(alpha = 0.12f),
                                textColor = NeumorphicTokens.AccentSalmon,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Main Details Column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Name and Badges Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = engineer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = NeumorphicTokens.TextMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    // Role Badge (fixed, never wraps)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = roleBgColor
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = roleTextColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                        )
                                    }

                                    // Status Badge (fixed, never wraps)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBgColor
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusTextColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }

                                // Identifier and Email
                                Text(
                                    text = "@${engineer.username} · ${engineer.email}",
                                    fontSize = 11.sp,
                                    color = NeumorphicTokens.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Schedule Offs or Management Level
                                if (engineer.role == EngineerRole.ENGINEER || engineer.role == EngineerRole.APPRENTICE) {
                                    val offsText = if (engineer.defaultWeeklyOffs.isNotEmpty()) {
                                        engineer.defaultWeeklyOffs.joinToString(", ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                    } else "None configured"
                                    Text(
                                        text = "Offs: $offsText",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NeumorphicTokens.AccentSalmon
                                    )
                                } else {
                                    Text(
                                        text = if (engineer.role == EngineerRole.ADMIN) "Full Administrator Access" else "Operations Management Access",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (engineer.role == EngineerRole.ADMIN) Color(0xFF7E22CE) else Color(0xFF4338CA)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    editingEngineer = engineer
                                    showEditDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Account",
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
