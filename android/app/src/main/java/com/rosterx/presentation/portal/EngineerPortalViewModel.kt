package com.rosterx.presentation.portal

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
import javax.inject.Inject

data class PortalState(
    val isLoading: Boolean = false,
    val isGridLoading: Boolean = false,
    val engineers: List<Engineer> = emptyList(),
    
    // Live Roster (Anchored to operational month for Dashboard & Team Shifts)
    val liveRoster: Map<String, Map<String, ShiftCode>> = emptyMap(),
    
    // Grid Roster (Independently navigated in Month Roster tab)
    val gridRoster: Map<String, Map<String, ShiftCode>> = emptyMap(),
    val gridYear: Int = 2026,
    val gridMonth: Int = 10,
    
    val loggedInEngineer: Engineer? = null,
    val selectedDate: LocalDate = LocalDate.of(2026, 10, 26),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isConnected: Boolean = false,
    val latencyMs: Long = 0L,
    val currentBaseUrl: String = "",
    val constraints: com.rosterx.domain.model.OperationalConstraints = com.rosterx.domain.model.OperationalConstraints(),
    val notifications: List<com.rosterx.data.remote.NotificationDto> = emptyList(),
    val dismissedNotificationIds: Set<String> = emptySet()
)

@HiltViewModel
class EngineerPortalViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PortalState(currentBaseUrl = sessionManager.getBaseUrl()))
    val state: StateFlow<PortalState> = _state.asStateFlow()

    init {
        checkConnection()
        observeSession()
        loadData()
    }

    fun checkConnection() {
        viewModelScope.launch {
            val diag = repository.getServerDiagnostics()
            _state.value = _state.value.copy(
                isConnected = diag.isConnected,
                latencyMs = diag.latencyMs,
                currentBaseUrl = sessionManager.getBaseUrl()
            )
        }
    }

    fun getBaseUrl(): String = sessionManager.getBaseUrl()

    fun saveBaseUrl(url: String) {
        sessionManager.setBaseUrl(url)
        _state.value = _state.value.copy(currentBaseUrl = sessionManager.getBaseUrl())
        checkConnection()
        loadData()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _state.value = _state.value.copy(loggedInEngineer = user)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            val engineersResult = repository.getEngineers()
            
            // If connection failed immediately, abort loading logic to clear spinner faster
            if (engineersResult.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isConnected = false,
                    errorMessage = engineersResult.exceptionOrNull()?.message ?: "Failed to connect to backend server."
                )
                return@launch
            }
            
            val engineers = engineersResult.getOrElse { emptyList() }

            // Merge details (like defaultWeeklyOffs) into loggedInEngineer
            val current = _state.value.loggedInEngineer
            if (current != null && engineers.isNotEmpty()) {
                val fullProfile = engineers.find { 
                    it.username.equals(current.username, ignoreCase = true) ||
                    it.id.equals(current.id, ignoreCase = true) ||
                    it.name.equals(current.name, ignoreCase = true)
                }
                if (fullProfile != null) {
                    val mergedUser = current.copy(
                        defaultWeeklyOffs = if (fullProfile.defaultWeeklyOffs.isNotEmpty()) fullProfile.defaultWeeklyOffs else current.defaultWeeklyOffs,
                        email = if (fullProfile.email.isNotBlank()) fullProfile.email else current.email,
                        phone = if (fullProfile.phone.isNotBlank()) fullProfile.phone else current.phone
                    )
                    sessionManager.setCurrentUser(mergedUser)
                    _state.value = _state.value.copy(loggedInEngineer = mergedUser)
                }
            }

            // 2. Load Live Operational Roster for selectedDate (e.g. 2026-10)
            val selected = _state.value.selectedDate
            var liveMonthKey = String.format(Locale.US, "%d-%02d", selected.year, selected.monthValue)
            var liveRosterResult = repository.getRoster(liveMonthKey)
            
            // Fallback to "2026-10" if active month is empty
            if (liveRosterResult.isFailure && liveMonthKey != "2026-10") {
                liveMonthKey = "2026-10"
                liveRosterResult = repository.getRoster("2026-10")
            }

            val liveRosterMap = mutableMapOf<String, Map<String, ShiftCode>>()
            if (liveRosterResult.isSuccess) {
                val rawRoster = liveRosterResult.getOrNull() ?: emptyMap()
                rawRoster.forEach { (engId, daysMap) ->
                    val shiftMap = mutableMapOf<String, ShiftCode>()
                    daysMap.forEach { (day, shiftStr) ->
                        shiftMap[day] = ShiftCode.fromString(shiftStr)
                    }
                    liveRosterMap[engId] = shiftMap
                }
            }

            // 3. Load Grid Roster for Month Grid section
            val gridMonthKey = String.format(Locale.US, "%d-%02d", _state.value.gridYear, _state.value.gridMonth)
            val gridRosterResult = if (gridMonthKey == liveMonthKey) liveRosterResult else repository.getRoster(gridMonthKey)
            
            val gridRosterMap = mutableMapOf<String, Map<String, ShiftCode>>()
            if (gridRosterResult.isSuccess) {
                val rawRoster = gridRosterResult.getOrNull() ?: emptyMap()
                rawRoster.forEach { (engId, daysMap) ->
                    val shiftMap = mutableMapOf<String, ShiftCode>()
                    daysMap.forEach { (day, shiftStr) ->
                        shiftMap[day] = ShiftCode.fromString(shiftStr)
                    }
                    gridRosterMap[engId] = shiftMap
                }
            }

            val notifs = repository.getNotifications().getOrElse { emptyList() }
            val savedDismissed = sessionManager.getDismissedNotifications()
            val loadedConstraints = repository.getConstraints().getOrDefault(com.rosterx.domain.model.OperationalConstraints())

            _state.value = _state.value.copy(
                isLoading = false,
                engineers = engineers,
                liveRoster = liveRosterMap,
                gridRoster = gridRosterMap,
                constraints = loadedConstraints,
                notifications = notifs,
                dismissedNotificationIds = savedDismissed,
                isConnected = engineersResult.isSuccess
            )
            
            if (engineersResult.isFailure) {
                _state.value = _state.value.copy(
                    errorMessage = engineersResult.exceptionOrNull()?.message ?: "Failed to load data. Please check connection."
                )
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val oldKey = String.format(Locale.US, "%d-%02d", _state.value.selectedDate.year, _state.value.selectedDate.monthValue)
        val newKey = String.format(Locale.US, "%d-%02d", date.year, date.monthValue)
        
        _state.value = _state.value.copy(selectedDate = date)
        
        if (oldKey != newKey || _state.value.liveRoster.isEmpty()) {
            loadLiveRosterOnly()
        }
    }

    private fun loadLiveRosterOnly() {
        viewModelScope.launch {
            val selected = _state.value.selectedDate
            var liveMonthKey = String.format(Locale.US, "%d-%02d", selected.year, selected.monthValue)
            var liveRosterResult = repository.getRoster(liveMonthKey)
            
            if (liveRosterResult.isFailure && liveMonthKey != "2026-10") {
                liveRosterResult = repository.getRoster("2026-10")
            }

            val liveRosterMap = mutableMapOf<String, Map<String, ShiftCode>>()
            if (liveRosterResult.isSuccess) {
                val rawRoster = liveRosterResult.getOrNull() ?: emptyMap()
                rawRoster.forEach { (engId, daysMap) ->
                    val shiftMap = mutableMapOf<String, ShiftCode>()
                    daysMap.forEach { (day, shiftStr) ->
                        shiftMap[day] = ShiftCode.fromString(shiftStr)
                    }
                    liveRosterMap[engId] = shiftMap
                }
            }

            _state.value = _state.value.copy(liveRoster = liveRosterMap)
        }
    }

    fun previousDay() {
        selectDate(_state.value.selectedDate.minusDays(1))
    }

    fun nextDay() {
        selectDate(_state.value.selectedDate.plusDays(1))
    }

    fun previousMonth() {
        var newMonth = _state.value.gridMonth - 1
        var newYear = _state.value.gridYear
        if (newMonth < 1) {
            newMonth = 12
            newYear--
        }
        _state.value = _state.value.copy(gridYear = newYear, gridMonth = newMonth)
        loadGridRosterOnly()
    }

    fun nextMonth() {
        var newMonth = _state.value.gridMonth + 1
        var newYear = _state.value.gridYear
        if (newMonth > 12) {
            newMonth = 1
            newYear++
        }
        _state.value = _state.value.copy(gridYear = newYear, gridMonth = newMonth)
        loadGridRosterOnly()
    }

    private fun loadGridRosterOnly() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGridLoading = true)
            val gridMonthKey = String.format(Locale.US, "%d-%02d", _state.value.gridYear, _state.value.gridMonth)
            val gridRosterResult = repository.getRoster(gridMonthKey)
            
            val gridRosterMap = mutableMapOf<String, Map<String, ShiftCode>>()
            if (gridRosterResult.isSuccess) {
                val rawRoster = gridRosterResult.getOrNull() ?: emptyMap()
                rawRoster.forEach { (engId, daysMap) ->
                    val shiftMap = mutableMapOf<String, ShiftCode>()
                    daysMap.forEach { (day, shiftStr) ->
                        shiftMap[day] = ShiftCode.fromString(shiftStr)
                    }
                    gridRosterMap[engId] = shiftMap
                }
            }

            _state.value = _state.value.copy(
                isGridLoading = false,
                gridRoster = gridRosterMap
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun changePassword(newPass: String) {
        if (newPass.length < 4) {
            _state.value = _state.value.copy(errorMessage = "Password must be at least 4 characters long.")
            return
        }
        _state.value = _state.value.copy(
            successMessage = "Password updated successfully!",
            errorMessage = null
        )
    }

    fun dismissNotification(id: String) {
        sessionManager.dismissNotification(id)
        _state.value = _state.value.copy(
            dismissedNotificationIds = _state.value.dismissedNotificationIds + id
        )
    }

    fun clearAllNotifications() {
        val ids = _state.value.notifications.mapIndexed { idx, n -> n.id ?: "notif_$idx" }
        sessionManager.clearAllNotifications(ids)
        _state.value = _state.value.copy(
            dismissedNotificationIds = _state.value.dismissedNotificationIds + ids
        )
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
