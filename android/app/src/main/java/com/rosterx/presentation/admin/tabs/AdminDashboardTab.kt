package com.rosterx.presentation.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.portal.getRosterForEngineer

data class RuleViolation(
    val rawEngineerName: String,
    val engineerName: String,
    val month: String,
    val dayNumber: Int,
    val dateLabel: String,
    val ruleDescription: String,
    val severity: String // "HIGH", "MEDIUM", "LOW"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardTab(
    state: AdminRosterState,
    onRefreshDiagnostics: () -> Unit,
    onNavigateToMonthRoster: (String?) -> Unit,
    onSaveConstraints: (com.rosterx.domain.model.OperationalConstraints) -> Unit = {}
) {
    // Editable Policy & AI Engine Specs State
    var showEditPolicyDialog by remember { mutableStateOf(false) }
    val activeModel = "Gemini 1.5 Pro"

    // Edit Policy Dialog (matching admin-web operational policies schema & syncing to Atlas)
    if (showEditPolicyDialog) {
        var tempNightHeadcount by remember { mutableStateOf(state.constraints.nightHeadcount.toString()) }
        var tempFsMin by remember { mutableStateOf(state.constraints.fsMin.toString()) }
        var tempSsMin by remember { mutableStateOf(state.constraints.ssMin.toString()) }
        var tempDaytimeOverlap by remember { mutableStateOf(state.constraints.daytimeOverlapMin.toString()) }
        var tempPostNightRest by remember { mutableStateOf(state.constraints.postNightRestDays.toString()) }
        var tempMaxConsecutive by remember { mutableStateOf(state.constraints.maxConsecutiveDays.toString()) }
        var tempSwapFirstPolicy by remember { mutableStateOf(state.constraints.swapFirstPolicy) }
        var tempCustomDirectives by remember { mutableStateOf(state.constraints.customDirectives) }

        AlertDialog(
            onDismissRequest = { showEditPolicyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Operational Policy & Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selected Best AI Model Info Badge (No selection needed)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Engine: $activeModel (Latest & Recommended)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempNightHeadcount,
                            onValueChange = { tempNightHeadcount = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Night Headcount", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempFsMin,
                            onValueChange = { tempFsMin = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Morning (FS) Min", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempSsMin,
                            onValueChange = { tempSsMin = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Second (SS) Min", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempDaytimeOverlap,
                            onValueChange = { tempDaytimeOverlap = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Daytime Overlap", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempPostNightRest,
                            onValueChange = { tempPostNightRest = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Post-Night Rest (Days)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempMaxConsecutive,
                            onValueChange = { tempMaxConsecutive = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Max Consecutive", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Swap-First Priority Policy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = tempSwapFirstPolicy,
                            onCheckedChange = { tempSwapFirstPolicy = it }
                        )
                    }

                    OutlinedTextField(
                        value = tempCustomDirectives,
                        onValueChange = { tempCustomDirectives = it },
                        label = { Text("Custom Directives & Notes", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = state.constraints.copy(
                            nightHeadcount = tempNightHeadcount.toIntOrNull() ?: 1,
                            fsMin = tempFsMin.toIntOrNull() ?: 1,
                            ssMin = tempSsMin.toIntOrNull() ?: 1,
                            daytimeOverlapMin = tempDaytimeOverlap.toIntOrNull() ?: 3,
                            postNightRestDays = tempPostNightRest.toIntOrNull() ?: 2,
                            maxConsecutiveDays = tempMaxConsecutive.toIntOrNull() ?: 6,
                            swapFirstPolicy = tempSwapFirstPolicy,
                            customDirectives = tempCustomDirectives.trim(),
                            aiModel = activeModel
                        )
                        onSaveConstraints(updated)
                        showEditPolicyDialog = false
                    }
                ) {
                    Text("Save to DB", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPolicyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val monthNames = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    val currentMonthShort = monthNames.getOrElse(state.currentMonth - 1) { "OCT" }

    // Audit violations from loaded roster according to corrected 24/7 rules
    val violations = remember(state.roster, state.engineers, state.currentYear, state.currentMonth) {
        val list = mutableListOf<RuleViolation>()
        val checkedEngineers = mutableSetOf<String>()

        state.engineers.forEach { eng ->
            val engKey = eng.id.ifBlank { eng.username }
            if (!checkedEngineers.add(engKey)) return@forEach

            val rawName = eng.name
            val username = eng.username
            val displayEngName = "${rawName} (${username.uppercase()})"

            val daysMap = getRosterForEngineer(state.roster, eng)
            var consecutiveWorkDays = 0
            var previousWasNight = false
            var previousShiftCode: String? = null

            daysMap.entries.sortedBy { it.key.toIntOrNull() ?: 0 }.forEach { (dayStr, shift) ->
                val dayNum = dayStr.toIntOrNull() ?: 0
                val shiftCode = shift.code
                val isRestOrSwap = shiftCode == "OFF" || shiftCode == "L" || shiftCode.startsWith("SW")

                if (shiftCode == "NS") {
                    // Continuous Night Shifts (NS -> NS) ARE ALLOWED!
                    previousWasNight = true
                } else if (isRestOrSwap) {
                    // Mandatory rest day taken! SW* shifts are considered as OFF so SW* after NS is OK!
                    previousWasNight = false
                } else {
                    // Working Day Shift (e.g. FS, FG, G, SG, SS)
                    if (previousWasNight) {
                        list.add(
                            RuleViolation(
                                rawEngineerName = rawName,
                                engineerName = displayEngName,
                                month = "$currentMonthShort ${state.currentYear}",
                                dayNumber = dayNum,
                                dateLabel = "$currentMonthShort $dayNum, ${state.currentYear}",
                                ruleDescription = "Shift $shiftCode on $currentMonthShort $dayNum immediately follows Night Shift (NS) without mandatory OFF/L/SW rest day.",
                                severity = "HIGH"
                            )
                        )
                        previousWasNight = false
                    }
                }

                // Check 6 consecutive working days rule (OFF, L, and SW* reset working days count)
                if (!isRestOrSwap) {
                    consecutiveWorkDays++
                    if (consecutiveWorkDays > 6) {
                        list.add(
                            RuleViolation(
                                rawEngineerName = rawName,
                                engineerName = displayEngName,
                                month = "$currentMonthShort ${state.currentYear}",
                                dayNumber = dayNum,
                                dateLabel = "$currentMonthShort $dayNum, ${state.currentYear}",
                                ruleDescription = "Exceeded 6 consecutive working days on $currentMonthShort $dayNum without mandatory weekly off.",
                                severity = "MEDIUM"
                            )
                        )
                    }
                } else {
                    consecutiveWorkDays = 0
                }

                previousShiftCode = shiftCode
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Server Health Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0284C7).copy(alpha = 0.2f),
                                    Color(0xFF4F46E5).copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (state.isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isConnected) "MONGODB ATLAS CONNECTED" else "SERVER DISCONNECTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (state.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }

                            IconButton(onClick = onRefreshDiagnostics) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Atlas Cluster", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.clusterName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Database", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.databaseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Latency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val latencyText = if (state.latencyMs > 0) "${state.latencyMs} ms" else if (state.isConnected) "< 25 ms" else "--"
                                Text(latencyText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // 2. AI Copilot & Policy Specs
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("OPERATIONAL POLICY & AI ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showEditPolicyDialog = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Policy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(activeModel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { PolicyBadge("Night Requirement", "${state.constraints.nightHeadcount} NS / Night", Color(0xFF1E293B), Color.White) }
                        item { PolicyBadge("Morning Presence", "≥${state.constraints.fsMin} FS Shift", Color(0xFFBBF7D0), Color(0xFF064E3B)) }
                        item { PolicyBadge("Evening Presence", "≥${state.constraints.ssMin} SS Shift", Color(0xFFFED7AA), Color(0xFF7C2D12)) }
                        item { PolicyBadge("Daytime Overlap", "≥${state.constraints.daytimeOverlapMin} Day Shifts", Color(0xFFE0F2FE), Color(0xFF0369A1)) }
                        item { PolicyBadge("Recovery Window", "${state.constraints.postNightRestDays} Rest Days Post-NS", Color(0xFFFEF08A), Color(0xFF713F12)) }
                        item { PolicyBadge("Max Consecutive", "${state.constraints.maxConsecutiveDays} Days Max", Color(0xFFF1F5F9), Color(0xFF334155)) }
                        item { PolicyBadge("Swap Arbitration", if (state.constraints.swapFirstPolicy) "Swap-First Policy" else "Standard Policy", Color(0xFFE2E8F0), Color(0xFF334155)) }
                    }
                }
            }
        }

        // 3. Real-Time Rule Violation Auditor (Compact Header so Counter is not squeezed)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "RULE AUDITOR ($currentMonthShort)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (violations.isEmpty()) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (violations.isEmpty()) "0 Clean" else "${violations.size} Issues",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (violations.isEmpty()) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    if (violations.isEmpty()) {
                        Text(
                            text = "All roster assignments strictly comply with 24/7 circadian fatigue & staffing rules.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            violations.forEach { violation ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "${violation.dateLabel} · ${violation.engineerName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFEF4444)
                                                )
                                                Text(
                                                    text = violation.ruleDescription,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onNavigateToMonthRoster(violation.rawEngineerName) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View Roster", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyBadge(title: String, value: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier
            .defaultMinSize(minHeight = 62.dp, minWidth = 115.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = fg.copy(alpha = 0.85f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 2
            )
        }
    }
}
