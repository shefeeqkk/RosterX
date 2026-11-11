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

import java.time.LocalDate

import java.util.Locale

import com.rosterx.domain.repository.RosterBackup

import javax.inject.Inject

import com.rosterx.presentation.portal.getRosterForEngineer



data class AdminRosterState(

    val isLoading: Boolean = false,

    val engineers: List<Engineer> = emptyList(),

    val roster: Map<String, Map<String, ShiftCode>> = emptyMap(),

    val currentYear: Int = LocalDate.now().year,

    val currentMonth: Int = LocalDate.now().monthValue,

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

    val broadcastTemplates: List<com.rosterx.data.remote.BroadcastTemplateDto> = emptyList(),

    val isSendingNotification: Boolean = false,

    val notificationActionMessage: String? = null,
    val isAssistantOpen: Boolean = false,
    val isAssistantThinking: Boolean = false,
    val assistantMessages: List<com.rosterx.presentation.components.AssistantChatMessage> = emptyList(),
    val telemetry: com.rosterx.data.remote.ServerTelemetryDataDto = com.rosterx.data.remote.ServerTelemetryDataDto(),
    val isTelemetryLoading: Boolean = false,
    val analyticsSummary: com.rosterx.analytics.FirebaseAnalyticsSummary = com.rosterx.analytics.FirebaseAnalyticsSummary()
)



