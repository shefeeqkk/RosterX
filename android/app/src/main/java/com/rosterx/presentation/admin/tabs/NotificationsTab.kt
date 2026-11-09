package com.rosterx.presentation.admin.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.data.remote.BroadcastTemplateDto
import com.rosterx.data.remote.NotificationDto
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.domain.model.isManagementAccount
import com.rosterx.presentation.admin.AdminRosterState

enum class NotificationPriority(val key: String, val label: String, val color: Color) {
    INFO("info", "Info", Color(0xFF3B82F6)),
    SHIFT_UPDATE("shift_update", "Shift Update", Color(0xFFF59E0B)),
    URGENT("urgent", "Urgent", Color(0xFFEF4444)),
    BROADCAST("broadcast", "Broadcast", Color(0xFF8B5CF6))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsTab(
    state: AdminRosterState,
    onSendNotification: (
        title: String,
        message: String,
        target: Any,
        priority: String,
        sendEmail: Boolean,
        sendInApp: Boolean,
        sendPush: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onRetractNotification: (
        notificationId: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit = { _, _ -> },
    onRefreshNotifications: () -> Unit
) {
    val context = LocalContext.current

    // Active (non-resigned) engineers for recipient selection
    val activeEngineers = remember(state.engineers) {
        state.engineers.filter { it.status != EngineerStatus.RESIGNED && !it.isManagementAccount }
    }

    // Default Operational Templates
    val defaultTemplates = remember {
        listOf(
            BroadcastTemplateDto(
                id = "tpl_shift_announcement",
                name = "Shift Announcement",
                title = "Shift Operations Announcement",
                message = "Dear {name},\n\nPlease review your updated shift assignments for the upcoming operational cycle. Contact the operations desk for any shift swap requests.\n\nBest regards,\nOperations Desk",
                priority = "info"
            ),
            BroadcastTemplateDto(
                id = "tpl_urgent_handover",
                name = "Urgent Handover",
                title = "Urgent Operational Handover",
                message = "Dear {name},\n\nURGENT: Critical operational updates require immediate attention before beginning your shift. Please acknowledge handover protocols immediately.\n\nBest regards,\nOperations Desk",
                priority = "urgent"
            ),
            BroadcastTemplateDto(
                id = "tpl_maintenance",
                name = "System Maintenance",
                title = "Scheduled System Maintenance",
                message = "Dear {name},\n\nScheduled maintenance is planned for this Sunday between 02:00 UTC and 04:00 UTC. System services may experience intermittent availability.\n\nBest regards,\nOperations Desk",
                priority = "info"
            ),
            BroadcastTemplateDto(
                id = "tpl_coverage_swap",
                name = "Swap Request",
                title = "Shift Coverage Swap Request",
                message = "Dear {name},\n\nA shift coverage swap request has been submitted for upcoming operational dates. Please review the updated schedule.\n\nBest regards,\nOperations Desk",
                priority = "shift_update"
            )
        )
    }

    val availableTemplates = remember(state.broadcastTemplates) {
        if (state.broadcastTemplates.isNotEmpty()) state.broadcastTemplates else defaultTemplates
    }

    // Form states
    var targetMode by remember { mutableStateOf("everyone") } // "everyone" or "specific"
    val selectedEngineerIds = remember { mutableStateListOf<String>() }
    var notificationTitle by remember { mutableStateOf("Shift Operations Announcement") }
    var notificationMessage by remember { mutableStateOf("Dear {name},\n\nPlease review your updated shift assignments for the upcoming operational cycle. Contact the operations desk for any shift swap requests.\n\nBest regards,\nOperations Desk") }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.INFO) }
    var sendInAppChecked by remember { mutableStateOf(true) }
    var sendPushChecked by remember { mutableStateOf(true) }
    var sendEmailChecked by remember { mutableStateOf(true) }
    var showMentionDialog by remember { mutableStateOf(false) }

    // Live '@' Mention inline detector
    val mentionMatch = remember(notificationMessage) {
        Regex("@([a-zA-Z0-9_\\-\\.]*)$").find(notificationMessage)
    }
    val mentionQuery = mentionMatch?.groupValues?.getOrNull(1)?.lowercase()
    val isMentionActive = mentionMatch != null

    val matchingMentionEngineers = remember(mentionQuery, activeEngineers) {
        if (mentionQuery == null) emptyList()
        else {
            activeEngineers.filter { eng ->
                val uname = eng.username.lowercase()
                val fname = (eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name).lowercase()
                mentionQuery.isEmpty() || uname.contains(mentionQuery) || fname.contains(mentionQuery)
            }.take(5)
        }
    }

    // Live preview recipient selector
    var previewTargetIndex by remember { mutableIntStateOf(0) }
    val previewEngineer = activeEngineers.getOrNull(previewTargetIndex) ?: activeEngineers.firstOrNull()

    // Calculated personalized preview with @mention resolution to Full Names
    val resolvedPreviewMessage = remember(notificationMessage, previewEngineer, activeEngineers) {
        val engName = previewEngineer?.fullName?.takeIf { it.isNotBlank() }
            ?: previewEngineer?.name ?: "Engineer"
        val engUsername = previewEngineer?.username ?: engName.lowercase().replace(" ", "")

        var text = notificationMessage
            .replace(Regex("\\{\\s*(?:Their\\s+name|their\\s+name|name)\\s*\\}", RegexOption.IGNORE_CASE), engName)
            .replace(Regex("\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), engUsername)
            .replace(Regex("@\\s*\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), "@$engUsername")

        val nameMap = activeEngineers.associate { e ->
            val u = e.username.lowercase()
            val fn = e.fullName?.takeIf { it.isNotBlank() } ?: e.name
            u to fn
        }

        text = Regex("@([a-zA-Z0-9_\\-\\.]+)").replace(text) { m ->
            val u = m.groupValues[1].lowercase()
            if (u in listOf("all", "everyone", "team")) {
                "@Everyone (All Team)"
            } else {
                val resolved = nameMap[u] ?: u
                "@$resolved"
            }
        }
        text
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // --- 1. Header Banner ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "TEAM BROADCAST CONSOLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Multi-Channel Operations Dispatcher",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = "Broadcast live notices across In-App feeds, Android Notification Panel (FCM), and Email. Type @ to mention teammates.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 2. Operational Templates Carousel ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OPERATIONAL TEMPLATES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap to load",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableTemplates) { tpl ->
                            SuggestionChip(
                                onClick = {
                                    notificationTitle = tpl.title ?: tpl.name ?: "Announcement"
                                    notificationMessage = tpl.message ?: ""
                                    when (tpl.priority?.lowercase()) {
                                        "urgent" -> selectedPriority = NotificationPriority.URGENT
                                        "shift_update" -> selectedPriority = NotificationPriority.SHIFT_UPDATE
                                        "broadcast" -> selectedPriority = NotificationPriority.BROADCAST
                                        else -> selectedPriority = NotificationPriority.INFO
                                    }
                                    Toast.makeText(context, "Loaded template: ${tpl.name ?: tpl.title}", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        tpl.name ?: tpl.title ?: "Template",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Article,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Recipient Audience Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARGET AUDIENCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (targetMode == "everyone") "${activeEngineers.size} Active Engineers" else "${selectedEngineerIds.size} Selected",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Segmented Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = targetMode == "everyone",
                            onClick = { targetMode = "everyone" },
                            label = { Text("🌐 Everyone (Team)") },
                            leadingIcon = if (targetMode == "everyone") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = targetMode == "specific",
                            onClick = { targetMode = "specific" },
                            label = { Text("👤 Specific Engineers") },
                            leadingIcon = if (targetMode == "specific") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Specific Engineers Chip Selector
                    AnimatedVisibility(visible = targetMode == "specific") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap engineers to include in notification:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        if (selectedEngineerIds.size == activeEngineers.size) {
                                            selectedEngineerIds.clear()
                                        } else {
                                            selectedEngineerIds.clear()
                                            selectedEngineerIds.addAll(activeEngineers.map { it.id.ifBlank { it.username } })
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (selectedEngineerIds.size == activeEngineers.size) "Clear All" else "Select All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activeEngineers.forEach { eng ->
                                    val engKey = eng.id.ifBlank { eng.username }
                                    val isSelected = selectedEngineerIds.contains(engKey)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedEngineerIds.remove(engKey)
                                            else selectedEngineerIds.add(engKey)
                                        },
                                        label = {
                                            Text(
                                                eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        eng.name.take(1).uppercase(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Composer Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "COMPOSE NOTIFICATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Priority Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Priority Level:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NotificationPriority.values().forEach { priority ->
                                val isSelected = selectedPriority == priority
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) priority.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) priority.color else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.clickable { selectedPriority = priority }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = priority.color,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Text(
                                            priority.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) priority.color else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = notificationTitle,
                        onValueChange = { notificationTitle = it },
                        label = { Text("Notification Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )

                    // Quick Mention & Variable Helpers Toolbar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Mentions & Tokens:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Type @ anytime",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dear {name}
                            SuggestionChip(
                                onClick = {
                                    notificationMessage = "Dear {name},\n\n$notificationMessage"
                                },
                                label = { Text("✨ {name}") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )

                            // {username}
                            SuggestionChip(
                                onClick = {
                                    notificationMessage += " {username} "
                                },
                                label = { Text("+ {username}") }
                            )

                            // @all
                            SuggestionChip(
                                onClick = {
                                    notificationMessage += " @all "
                                },
                                label = { Text("@all") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                )
                            )

                            // @ Mention Engineer Dialog
                            SuggestionChip(
                                onClick = { showMentionDialog = true },
                                label = { Text("@ Mention...") },
                                icon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                )
                            )

                            // Urgent Window
                            SuggestionChip(
                                onClick = {
                                    notificationMessage += "\n⚠️ ACTION REQUIRED: Please review and acknowledge within 24 hours."
                                },
                                label = { Text("+ Urgent Action") }
                            )
                        }
                    }

                    // Message Body Textfield
                    OutlinedTextField(
                        value = notificationMessage,
                        onValueChange = { notificationMessage = it },
                        label = { Text("Message Body (Supports {name}, @username, @all)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Live '@' Mention Autocomplete Popup Dropdown
                    AnimatedVisibility(
                        visible = isMentionActive && (matchingMentionEngineers.isNotEmpty() || "all".contains(mentionQuery ?: ""))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Select teammate to mention:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                // @all team option
                                if ("all".contains(mentionQuery ?: "") || "everyone".contains(mentionQuery ?: "")) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val q = mentionQuery ?: ""
                                                notificationMessage = notificationMessage.dropLast(q.length + 1) + "@all "
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF8B5CF6),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("@", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                            Text("@all — Everyone (All Team)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                matchingMentionEngineers.forEach { eng ->
                                    val engKey = eng.id.ifBlank { eng.username }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val q = mentionQuery ?: ""
                                                notificationMessage = notificationMessage.dropLast(q.length + 1) + "@${eng.username} "
                                                if (targetMode == "specific" && !selectedEngineerIds.contains(engKey)) {
                                                    selectedEngineerIds.add(engKey)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        eng.name.take(1).uppercase(),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "@${eng.username}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Delivery Options (In-App, FCM Push, and Email)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Delivery Channels:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Channel 1: In-App Notification (Live App Alert)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sendInAppChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (sendInAppChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive, 
                                        contentDescription = null, 
                                        tint = if (sendInAppChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Column {
                                        Text("Send In-App Portal Notice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Publishes live banner announcement card inside mobile & web portal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = sendInAppChecked,
                                    onCheckedChange = { sendInAppChecked = it }
                                )
                            }
                        }

                        // Channel 2: FCM Push Notification (Android Shade Panel)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sendPushChecked) Color(0xFFF59E0B).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (sendPushChecked) Color(0xFFF59E0B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.PhoneAndroid, 
                                        contentDescription = null, 
                                        tint = if (sendPushChecked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                    )
                                    Column {
                                        Text("Phone Panel (FCM Push)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Instant alert straight into phone notification panel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = sendPushChecked,
                                    onCheckedChange = { sendPushChecked = it }
                                )
                            }
                        }

                        // Channel 3: Email via Resend
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sendEmailChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (sendEmailChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Email, 
                                        contentDescription = null, 
                                        tint = if (sendEmailChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Column {
                                        Text("Send Email via Resend", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Sends styled HTML email to recipient mailboxes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = sendEmailChecked,
                                    onCheckedChange = { sendEmailChecked = it }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Live Personalized Preview Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                            Text(
                                text = "LIVE PERSONALIZED PREVIEW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                letterSpacing = 1.sp
                            )
                        }

                        // Preview recipient picker
                        if (activeEngineers.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    previewTargetIndex = (previewTargetIndex + 1) % activeEngineers.size
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "As: ${previewEngineer?.name ?: "None"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Channel indicator badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sendInAppChecked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "✓ In-App",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (sendPushChecked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "🔔 FCM Push",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (sendEmailChecked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "✉ Email",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notificationTitle.ifBlank { "Untitled Notice" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = selectedPriority.color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = selectedPriority.label.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = selectedPriority.color,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            Text(
                                text = resolvedPreviewMessage,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- 6. Dispatch Button ---
        item {
            val recipientCount = if (targetMode == "everyone") activeEngineers.size else selectedEngineerIds.size
            val hasChannel = sendInAppChecked || sendPushChecked || sendEmailChecked
            val canSend = notificationTitle.isNotBlank() && notificationMessage.isNotBlank() && recipientCount > 0 && hasChannel && !state.isSendingNotification

            Button(
                onClick = {
                    if (targetMode == "specific" && selectedEngineerIds.isEmpty()) {
                        Toast.makeText(context, "Please select at least one engineer.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!hasChannel) {
                        Toast.makeText(context, "Please enable at least one delivery channel.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val targetPayload: Any = if (targetMode == "everyone") "everyone" else selectedEngineerIds.toList()

                    onSendNotification(
                        notificationTitle,
                        notificationMessage,
                        targetPayload,
                        selectedPriority.key,
                        sendEmailChecked,
                        sendInAppChecked,
                        sendPushChecked
                    ) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isSendingNotification) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Broadcasting Announcement...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Broadcast to $recipientCount Engineer${if (recipientCount != 1) "s" else ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // --- 7. Broadcast History Section with Read Receipts & Retraction ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BROADCAST HISTORY & AUDIT (${state.notifications.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onRefreshNotifications) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (state.notifications.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No broadcasts dispatched yet. Create your first announcement above!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(state.notifications) { notif ->
                NotificationHistoryCard(
                    notif = notif,
                    onClone = {
                        notificationTitle = notif.title ?: ""
                        notificationMessage = notif.rawMessage ?: ""
                        when (notif.priority?.lowercase()) {
                            "urgent" -> selectedPriority = NotificationPriority.URGENT
                            "shift_update" -> selectedPriority = NotificationPriority.SHIFT_UPDATE
                            "broadcast" -> selectedPriority = NotificationPriority.BROADCAST
                            else -> selectedPriority = NotificationPriority.INFO
                        }
                        Toast.makeText(context, "Loaded notice into composer!", Toast.LENGTH_SHORT).show()
                    },
                    onRetract = { nid ->
                        onRetractNotification(nid) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // --- Mention Engineer Modal Dialog ---
    if (showMentionDialog) {
        AlertDialog(
            onDismissRequest = { showMentionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Text("Mention an Engineer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Everyone option
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notificationMessage += " @all "
                                    showMentionDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("@", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Everyone (Team)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("@all", fontSize = 11.sp, color = Color(0xFF8B5CF6))
                                }
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    items(activeEngineers) { eng ->
                        val engKey = eng.id.ifBlank { eng.username }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notificationMessage += " @${eng.username} "
                                    if (targetMode == "specific" && !selectedEngineerIds.contains(engKey)) {
                                        selectedEngineerIds.add(engKey)
                                    }
                                    showMentionDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            eng.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("@${eng.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMentionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun NotificationHistoryCard(
    notif: NotificationDto,
    onClone: () -> Unit = {},
    onRetract: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showRetractConfirm by remember { mutableStateOf(false) }

    val isRetracted = notif.isRetracted == true

    val pColor = when (notif.priority?.lowercase()) {
        "urgent" -> Color(0xFFEF4444)
        "shift_update" -> Color(0xFFF59E0B)
        "broadcast" -> Color(0xFF8B5CF6)
        else -> Color(0xFF3B82F6)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRetracted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isRetracted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Priority, Retracted Badge, Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = (notif.priority ?: "INFO").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isRetracted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF64748B).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "🚫 RETRACTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = notif.title ?: "Untitled Notice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = notif.createdAt?.take(16) ?: "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Metric Pills (Targeted, Read Receipts, FCM Pushes, Emails)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Targeted
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "👥 ${notif.recipientsCount ?: 0} Targeted",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Read Receipts Breakdown
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "✓ ${notif.readCount ?: 0} Read / ⏳ ${notif.pendingCount ?: 0} Pending",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // FCM Pushes
                if ((notif.pushDispatched ?: 0) > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🔔 ${notif.pushDispatched} FCM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Emails
                if ((notif.emailsDispatched ?: 0) > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "✉ ${notif.emailsDispatched} Mails",
                            fontSize = 10.sp,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Card Action Buttons (Clone, Details, Retract)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClone,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clone", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expanded) "Hide" else "Details", fontSize = 11.sp)
                }

                if (!isRetracted && notif.id != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showRetractConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retract", fontSize = 11.sp)
                    }
                }
            }

            // Expanded Details View
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text(
                        text = "Message Content:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notif.rawMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // Read by engineers
                    if (!notif.readBy.isNullOrEmpty()) {
                        Text(
                            text = "Read / Dismissed by (${notif.readBy.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notif.readBy.joinToString(", "),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Pending engineers
                    if (!notif.pending.isNullOrEmpty()) {
                        Text(
                            text = "Pending Acknowledgment (${notif.pending.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notif.pending.joinToString(", "),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Retract Confirmation Dialog
    if (showRetractConfirm && notif.id != null) {
        AlertDialog(
            onDismissRequest = { showRetractConfirm = false },
            title = { Text("Retract Broadcast Announcement?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will immediately retract and hide the broadcast from all engineer mobile feeds and in-app views while preserving audit history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRetractConfirm = false
                        onRetract(notif.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Retraction")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRetractConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
