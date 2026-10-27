package com.rosterx.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.ShiftCode
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRosterScreen(
    viewModel: AdminRosterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Roster - ${state.currentYear}/${state.currentMonth}") },
                actions = {
                    Icon(
                        imageVector = if (state.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Connection Status",
                        tint = if (state.isConnected) Color.Green else Color.Red,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Retry")
                    }
                }
            } else {
                RosterGrid(
                    engineers = state.engineers,
                    roster = state.roster,
                    year = state.currentYear,
                    month = state.currentMonth
                )
            }
        }
    }
}

@Composable
fun RosterGrid(
    engineers: List<Engineer>,
    roster: Map<String, Map<String, ShiftCode>>,
    year: Int,
    month: Int
) {
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Engineer", fontWeight = FontWeight.Bold)
            }

            for (day in 1..daysInMonth) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Engineer Rows
        LazyColumn {
            items(engineers) { engineer ->
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Engineer Name sticky column equivalent
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = engineer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "@${engineer.username}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Shift Cells
                    val engRoster = roster[engineer.username] ?: emptyMap()
                    for (day in 1..daysInMonth) {
                        val shift = engRoster[day.toString()]
                        ShiftCell(shift = shift)
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun ShiftCell(shift: ShiftCode?) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(36.dp)
            .padding(horizontal = 2.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .background(
                color = if (shift != null) Color(android.graphics.Color.parseColor(shift.bgColorHex)) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (shift != null) {
            Text(
                text = shift.code,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(android.graphics.Color.parseColor(shift.textColorHex))
            )
        }
    }
}
