package com.rosterx.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosterx.data.local.UserSessionManager
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.ShiftCode
import com.rosterx.domain.repository.RosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import com.rosterx.domain.repository.RosterBackup
import javax.inject.Inject
import com.rosterx.presentation.portal.getRosterForEngineer

data class AdminRosterState(
    val isLoading: Boolean = false,
    val engineers: List<Engineer> = emptyList(),
    val roster: Map<String, Map<String, ShiftCode>> = emptyMap(),
    val currentYear: Int = 2026,
    val currentMonth: Int = 10,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    val clusterName: String = "Cluster0 (M0)",
    val databaseName: String = "rosterx",
    val latencyMs: Long = 0L,
    val serverLogs: List<String> = emptyList(),
    val backups: List<RosterBackup> = emptyList(),
    val isBackupsLoading: Boolean = false,
    val backupActionMessage: String? = null,
    val constraints: com.rosterx.domain.model.OperationalConstraints = com.rosterx.domain.model.OperationalConstraints(),
    val notifications: List<com.rosterx.data.remote.NotificationDto> = emptyList(),
    val isSendingNotification: Boolean = false,
    val notificationActionMessage: String? = null
)

@HiltViewModel
class AdminRosterViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AdminRosterState())
    val state: StateFlow<AdminRosterState> = _state.asStateFlow()

    init {
        checkConnection()
        loadData()
        loadNotifications()
    }

    fun checkConnection() {
        viewModelScope.launch {
            val diag = repository.getServerDiagnostics()
            _state.value = _state.value.copy(
                isConnected = diag.isConnected,
                clusterName = if (diag.cluster.isNotBlank()) "${diag.cluster} (M0)" else "Cluster0 (M0)",
                databaseName = if (diag.database.isNotBlank()) diag.database else "rosterx",
                latencyMs = diag.latencyMs,
                serverLogs = if (diag.logs.isNotEmpty()) diag.logs else _state.value.serverLogs
            )
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            val logs = repository.getServerLogs()
            if (logs.isNotEmpty()) {
                _state.value = _state.value.copy(serverLogs = logs)
            }
        }
    }

    fun getBaseUrl(): String = sessionManager.getBaseUrl()

    fun saveBaseUrl(url: String) {
        sessionManager.setBaseUrl(url)
        checkConnection()
        loadData()
    }

    fun loadData() {
        checkConnection()
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            // 1. Load Engineers
            val engineersResult = repository.getEngineers()
            val engineers = engineersResult.getOrElse { emptyList() }

            // 2. Load Roster for current month/year
            val monthKey = String.format(Locale.US, "%d-%02d", _state.value.currentYear, _state.value.currentMonth)
            val rosterResult = repository.getRoster(monthKey)
            
            val rosterMap = mutableMapOf<String, Map<String, ShiftCode>>()
            
            if (rosterResult.isSuccess) {
                val rawRoster = rosterResult.getOrNull() ?: emptyMap()
                rawRoster.forEach { (engId, daysMap) ->
                    val shiftMap = mutableMapOf<String, ShiftCode>()
                    daysMap.forEach { (day, shiftStr) ->
                        shiftMap[day] = ShiftCode.fromString(shiftStr)
                    }
                    rosterMap[engId] = shiftMap
                }
            }

            // 3. Load Operational Constraints
            val constraintsResult = repository.getConstraints()
            val loadedConstraints = constraintsResult.getOrDefault(com.rosterx.domain.model.OperationalConstraints())

            _state.value = _state.value.copy(
                isLoading = false,
                engineers = engineers,
                roster = rosterMap,
                constraints = loadedConstraints
            )
            loadBackups()
        }
    }

    fun loadBackups() {
        viewModelScope.launch {
            val monthKey = String.format(Locale.US, "%d-%02d", _state.value.currentYear, _state.value.currentMonth)
            _state.value = _state.value.copy(isBackupsLoading = true)
            val result = repository.getRosterBackups(monthKey)
            _state.value = _state.value.copy(
                isBackupsLoading = false,
                backups = result.getOrElse { emptyList() }
            )
        }
    }

    fun restoreBackup(backupId: String) {
        viewModelScope.launch {
            val monthKey = String.format(Locale.US, "%d-%02d", _state.value.currentYear, _state.value.currentMonth)
            _state.value = _state.value.copy(isLoading = true, backupActionMessage = "Restoring backup...")
            val result = repository.restoreRosterBackup(backupId, monthKey)
            if (result.isSuccess) {
                _state.value = _state.value.copy(backupActionMessage = "Roster successfully restored from backup!")
                loadData()
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to restore backup: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun previousMonth() {
        var newMonth = _state.value.currentMonth - 1
        var newYear = _state.value.currentYear
        if (newMonth < 1) {
            newMonth = 12
            newYear--
        }
        _state.value = _state.value.copy(currentYear = newYear, currentMonth = newMonth)
        loadData()
    }

    fun nextMonth() {
        var newMonth = _state.value.currentMonth + 1
        var newYear = _state.value.currentYear
        if (newMonth > 12) {
            newMonth = 1
            newYear++
        }
        _state.value = _state.value.copy(currentYear = newYear, currentMonth = newMonth)
        loadData()
    }

    fun syncEngineers(engineers: List<Engineer>) {
        viewModelScope.launch {
            repository.syncEngineers(engineers)
            loadData()
        }
    }

    fun deleteEngineer(engineer: Engineer, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.deleteEngineer(engineer.id, engineer.name, engineer.username)
            if (res.isSuccess) {
                val current = _state.value.engineers.filter { it.id != engineer.id && it.name.uppercase() != engineer.name.uppercase() }
                _state.value = _state.value.copy(engineers = current)
                loadData()
                onComplete(true, "Engineer ${engineer.name} successfully deleted.")
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed to delete engineer")
            }
        }
    }

    fun updateShift(engineer: Engineer, day: Int, shift: ShiftCode?) {
        val currentRoster = _state.value.roster.toMutableMap()
        val currentEngRoster = getRosterForEngineer(currentRoster, engineer).toMutableMap()
        if (shift == null) {
            currentEngRoster.remove(day.toString())
        } else {
            currentEngRoster[day.toString()] = shift
        }
        val keys = listOfNotNull(
            engineer.username.takeIf { it.isNotBlank() },
            engineer.id.takeIf { it.isNotBlank() },
            engineer.name.takeIf { it.isNotBlank() },
            engineer.name.uppercase().takeIf { it.isNotBlank() },
            engineer.name.lowercase().takeIf { it.isNotBlank() }
        )
        keys.forEach { k -> currentRoster[k] = currentEngRoster }
        _state.value = _state.value.copy(roster = currentRoster)
    }

    fun saveRoster(rosterMap: Map<String, Map<String, ShiftCode>>) {
        viewModelScope.launch {
            val monthKey = String.format(Locale.US, "%d-%02d", _state.value.currentYear, _state.value.currentMonth)
            val stringRosterMap = rosterMap.mapValues { (_, dayMap) ->
                dayMap.mapValues { it.value.code }
            }
            repository.saveRoster(monthKey, stringRosterMap)
            loadData()
        }
    }

    fun saveConstraints(constraints: com.rosterx.domain.model.OperationalConstraints) {
        viewModelScope.launch {
            _state.value = _state.value.copy(constraints = constraints)
            repository.saveConstraints(constraints)
            loadData()
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            repository.getNotifications().onSuccess { notifs ->
                _state.value = _state.value.copy(notifications = notifs)
            }
        }
    }

    fun sendNotification(
        title: String,
        message: String,
        target: Any,
        priority: String,
        sendEmail: Boolean,
        sendInApp: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingNotification = true)
            val result = repository.sendNotification(
                title = title,
                message = message,
                target = target,
                priority = priority,
                sendEmail = sendEmail,
                sendInApp = sendInApp
            )
            result.onSuccess { resp ->
                _state.value = _state.value.copy(
                    isSendingNotification = false,
                    notificationActionMessage = resp.message ?: "Notification dispatched successfully"
                )
                loadNotifications()
                onComplete(true, resp.message ?: "Broadcast sent successfully")
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isSendingNotification = false,
                    notificationActionMessage = "Failed: ${err.message}"
                )
                onComplete(false, err.message ?: "Dispatch failed")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