@HiltViewModel
class AdminRosterViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager,
    val analyticsManager: com.rosterx.analytics.AnalyticsManager
) : ViewModel() {

    private val _state = MutableStateFlow(AdminRosterState())
    private val rosterCache = mutableMapOf<String, Map<String, Map<String, ShiftCode>>>()
    val state: StateFlow<AdminRosterState> = _state.asStateFlow()
    val currentUser = sessionManager.currentUser

    fun resetToCurrentMonth() {
        val now = LocalDate.now()
        val monthKey = String.format(Locale.US, "%d-%02d", now.year, now.monthValue)
        val cached = rosterCache[monthKey]
        _state.value = _state.value.copy(
            currentYear = now.year,
            currentMonth = now.monthValue,
            roster = cached ?: _state.value.roster
        )
        loadData(silent = cached != null && _state.value.engineers.isNotEmpty())
    }

    init {
        checkConnection()
        refreshTelemetry()
        loadData()
        loadNotifications()
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                if (user == null) {
                    clearAssistantChat()
                }
            }
        }
        viewModelScope.launch {
            analyticsManager.summaryState.collect { summary ->
                _state.value = _state.value.copy(analyticsSummary = summary)
            }
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTelemetryLoading = true)
            val result = repository.getServerTelemetry()
            result.onSuccess { data ->
                _state.value = _state.value.copy(
                    telemetry = data,
                    isTelemetryLoading = false,
                    isConnected = true,
                    clusterName = if (data.atlas.cluster.isNotBlank()) "${data.atlas.cluster} (M0)" else _state.value.clusterName,
                    databaseName = if (data.atlas.database.isNotBlank()) data.atlas.database else _state.value.databaseName,
                    latencyMs = if (data.atlas.pingMs > 0) data.atlas.pingMs else _state.value.latencyMs
                )
            }.onFailure {
                _state.value = _state.value.copy(isTelemetryLoading = false)
            }
        }
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



    fun loadData(silent: Boolean = false) {
        checkConnection()
        refreshTelemetry()
        viewModelScope.launch {
            val monthKey = String.format(Locale.US, "%d-%02d", _state.value.currentYear, _state.value.currentMonth)
            val cachedRoster = rosterCache[monthKey]
            if (cachedRoster != null && _state.value.engineers.isNotEmpty()) {
                _state.value = _state.value.copy(
                    roster = cachedRoster,
                    isLoading = false
                )
            } else if (!silent) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            // 1. Load Engineers
            val engineersResult = repository.getEngineers()
            val engineers = engineersResult.getOrElse { emptyList() }

            // 2. Load Roster for current month/year
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
                rosterCache[monthKey] = rosterMap
            } else {
                rosterCache.remove(monthKey)
            }

            // 3. Load Operational Constraints
            val constraintsResult = repository.getConstraints()
            val loadedConstraints = constraintsResult.getOrDefault(com.rosterx.domain.model.OperationalConstraints())

            _state.value = _state.value.copy(
                isLoading = false,
                engineers = if (engineers.isNotEmpty()) engineers else _state.value.engineers,
                roster = if (rosterResult.isSuccess) rosterMap else emptyMap(),
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

        val monthKey = String.format(Locale.US, "%d-%02d", newYear, newMonth)

        _state.value = _state.value.copy(

            currentYear = newYear, 

            currentMonth = newMonth, 

            roster = rosterCache[monthKey] ?: emptyMap()

        )

        loadData()

    }



    fun nextMonth() {

        var newMonth = _state.value.currentMonth + 1

        var newYear = _state.value.currentYear

        if (newMonth > 12) {

            newMonth = 1

            newYear++

        }

        val monthKey = String.format(Locale.US, "%d-%02d", newYear, newMonth)

        _state.value = _state.value.copy(

            currentYear = newYear, 

            currentMonth = newMonth, 

            roster = rosterCache[monthKey] ?: emptyMap()

        )

        loadData()

    }



    fun syncEngineers(engineers: List<Engineer>, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {

        viewModelScope.launch {

            _state.value = _state.value.copy(engineers = engineers)

            val result = repository.syncEngineers(engineers)

            if (result.isSuccess) {

                loadData()

                onComplete(true, "Account directory updated successfully.")

            } else {

                val err = result.exceptionOrNull()?.message ?: "Failed to synchronize engineers"

                _state.value = _state.value.copy(errorMessage = err)

                onComplete(false, err)

            }

        }

    }



    fun deleteEngineer(engineer: Engineer, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {

        viewModelScope.launch {

            val res = repository.deleteEngineer(engineer.id, engineer.name, engineer.username)

            if (res.isSuccess) {

                val current = _state.value.engineers.filter { 
                    val matchesUser = engineer.username.isNotBlank() && it.username.equals(engineer.username, ignoreCase = true)
                    val matchesId = engineer.id.isNotBlank() && it.id.equals(engineer.id, ignoreCase = true)
                    val matchesName = (engineer.username.isBlank() && engineer.id.isBlank()) && it.name.equals(engineer.name, ignoreCase = true)
                    !(matchesUser || matchesId || matchesName)
                }
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
            loadBroadcastTemplates()
        }
    }

    fun loadBroadcastTemplates() {
        viewModelScope.launch {
            repository.getBroadcastTemplates().onSuccess { tpls ->
                _state.value = _state.value.copy(broadcastTemplates = tpls)
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
        sendPush: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingNotification = true)
            val user = sessionManager.currentUser.value
            val isManager = user?.role == com.rosterx.domain.model.EngineerRole.MANAGER
            val prefix = if (isManager) "Manager" else "Admin"
            val name = user?.name ?: user?.username ?: "Administrator"
            val senderSig = "$prefix ($name)"

            val result = repository.sendNotification(
                title = title,
                message = message,
                target = target,
                priority = priority,
                sendEmail = sendEmail,
                sendInApp = sendInApp,
                sendPush = sendPush,
                sender = senderSig
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

    fun retractNotification(
        notificationId: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = repository.retractNotification(notificationId)
            res.onSuccess {
                loadNotifications()
                onComplete(true, "Announcement retracted successfully")
            }.onFailure { err ->
                onComplete(false, err.message ?: "Failed to retract")
            }
        }
    }



    fun logout() {
        val fcmToken = sessionManager.getFcmToken()
        if (!fcmToken.isNullOrBlank()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    repository.unregisterFcmToken(fcmToken)
                } catch (_: Exception) {}
            }
        }
        _state.value = _state.value.copy(assistantMessages = emptyList(), isAssistantOpen = false)
        sessionManager.clearSession()
    }

    fun clearAssistantChat() {
        val welcomeMsg = listOf(
            com.rosterx.presentation.components.AssistantChatMessage(
                id = "init",
                role = "assistant",
                text = "### 🤖 RosterX Operations Assistant\n\nHello **Administrator**! I have real-time visibility into the NOC roster, shift schedules, and duty handovers.\n\nAsk me anything like:\n- *\"Who all are in shift now?\"*\n- *\"Who has duty tomorrow?\"*",
                timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            )
        )
        _state.value = _state.value.copy(assistantMessages = welcomeMsg)
    }


    fun openAssistant() {
        val msgs = if (_state.value.assistantMessages.isEmpty()) {
            listOf(
                com.rosterx.presentation.components.AssistantChatMessage(
                    id = "init",
                    role = "assistant",
                    text = "### 🤖 RosterX Operations Assistant\n\nHello Admin! I have full real-time visibility into the NOC roster, shift matrices, and Atlas telemetry.\n\nAsk me anything like:\n- *\"Who all are in shift now?\"*\n- *\"When is the next night shift?\"*\n- *\"Who is covering night duty tonight?\"*",
                    timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                )
            )
        } else _state.value.assistantMessages

        _state.value = _state.value.copy(isAssistantOpen = true, assistantMessages = msgs)
    }

    fun closeAssistant() {
        _state.value = _state.value.copy(isAssistantOpen = false)
    }

    fun sendAssistantMessage(prompt: String) {
        val q = prompt.trim()
        if (q.isBlank() || _state.value.isAssistantThinking) return

        val nowTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val userMsg = com.rosterx.presentation.components.AssistantChatMessage(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            text = q,
            timestamp = nowTime
        )
        _state.value = _state.value.copy(
            assistantMessages = _state.value.assistantMessages + userMsg,
            isAssistantThinking = true
        )

        viewModelScope.launch {
            val u = currentUser.value
            val userMap = u?.let {
                mapOf<String, Any?>(
                    "id" to it.id,
                    "name" to it.name,
                    "username" to it.username,
                    "role" to it.role.name.lowercase()
                )
            }
            val res = repository.queryAssistant(q, userMap)
            val botText = res.getOrNull()?.reply ?: res.exceptionOrNull()?.message ?: "Unable to complete assistant request."
            val botMsg = com.rosterx.presentation.components.AssistantChatMessage(
                id = "bot_${System.currentTimeMillis()}",
                role = "assistant",
                text = botText,
                timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            )
            _state.value = _state.value.copy(
                assistantMessages = _state.value.assistantMessages + botMsg,
                isAssistantThinking = false
            )
        }
    }

    fun uploadProfilePicture(base64Image: String, targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.updateProfilePicture(targetUsername, base64Image)
            if (res.isSuccess) {
                val newPic = res.getOrNull()
                val updatedEngineers = _state.value.engineers.map { eng ->
                    if (eng.username.equals(targetUsername ?: sessionManager.currentUser.value?.username, ignoreCase = true)) {
                        eng.copy(profilePicture = newPic)
                    } else {
                        eng
                    }
                }
                _state.value = _state.value.copy(engineers = updatedEngineers)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }

    fun removeProfilePicture(targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        uploadProfilePicture("", targetUsername, onResult)
    }

    fun updateProfileName(newName: String, targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.updateProfileName(newName, targetUsername)
            if (res.isSuccess) {
                val updatedName = res.getOrNull() ?: newName
                val updatedEngineers = _state.value.engineers.map { eng ->
                    if (eng.username.equals(targetUsername ?: sessionManager.currentUser.value?.username, ignoreCase = true)) {
                        eng.copy(name = updatedName)
                    } else {
                        eng
                    }
                }
                _state.value = _state.value.copy(engineers = updatedEngineers)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }
}
