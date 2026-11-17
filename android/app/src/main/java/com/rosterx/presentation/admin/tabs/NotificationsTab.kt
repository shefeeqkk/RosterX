package com.rosterx.presentation.admin.tabs

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import com.rosterx.presentation.components.NeumorphicButton
import com.rosterx.presentation.components.NeumorphicButtonVariant
import com.rosterx.presentation.components.NeumorphicCard
import com.rosterx.presentation.components.NeumorphicElevationStyle
import com.rosterx.presentation.components.NeumorphicFormInput
import com.rosterx.ui.neumorphic.*
import com.rosterx.ui.theme.LocalIsDarkTheme

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
    onRefreshNotifications: () -> Unit,
    onSaveTemplate: ((name: String, title: String, message: String, priority: String, onComplete: (Boolean, String) -> Unit) -> Unit)? = null,
    onDeleteTemplate: ((id: String, onComplete: (Boolean, String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    // Active (non-resigned) engineers for recipient selection
    val activeEngineers = remember(state.engineers) {
        state.engineers.filter { it.status != EngineerStatus.RESIGNED && !it.isManagementAccount }
    }

    // Segmented Navigation: 0 = Compose, 1 = History
    var activeTabSection by rememberSaveable { mutableIntStateOf(0) }

    // Form states - Clean slate on initialization (no hardcoded default announcement)
    var targetMode by rememberSaveable { mutableStateOf("everyone") } // "everyone" or "specific"
    val selectedEngineerIds = remember { mutableStateListOf<String>() }
    var notificationTitle by rememberSaveable { mutableStateOf("") }
    var messageTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val notificationMessage = messageTextFieldValue.text
    var selectedPriority by rememberSaveable { mutableStateOf(NotificationPriority.INFO) }

    // Channel toggles
    var sendInAppChecked by rememberSaveable { mutableStateOf(true) }
    var sendPushChecked by rememberSaveable { mutableStateOf(true) }
    var sendEmailChecked by rememberSaveable { mutableStateOf(true) }

    // Dialog states for Save as Template & Delete Template
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }
    var templateToDelete by remember { mutableStateOf<BroadcastTemplateDto?>(null) }

    // Cursor-aware live '@' Mention detector
    val textBeforeCursor = remember(messageTextFieldValue) {
        val sel = messageTextFieldValue.selection.start.coerceIn(0, messageTextFieldValue.text.length)
        messageTextFieldValue.text.take(sel)
    }
    val mentionMatch = remember(textBeforeCursor) {
        Regex("@([a-zA-Z0-9_\\-\\.]*)$").find(textBeforeCursor)
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

    // Function to safely insert a mention tag at the cursor
    val insertMention: (String) -> Unit = { tag ->
        val q = mentionQuery ?: ""
        val selStart = messageTextFieldValue.selection.start.coerceIn(0, messageTextFieldValue.text.length)
        val prefix = messageTextFieldValue.text.take(selStart).dropLast(q.length + 1)
        val suffix = messageTextFieldValue.text.drop(selStart)
        val inserted = "@$tag "
        val newText = prefix + inserted + suffix
        val newPos = prefix.length + inserted.length
        messageTextFieldValue = TextFieldValue(newText, TextRange(newPos))
    }

    // Live preview recipient selector
    var previewTargetIndex by remember { mutableIntStateOf(0) }
    val previewEngineer = activeEngineers.getOrNull(previewTargetIndex) ?: activeEngineers.firstOrNull()

    // Calculated personalized preview with @mention resolution to Full Names
    val resolvedPreviewMessage = remember(notificationMessage, previewEngineer, activeEngineers) {
        val engName = previewEngineer?.fullName?.takeIf { it.isNotBlank() }
            ?: previewEngineer?.name
            ?: "Engineer"
        var text = notificationMessage
        text = text.replace(Regex("@name\\b", RegexOption.IGNORE_CASE), engName)
        text = text.replace(Regex("\\{\\s*(?:Their\\s+name|their\\s+name|name)\\s*\\}", RegexOption.IGNORE_CASE), engName)
        text = text.replace(Regex("\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), previewEngineer?.username ?: "engineer")
        text = text.replace(Regex("@\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), "@${previewEngineer?.username ?: "engineer"}")

        val mentionRegex = Regex("@([a-zA-Z0-9_\\-\\.]+)")
        text = mentionRegex.replace(text) { matchResult ->
            val uname = matchResult.groupValues[1]
            if (uname.equals("all", ignoreCase = true) || uname.equals("everyone", ignoreCase = true)) {
                "@everyone"
            } else if (uname.equals("name", ignoreCase = true)) {
                engName
            } else {
                val found = activeEngineers.find { it.username.equals(uname, ignoreCase = true) }
                val resolved = found?.fullName?.takeIf { it.isNotBlank() } ?: found?.name ?: uname
                "@$resolved"
            }
        }
        text
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // --- 1. Top Header Banner ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .neumorphicRaised(
                                shape = CircleShape,
                                elevation = NeumorphicTokens.ElevationRaisedSubtle
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = NeumorphicTokens.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TEAM BROADCAST CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.Accent,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Multi-Channel Operations Dispatcher",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                    }
                }

                if (activeTabSection == 1) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .neumorphicRaised(
                                shape = CircleShape,
                                elevation = NeumorphicTokens.ElevationRaisedSubtle
                            )
                            .clip(CircleShape)
                            .clickable { onRefreshNotifications() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh History",
                            tint = NeumorphicTokens.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Neumorphic Segmented Tab Row ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        shape = RoundedCornerShape(16.dp),
                        elevation = NeumorphicTokens.ElevationInsetSubtle
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Segment 0: Compose Broadcast
                    val isComposeActive = activeTabSection == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .then(
                                if (isComposeActive) {
                                    Modifier
                                        .neumorphicAccentGlow(radius = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeumorphicTokens.AccentGradient)
                                } else {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { activeTabSection = 0 }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isComposeActive) Color.White else NeumorphicTokens.TextMuted
                            )
                            Text(
                                text = "Compose Broadcast",
                                fontSize = 12.5.sp,
                                fontWeight = if (isComposeActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isComposeActive) Color.White else NeumorphicTokens.TextMuted
                            )
                        }
                    }

                    // Segment 1: Broadcast History
                    val isHistoryActive = activeTabSection == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .then(
                                if (isHistoryActive) {
                                    Modifier
                                        .neumorphicAccentGlow(radius = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeumorphicTokens.AccentGradient)
                                } else {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { activeTabSection = 1 }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isHistoryActive) Color.White else NeumorphicTokens.TextMuted
                            )
                            Text(
                                text = "Broadcast History (${state.notifications.size})",
                                fontSize = 12.5.sp,
                                fontWeight = if (isHistoryActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isHistoryActive) Color.White else NeumorphicTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 0: COMPOSE BROADCAST
        // ==========================================
        if (activeTabSection == 0) {
            // --- Custom Saved Templates Strip (Visible ONLY if custom templates exist) ---
            if (state.broadcastTemplates.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAVED TEMPLATES (${state.broadcastTemplates.size})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tap to load",
                                fontSize = 10.sp,
                                color = NeumorphicTokens.TextMuted.copy(alpha = 0.7f)
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.broadcastTemplates) { tpl ->
                                Row(
                                    modifier = Modifier
                                        .neumorphicRaised(
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = NeumorphicTokens.ElevationRaisedSubtle
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            notificationTitle = tpl.title ?: tpl.name ?: ""
                                            val msg = tpl.message ?: ""
                                            messageTextFieldValue = TextFieldValue(msg, TextRange(msg.length))
                                            when (tpl.priority?.lowercase()) {
                                                "urgent" -> selectedPriority = NotificationPriority.URGENT
                                                "shift_update" -> selectedPriority = NotificationPriority.SHIFT_UPDATE
                                                "broadcast" -> selectedPriority = NotificationPriority.BROADCAST
                                                else -> selectedPriority = NotificationPriority.INFO
                                            }
                                            Toast.makeText(context, "Loaded template: ${tpl.name ?: tpl.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Article,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = NeumorphicTokens.Accent
                                    )
                                    Text(
                                        text = tpl.name ?: tpl.title ?: "Template",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTokens.TextMain
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { templateToDelete = tpl },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete template",
                                            modifier = Modifier.size(12.dp),
                                            tint = NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Compose Card ---
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = NeumorphicTokens.ShapeContainer,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Card Header Row: Title & Save as Template Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMPOSE ANNOUNCEMENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMuted,
                                letterSpacing = 1.sp
                            )

                            // Neumorphic Save as Template Button
                            Box(
                                modifier = Modifier
                                    .neumorphicRaised(
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = NeumorphicTokens.ElevationRaisedSubtle
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (notificationTitle.isBlank() && notificationMessage.isBlank()) {
                                            Toast.makeText(context, "Enter a title or message first to save as template", Toast.LENGTH_SHORT).show()
                                        } else {
                                            templateNameInput = notificationTitle.takeIf { it.isNotBlank() } ?: "Operations Notice"
                                            showSaveTemplateDialog = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NeumorphicTokens.Accent
                                    )
                                    Text(
                                        text = "Save as Template",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTokens.Accent
                                    )
                                }
                            }
                        }

                        // Target Audience Selector Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Target Audience:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicInset(
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = NeumorphicTokens.ElevationInsetSubtle
                                    )
                                    .padding(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Everyone option
                                    val isEveryone = targetMode == "everyone"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .then(
                                                if (isEveryone) {
                                                    Modifier
                                                        .neumorphicAccentGlow(radius = 4.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(NeumorphicTokens.AccentGradient)
                                                } else {
                                                    Modifier
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .clickable { targetMode = "everyone" }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🌐 Everyone (${activeEngineers.size})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isEveryone) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isEveryone) Color.White else NeumorphicTokens.TextMuted
                                        )
                                    }

                                    // Specific Engineers option
                                    val isSpecific = targetMode == "specific"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .then(
                                                if (isSpecific) {
                                                    Modifier
                                                        .neumorphicAccentGlow(radius = 4.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(NeumorphicTokens.AccentGradient)
                                                } else {
                                                    Modifier
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .clickable { targetMode = "specific" }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "👤 Specific (${selectedEngineerIds.size})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSpecific) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSpecific) Color.White else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }

                            // Specific Engineers Multi-Select Grid
                            AnimatedVisibility(visible = targetMode == "specific") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Select engineers to notify:",
                                            fontSize = 11.sp,
                                            color = NeumorphicTokens.TextMuted
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
                                                text = if (selectedEngineerIds.size == activeEngineers.size) "Deselect All" else "Select All",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeumorphicTokens.Accent
                                            )
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activeEngineers.forEach { eng ->
                                            val engKey = eng.id.ifBlank { eng.username }
                                            val isSelected = selectedEngineerIds.contains(engKey)
                                            Box(
                                                modifier = Modifier
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier
                                                                .neumorphicRaised(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    elevation = NeumorphicTokens.ElevationRaisedSubtle
                                                                )
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(NeumorphicTokens.Accent.copy(alpha = 0.15f))
                                                                .border(1.dp, NeumorphicTokens.Accent, RoundedCornerShape(8.dp))
                                                        } else {
                                                            Modifier
                                                                .neumorphicInset(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    elevation = NeumorphicTokens.ElevationInsetSubtle
                                                                )
                                                                .clip(RoundedCornerShape(8.dp))
                                                        }
                                                    )
                                                    .clickable {
                                                        if (isSelected) selectedEngineerIds.remove(engKey)
                                                        else selectedEngineerIds.add(engKey)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) NeumorphicTokens.Accent else NeumorphicTokens.TextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Delivery Channels Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Delivery Channels:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Channel 1: In-App
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .then(
                                            if (sendInAppChecked) {
                                                Modifier
                                                    .neumorphicRaised(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationRaisedSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(10.dp))
                                            } else {
                                                Modifier
                                                    .neumorphicInset(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                            }
                                        )
                                        .clickable { sendInAppChecked = !sendInAppChecked },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (sendInAppChecked) Color(0xFF3B82F6) else NeumorphicTokens.TextMuted
                                        )
                                        Text(
                                            text = "In-App",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (sendInAppChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sendInAppChecked) Color(0xFF3B82F6) else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }

                                // Channel 2: FCM Push
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .then(
                                            if (sendPushChecked) {
                                                Modifier
                                                    .neumorphicRaised(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationRaisedSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                                            } else {
                                                Modifier
                                                    .neumorphicInset(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                            }
                                        )
                                        .clickable { sendPushChecked = !sendPushChecked },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (sendPushChecked) Color(0xFFF59E0B) else NeumorphicTokens.TextMuted
                                        )
                                        Text(
                                            text = "Push (FCM)",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (sendPushChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sendPushChecked) Color(0xFFF59E0B) else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }

                                // Channel 3: Email
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .then(
                                            if (sendEmailChecked) {
                                                Modifier
                                                    .neumorphicRaised(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationRaisedSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                            } else {
                                                Modifier
                                                    .neumorphicInset(shape = RoundedCornerShape(10.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
                                                    .clip(RoundedCornerShape(10.dp))
                                            }
                                        )
                                        .clickable { sendEmailChecked = !sendEmailChecked },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (sendEmailChecked) Color(0xFF10B981) else NeumorphicTokens.TextMuted
                                        )
                                        Text(
                                            text = "Email",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (sendEmailChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sendEmailChecked) Color(0xFF10B981) else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Priority Level Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Priority Level:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NotificationPriority.values().forEach { priority ->
                                    val isSelected = selectedPriority == priority
                                    Box(
                                        modifier = Modifier
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .neumorphicRaised(
                                                            shape = RoundedCornerShape(10.dp),
                                                            elevation = NeumorphicTokens.ElevationRaisedSubtle
                                                        )
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(priority.color.copy(alpha = 0.18f))
                                                        .border(1.dp, priority.color, RoundedCornerShape(10.dp))
                                                } else {
                                                    Modifier
                                                        .neumorphicInset(
                                                            shape = RoundedCornerShape(10.dp),
                                                            elevation = NeumorphicTokens.ElevationInsetSubtle
                                                        )
                                                        .clip(RoundedCornerShape(10.dp))
                                                }
                                            )
                                            .clickable { selectedPriority = priority }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = priority.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) priority.color else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Notification Title Input (Sunken Well)
                        NeumorphicFormInput(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = "Notification Title *",
                            placeholder = "Enter broadcast title...",
                            leadingIcon = {
                                Icon(Icons.Default.Title, contentDescription = null, tint = NeumorphicTokens.Accent, modifier = Modifier.size(16.dp))
                            }
                        )

                        // Message Body Editor (Sunken Multiline Well)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Message Body *",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted,
                                letterSpacing = 0.2.sp
                            )

                            val isDark = LocalIsDarkTheme.current
                            val borderColor = if (isDark) Color(0x1FFFFFFF) else Color(0x1F94A3B8)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                                    .neumorphicInset(
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = NeumorphicTokens.ElevationInsetSubtle,
                                        surfaceColor = NeumorphicTokens.InputSurface
                                    )
                                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                if (messageTextFieldValue.text.isEmpty()) {
                                    Text(
                                        text = "Type broadcast message...",
                                        fontSize = 13.5.sp,
                                        color = NeumorphicTokens.TextMuted.copy(alpha = 0.6f)
                                    )
                                }
                                BasicTextField(
                                    value = messageTextFieldValue,
                                    onValueChange = { messageTextFieldValue = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        color = NeumorphicTokens.TextMain,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 19.sp
                                    ),
                                    cursorBrush = SolidColor(NeumorphicTokens.Accent)
                                )
                            }

                            // Subtle Helper Tip Description
                            Text(
                                text = "💡 Tip: Type @ to insert dynamic @name, alert @all, or mention specific teammates.",
                                fontSize = 11.sp,
                                color = NeumorphicTokens.TextMuted,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                            )

                            // Live '@' Mention Autocomplete Popup Dropdown
                            AnimatedVisibility(
                                visible = isMentionActive && (
                                    matchingMentionEngineers.isNotEmpty() ||
                                    "name".contains(mentionQuery ?: "") ||
                                    "all".contains(mentionQuery ?: "") ||
                                    "everyone".contains(mentionQuery ?: "")
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .neumorphicFloating(
                                            shape = RoundedCornerShape(14.dp),
                                            surfaceColor = NeumorphicTokens.Surface
                                        )
                                        .border(1.dp, NeumorphicTokens.Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "SUGGESTED MENTIONS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NeumorphicTokens.Accent,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )

                                        // 1. Dynamic @name
                                        if ("name".contains(mentionQuery ?: "") || (mentionQuery ?: "").isEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { insertMention("name") }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(NeumorphicTokens.Accent.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp),
                                                        tint = NeumorphicTokens.Accent
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "@name",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeumorphicTokens.TextMain
                                                    )
                                                    Text(
                                                        text = "Dynamic Recipient Name — Personalizes per recipient",
                                                        fontSize = 10.5.sp,
                                                        color = NeumorphicTokens.TextMuted
                                                    )
                                                }
                                            }
                                        }

                                        // 2. Alert @all
                                        if ("all".contains(mentionQuery ?: "") || "everyone".contains(mentionQuery ?: "") || (mentionQuery ?: "").isEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { insertMention("all") }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Groups,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp),
                                                        tint = Color(0xFF8B5CF6)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "@all",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeumorphicTokens.TextMain
                                                    )
                                                    Text(
                                                        text = "Alert entire operational team",
                                                        fontSize = 10.5.sp,
                                                        color = NeumorphicTokens.TextMuted
                                                    )
                                                }
                                            }
                                        }

                                        // 3. Teammates matching query
                                        matchingMentionEngineers.forEach { eng ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { insertMention(eng.username) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(NeumorphicTokens.SurfaceContrast),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = eng.name.take(1).uppercase(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeumorphicTokens.Accent
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = eng.fullName?.takeIf { it.isNotBlank() } ?: eng.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = NeumorphicTokens.TextMain
                                                    )
                                                    Text(
                                                        text = "@${eng.username}",
                                                        fontSize = 10.5.sp,
                                                        color = NeumorphicTokens.Accent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live Personalized Recipient Preview Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicInset(
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = NeumorphicTokens.ElevationInsetSubtle
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = NeumorphicTokens.Accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "LIVE PREVIEW",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeumorphicTokens.Accent,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // Switch preview recipient
                                    if (activeEngineers.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .neumorphicRaised(
                                                    shape = RoundedCornerShape(8.dp),
                                                    elevation = NeumorphicTokens.ElevationRaisedSubtle
                                                )
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    previewTargetIndex = (previewTargetIndex + 1) % activeEngineers.size
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "As: ${previewEngineer?.name ?: "Recipient"}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeumorphicTokens.Accent
                                                )
                                                Icon(
                                                    Icons.Default.SwapHoriz,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = NeumorphicTokens.Accent
                                                )
                                            }
                                        }
                                    }
                                }

                                // Enabled channels badges
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (sendInAppChecked) {
                                        Text(
                                            "✓ In-App",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B82F6),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (sendPushChecked) {
                                        Text(
                                            "🔔 Push",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (sendEmailChecked) {
                                        Text(
                                            "✉ Email",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Message Preview Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicRaised(
                                            shape = RoundedCornerShape(10.dp),
                                            elevation = NeumorphicTokens.ElevationRaisedSubtle
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = notificationTitle.ifBlank { "(Untitled Notice)" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NeumorphicTokens.TextMain
                                            )
                                            Text(
                                                text = selectedPriority.label.uppercase(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = selectedPriority.color,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(selectedPriority.color.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = resolvedPreviewMessage.ifBlank { "(Type message above to preview dynamic personalization)" },
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp,
                                            color = if (resolvedPreviewMessage.isNotBlank()) NeumorphicTokens.TextMain else NeumorphicTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Dispatch Broadcast Button
                        val recipientCount = if (targetMode == "everyone") activeEngineers.size else selectedEngineerIds.size
                        val hasChannel = sendInAppChecked || sendPushChecked || sendEmailChecked
                        val canSend = notificationTitle.isNotBlank() && notificationMessage.isNotBlank() && recipientCount > 0 && hasChannel && !state.isSendingNotification

                        NeumorphicButton(
                            onClick = {
                                if (targetMode == "specific" && selectedEngineerIds.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one engineer.", Toast.LENGTH_SHORT).show()
                                    return@NeumorphicButton
                                }
                                if (!hasChannel) {
                                    Toast.makeText(context, "Please enable at least one delivery channel.", Toast.LENGTH_SHORT).show()
                                    return@NeumorphicButton
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
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            variant = NeumorphicButtonVariant.PrimaryAccent,
                            shape = RoundedCornerShape(14.dp),
                            enabled = canSend,
                            isLoading = state.isSendingNotification
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dispatch to $recipientCount Engineer${if (recipientCount != 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 1: BROADCAST HISTORY & AUDIT
        // ==========================================
        if (activeTabSection == 1) {
            if (state.notifications.isEmpty()) {
                item {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = NeumorphicTokens.ShapeContainer,
                        contentPadding = 32.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Broadcasts Dispatched",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.TextMain
                            )
                            Text(
                                text = "Switch to Compose tab to dispatch your first announcement!",
                                fontSize = 12.sp,
                                color = NeumorphicTokens.TextMuted
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
                            val msg = notif.rawMessage ?: ""
                            messageTextFieldValue = TextFieldValue(msg, TextRange(msg.length))
                            when (notif.priority?.lowercase()) {
                                "urgent" -> selectedPriority = NotificationPriority.URGENT
                                "shift_update" -> selectedPriority = NotificationPriority.SHIFT_UPDATE
                                "broadcast" -> selectedPriority = NotificationPriority.BROADCAST
                                else -> selectedPriority = NotificationPriority.INFO
                            }
                            activeTabSection = 0
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
    }

    // --- Save as Template Dialog ---
    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = NeumorphicTokens.Accent)
                    Text("Save as Template", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a memorable name for this reusable broadcast template:",
                        fontSize = 12.sp,
                        color = NeumorphicTokens.TextMuted
                    )

                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        label = { Text("Template Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (notificationTitle.isNotBlank()) {
                        Text(
                            text = "Title: $notificationTitle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMain
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = templateNameInput.trim().ifBlank { notificationTitle.takeIf { it.isNotBlank() } ?: "Custom Template" }
                        onSaveTemplate?.invoke(name, notificationTitle, notificationMessage, selectedPriority.key) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        showSaveTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicTokens.Accent)
                ) {
                    Text("Save Template", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Delete Template Confirmation Dialog ---
    if (templateToDelete != null) {
        val tpl = templateToDelete!!
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Delete Template?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete template '${tpl.name ?: tpl.title}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate?.invoke(tpl.id) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("Cancel")
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
    val isCelebration = notif.celebrationType != null || notif.sender?.contains("Celebration", ignoreCase = true) == true || notif.title?.contains("🎂") == true || notif.title?.contains("🏆") == true

    val pColor = when {
        isCelebration -> Color(0xFFF59E0B)
        notif.priority?.lowercase() == "urgent" -> Color(0xFFEF4444)
        notif.priority?.lowercase() == "shift_update" -> Color(0xFFF59E0B)
        notif.priority?.lowercase() == "broadcast" -> Color(0xFF8B5CF6)
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
                            text = if (isCelebration) "CELEBRATION 🎉" else (notif.priority ?: "INFO").uppercase(),
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
                    text = formatNotificationTimestamp(notif),
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

                    // Sender Signature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sender Signature:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = notif.sender ?: "System Administrator",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3-Card Delivery Breakdown Panel
                    Text(
                        text = "Delivery Audit Breakdown:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: FCM Push
                        val fcmCount = notif.fcmDelivery?.sentCount ?: notif.pushDispatched ?: 0
                        val fcmStatus = notif.fcmDelivery?.status ?: if (fcmCount > 0) "Delivered" else "No Active Tokens"
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🔔", fontSize = 12.sp)
                                    Text("FCM Push", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                                Text("$fcmCount Devices", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (fcmCount > 0) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = fcmStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fcmCount > 0) Color(0xFF059669) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Card 2: In-App Feed
                        val inappCount = notif.inappDelivery?.sentCount ?: notif.recipientsCount ?: 0
                        val inappStatus = notif.inappDelivery?.status ?: "Published"
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF8B5CF6).copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("📱", fontSize = 12.sp)
                                    Text("In-App Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                }
                                Text("$inappCount Targets", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = inappStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Card 3: Email Broadcast
                        val emailCount = notif.emailDelivery?.sentCount ?: notif.emailsDispatched ?: 0
                        val emailStatus = notif.emailDelivery?.status ?: if (emailCount > 0) "Dispatched" else "Disabled"
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3B82F6).copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("✉", fontSize = 12.sp)
                                    Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                                Text("$emailCount Mails", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (emailCount > 0) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = emailStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (emailCount > 0) Color(0xFF059669) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

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

fun formatNotificationTimestamp(notif: NotificationDto): String {
    val raw = notif.createdAt ?: return "Recently"
    return try {
        val instant = when {
            raw.endsWith(" UTC") -> {
                val iso = raw.removeSuffix(" UTC").replace(' ', 'T') + "Z"
                java.time.Instant.parse(iso)
            }
            raw.contains("T") -> java.time.Instant.parse(raw)
            else -> null
        }
        if (instant != null) {
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
            zdt.format(formatter)
        } else {
            raw.take(16)
        }
    } catch (_: Exception) {
        notif.createdAt?.take(16) ?: "Recently"
    }
}
