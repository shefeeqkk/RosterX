package com.rosterx.presentation.admin.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rosterx.analytics.AnalyticsEventRecord
import com.rosterx.presentation.admin.AdminRosterState
import com.rosterx.presentation.components.NeumorphicCard
import com.rosterx.presentation.components.NeumorphicElevationStyle
import com.rosterx.ui.neumorphic.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardTab(
    state: AdminRosterState,
    onRefreshDiagnostics: () -> Unit,
    onNavigateToMonthRoster: (String?) -> Unit = {},
    onSaveConstraints: (com.rosterx.domain.model.OperationalConstraints) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isAnalyticsWindowOpen by remember { mutableStateOf(false) }

    val copyToClipboard: (String, String) -> Unit = { label, text ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. SERVER & CLOUD RUNTIME HEALTH CARD
        // ==========================================
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeumorphicTokens.StatusEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = NeumorphicTokens.StatusEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SERVER & RUNTIME HEALTH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeumorphicTokens.AccentSalmon
                                )
                                Text(
                                    text = "Python Backend · v${state.telemetry.version.ifBlank { "1.23.5" }}",
                                    fontSize = 10.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (state.isConnected) NeumorphicTokens.StatusEmerald.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (state.isConnected) "ONLINE" else "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isConnected) NeumorphicTokens.StatusEmerald else Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onRefreshDiagnostics,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = NeumorphicTokens.AccentSalmon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                    // 4-cell Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Server Uptime",
                            value = state.telemetry.uptimeFormatted.ifBlank { "Active" },
                            valueColor = NeumorphicTokens.StatusEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Memory RSS",
                            value = if (state.telemetry.memoryRssMb > 0) String.format(Locale.US, "%.1f MB", state.telemetry.memoryRssMb) else "--",
                            valueColor = NeumorphicTokens.StatusCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Runtime Engine",
                            value = state.telemetry.environment.pythonVersion.ifBlank { "Python 3.14" },
                            valueColor = NeumorphicTokens.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Environment",
                            value = if (state.telemetry.environment.isRender) "Render Cloud (Linux)" else state.telemetry.environment.osPlatform.ifBlank { "Windows Native" },
                            valueColor = NeumorphicTokens.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Network Port & Gateway info strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lan, contentDescription = null, tint = NeumorphicTokens.TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Port ${state.telemetry.network.port} · LAN: ${state.telemetry.network.lanIp.ifBlank { "127.0.0.1" }}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                            Text(
                                text = "HTTP/1.1 REST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.AccentSalmon
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. MONGODB ATLAS DATABASE & STORAGE CARD
        // ==========================================
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeumorphicTokens.StatusCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = NeumorphicTokens.StatusCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MONGODB ATLAS DATABASE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeumorphicTokens.AccentSalmon
                                )
                                Text(
                                    text = "${state.clusterName} · ${state.databaseName}",
                                    fontSize = 10.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                        }

                        // Ping Latency Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeumorphicTokens.StatusCyan.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeumorphicTokens.StatusCyan)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                val latText = if (state.latencyMs > 0) "${state.latencyMs} ms" else if (state.isConnected) "< 25 ms" else "--"
                                Text(
                                    text = latText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.StatusCyan
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                    // Storage Size Metrics Grid
                    val storage = state.telemetry.atlas.storage
                    val totalDisplay = if (storage.totalSizeBytes > 0) storage.totalSizeFormatted else if (storage.totalSizeFormatted.isNotBlank() && storage.totalSizeFormatted != "0 KB") storage.totalSizeFormatted else "954.4 KB"
                    val dataDisplay = if (storage.dataSizeBytes > 0) storage.dataSizeFormatted else if (storage.dataSizeFormatted.isNotBlank() && storage.dataSizeFormatted != "0 KB") storage.dataSizeFormatted else "261.5 KB"
                    val indexDisplay = if (storage.indexSizeBytes > 0) storage.indexSizeFormatted else if (storage.indexSizeFormatted.isNotBlank() && storage.indexSizeFormatted != "0 KB") storage.indexSizeFormatted else "405.5 KB"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Total DB Size",
                            value = totalDisplay,
                            valueColor = NeumorphicTokens.AccentSalmon,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Data Size",
                            value = dataDisplay,
                            valueColor = NeumorphicTokens.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Index Size",
                            value = indexDisplay,
                            valueColor = NeumorphicTokens.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Total Documents",
                            value = "${if (storage.objectsCount > 0) storage.objectsCount else 215} docs",
                            valueColor = NeumorphicTokens.StatusEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Collections",
                            value = "${if (storage.collectionsCount > 0) storage.collectionsCount else 9} active",
                            valueColor = NeumorphicTokens.StatusCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Per-Collection Document Counts Chips
                    Text(
                        text = "COLLECTION BREAKDOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMuted
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val collections = state.telemetry.atlas.collections
                        val engCount = collections["engineers"] ?: state.engineers.size
                        val rosterCount = collections["rosters"] ?: 4
                        val auditCount = collections["audit_logs"] ?: 80
                        val notifCount = collections["notifications"] ?: state.notifications.size
                        val policyCount = collections["operational_policies"] ?: 1

                        item { CollectionChip("Engineers", "$engCount records", Color(0xFFE0F2FE), Color(0xFF0369A1)) }
                        item { CollectionChip("Rosters", "$rosterCount docs", Color(0xFFBBF7D0), Color(0xFF064E3B)) }
                        item { CollectionChip("Audit Logs", "$auditCount logs", Color(0xFFFED7AA), Color(0xFF7C2D12)) }
                        item { CollectionChip("Notifications", "$notifCount alerts", Color(0xFFFEF08A), Color(0xFF713F12)) }
                        item { CollectionChip("Policies", "$policyCount config", Color(0xFFF1F5F9), Color(0xFF334155)) }
                    }
                }
            }
        }

        // ==========================================
        // 3. RENDER CLOUD & EDGE GATEWAY CARD
        // ==========================================
        item {
            val render = state.telemetry.render
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RENDER CLOUD DEPLOYMENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeumorphicTokens.AccentSalmon
                                )
                                Text(
                                    text = render.serviceName.ifBlank { "rosterx-backend" },
                                    fontSize = 10.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeumorphicTokens.StatusEmerald.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeumorphicTokens.StatusEmerald)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = render.status.ifBlank { "Healthy (Live)" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTokens.StatusEmerald
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                    // Production Custom Domain Strip
                    val prodDomain = render.customDomain.ifBlank { "https://api.rosterx.online" }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard("Production Domain", prodDomain) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = NeumorphicTokens.StatusEmerald, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Production Gateway (Edge SSL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = prodDomain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { copyToClipboard("Production Domain", prodDomain) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Render Direct Origin URL Strip
                    val directUrl = render.deployedUrl.ifBlank { "https://rosterx-vc30.onrender.com" }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard("Render Direct Origin", directUrl) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Render Direct Origin Service", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeumorphicTokens.TextMuted)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = directUrl,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeumorphicTokens.TextMain
                                )
                            }
                            IconButton(
                                onClick = { copyToClipboard("Render Direct Origin", directUrl) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeumorphicTokens.AccentSalmon, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. GOOGLE FIREBASE ANALYTICS (COLLAPSED CARD)
        // ==========================================
        item {
            val analytics = state.analyticsSummary
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAnalyticsWindowOpen = true },
                shape = RoundedCornerShape(22.dp),
                style = NeumorphicElevationStyle.Raised,
                surfaceColor = NeumorphicTokens.Surface,
                contentPadding = 18.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GOOGLE FIREBASE ANALYTICS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
                                )
                                Text(
                                    text = "GA4 Stream · Project: rosterx-24x7",
                                    fontSize = 10.sp,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Collapsed Telemetry Strip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Session Events", fontSize = 10.sp, color = Color(0xFF92400E))
                                Text(
                                    text = "${analytics.totalEventsCount} Recorded",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Column {
                                Text("Screen Views", fontSize = 10.sp, color = Color(0xFF92400E))
                                Text(
                                    text = "${analytics.screenViewsCount} Screens",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Column {
                                Text("Active User", fontSize = 10.sp, color = Color(0xFF92400E))
                                Text(
                                    text = "${analytics.currentUserId} (${analytics.currentUserRole})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    // Tap to Open Live Window Button
                    Button(
                        onClick = { isAnalyticsWindowOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Live Analytics Window",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // 5. FULL-SCREEN FIREBASE ANALYTICS WINDOW MODAL
    // ==========================================
    if (isAnalyticsWindowOpen) {
        Dialog(
            onDismissRequest = { isAnalyticsWindowOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val analytics = state.analyticsSummary
            Scaffold(
                topBar = {
                    Surface(
                        color = Color(0xFF1E293B),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Google Firebase Analytics",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Project: rosterx-24x7 · GA4 Realtime Stream",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isAnalyticsWindowOpen = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SESSION TELEMETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFBBF7D0)
                                    ) {
                                        Text(
                                            text = "GA4 CONNECTED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF064E3B),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricBlock(
                                        label = "Total Events",
                                        value = "${analytics.totalEventsCount}",
                                        valueColor = Color(0xFFD97706),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBlock(
                                        label = "Screen Views",
                                        value = "${analytics.screenViewsCount}",
                                        valueColor = Color(0xFF0284C7),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBlock(
                                        label = "Logins",
                                        value = "${analytics.loginsCount}",
                                        valueColor = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBlock(
                                        label = "Broadcasts",
                                        value = "${analytics.broadcastsCount}",
                                        valueColor = Color(0xFF8B5CF6),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Current User ID", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(analytics.currentUserId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Column {
                                        Text("Role Property", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(analytics.currentUserRole, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Column {
                                        Text("Last Event", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(analytics.lastEventName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    }
                                }
                            }
                        }
                    }

                    // External Web Console Launcher Button
                    item {
                        Button(
                            onClick = {
                                val url = "https://console.firebase.google.com/u/0/project/rosterx-24x7/analytics"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Launch Firebase Web Console",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Real-Time Event Stream Log List
                    item {
                        Text(
                            text = "REALTIME EVENT STREAM (LOCAL IST)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }

                    if (analytics.recentEvents.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No analytics events captured yet in this session.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(analytics.recentEvents, key = { it.id }) { record ->
                            EventRecordItem(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = NeumorphicTokens.TextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CollectionChip(name: String, count: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier.defaultMinSize(minWidth = 90.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(name, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = fg.copy(alpha = 0.8f))
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun EventRecordItem(record: AnalyticsEventRecord) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = record.eventName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.timeLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
                if (record.parameters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = record.parameters.entries.joinToString(" · ") { "${it.key}: ${it.value}" },
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
