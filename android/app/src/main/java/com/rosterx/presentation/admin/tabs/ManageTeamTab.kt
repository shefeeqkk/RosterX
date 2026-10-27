package com.rosterx.presentation.admin.tabs

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.portal.sortEngineersByWeekOffs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeamTab(
    state: AdminRosterState,
    onSyncEngineers: (List<Engineer>) -> Unit,
    onDeleteEngineer: ((Engineer, (Boolean, String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingEngineer by remember { mutableStateOf<Engineer?>(null) }
    var localEngineersList by remember(state.engineers) { mutableStateOf(state.engineers) }

    val sortedAndFiltered = remember(localEngineersList, searchQuery) {
        val sorted = sortEngineersByWeekOffs(localEngineersList)
        if (searchQuery.isBlank()) sorted
        else sorted.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    // Add / Edit Engineer Dialog
    if (showEditDialog) {
        var nameInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.name ?: "") }
        var usernameInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.username ?: "") }
        var emailInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.email ?: "") }
        var phoneInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.phone ?: "") }
        var roleInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.role ?: EngineerRole.ENGINEER) }
        var statusInput by remember(editingEngineer) { mutableStateOf(editingEngineer?.status ?: EngineerStatus.ACTIVE) }

        val allDays = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        var selectedOffs by remember(editingEngineer) { mutableStateOf(editingEngineer?.defaultWeeklyOffs?.map { it.uppercase() }?.toSet() ?: setOf("SUNDAY", "MONDAY")) }

        AlertDialog(
            onDismissRequest = { 
                editingEngineer = null
                showEditDialog = false 
            },
            title = {
                Text(
                    text = if (editingEngineer == null) "Add New Engineer" else "Edit Engineer Specs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Domain Username", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Registered Email", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Contact Phone", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Weekly Offs Day Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Default Weekly Offs:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            allDays.forEach { day ->
                                val isSelected = selectedOffs.contains(day)
                                Surface(
                                    onClick = {
                                        selectedOffs = if (isSelected) selectedOffs - day else selectedOffs + day
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = day.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = statusInput == EngineerStatus.ACTIVE,
                                onClick = { statusInput = EngineerStatus.ACTIVE },
                                label = { Text("ACTIVE") }
                            )
                            FilterChip(
                                selected = statusInput == EngineerStatus.RESIGNED,
                                onClick = { statusInput = EngineerStatus.RESIGNED },
                                label = { Text("RESIGNED") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingEngineer
                        val resolvedId = target?.id?.ifBlank { null } 
                            ?: "eng_${usernameInput.ifBlank { nameInput.lowercase() }.trim()}"
                        val newEng = Engineer(
                            id = resolvedId,
                            name = nameInput.trim(),
                            fullName = nameInput.trim(),
                            username = usernameInput.ifBlank { nameInput.split(" ")[0].lowercase() }.trim(),
                            email = emailInput.trim(),
                            phone = phoneInput.trim(),
                            role = roleInput,
                            status = statusInput,
                            defaultWeeklyOffs = selectedOffs.toList(),
                            joinDate = target?.joinDate?.ifBlank { null } ?: "2026-01-01"
                        )
                        val newList = if (target == null) {
                            localEngineersList + newEng
                        } else {
                            localEngineersList.map {
                                val isMatch = (target.id.isNotBlank() && it.id == target.id) ||
                                              it.name.equals(target.name, ignoreCase = true) ||
                                              (target.username.isNotBlank() && it.username.equals(target.username, ignoreCase = true))
                                if (isMatch) newEng else it
                            }
                        }
                        localEngineersList = newList
                        onSyncEngineers(newList)
                        editingEngineer = null
                        showEditDialog = false
                    },
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("Save & Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingEngineer != null && onDeleteEngineer != null) {
                        TextButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { 
                        editingEngineer = null
                        showEditDialog = false 
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Delete Engineer Confirmation Dialog
    if (showDeleteConfirmDialog && editingEngineer != null) {
        val targetEng = editingEngineer!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Engineer?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${targetEng.name}\"?\n\nThis will remove them from the database, directory, and future roster schedules.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEngineer?.invoke(targetEng) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                localEngineersList = localEngineersList.filter { it.id != targetEng.id }
                                showDeleteConfirmDialog = false
                                showEditDialog = false
                                editingEngineer = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEngineer = null
                    showEditDialog = true
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Engineer", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Toolbar & Search
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEAM DIRECTORY (${localEngineersList.size})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(160.dp)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Filter...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Engineers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedAndFiltered) { engineer ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingEngineer = engineer
                                showEditDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = engineer.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(engineer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (engineer.status == EngineerStatus.ACTIVE) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = engineer.status.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (engineer.status == EngineerStatus.ACTIVE) Color(0xFF10B981) else Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${engineer.username} · ${engineer.email}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Offs: ${if (engineer.defaultWeeklyOffs.isNotEmpty()) engineer.defaultWeeklyOffs.joinToString(", ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } } else "None"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = {
                                editingEngineer = engineer
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
