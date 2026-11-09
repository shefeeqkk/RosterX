package com.rosterx.presentation.portal



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.rosterx.data.local.AppThemeMode
import com.rosterx.data.local.UserSessionManager

import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.ShiftCode
import com.rosterx.domain.model.isManagementAccount
import com.rosterx.domain.model.isEligibleForShiftRoster
import com.rosterx.domain.model.isEligibleForMonthRoster
import com.rosterx.presentation.components.toProperCase

import com.rosterx.domain.repository.RosterRepository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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

    val yesterdayRoster: Map<String, Map<String, ShiftCode>> = emptyMap(),

    

    // Grid Roster (Independently navigated in Month Roster tab)

    val gridRoster: Map<String, Map<String, ShiftCode>> = emptyMap(),

    val gridYear: Int = LocalDate.now().year,

    val gridMonth: Int = LocalDate.now().monthValue,

    

    val loggedInEngineer: Engineer? = null,

    val selectedDate: LocalDate = LocalDate.now(),

    val searchQuery: String = "",

    val errorMessage: String? = null,

    val successMessage: String? = null,

    val isConnected: Boolean = false,

    val isRefreshingHealth: Boolean = false,

    val latencyMs: Long = 0L,

    val clusterName: String = "Cluster0",

    val databaseName: String = "rosterx",

    val currentBaseUrl: String = "",

    val constraints: com.rosterx.domain.model.OperationalConstraints = com.rosterx.domain.model.OperationalConstraints(),

    val notifications: List<com.rosterx.data.remote.NotificationDto> = emptyList(),

    val dismissedNotificationIds: Set<String> = emptySet(),

    val isAssistantOpen: Boolean = false,

    val isAssistantThinking: Boolean = false,

    val assistantMessages: List<com.rosterx.presentation.components.AssistantChatMessage> = emptyList()

)



@HiltViewModel
class EngineerPortalViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager,
    val analyticsManager: com.rosterx.analytics.AnalyticsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {



    private val _state = MutableStateFlow(PortalState(currentBaseUrl = sessionManager.getBaseUrl()))
    private val rosterCache = mutableMapOf<String, Map<String, Map<String, ShiftCode>>>()


    val state: StateFlow<PortalState> = _state.asStateFlow()



    init {

        checkConnection()

        observeSession()

        loadData()

    }



    fun checkConnection() {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshingHealth = true)
            val startTime = System.currentTimeMillis()
            val diag = repository.getServerDiagnostics()
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 450) {
                kotlinx.coroutines.delay(450 - elapsed)
            }
            _state.value = _state.value.copy(
                isRefreshingHealth = false,
                isConnected = diag.isConnected,
                latencyMs = diag.latencyMs,
                clusterName = diag.cluster.ifBlank { "Cluster0" },
                databaseName = diag.database.ifBlank { "rosterx" },
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
                if (_state.value.loggedInEngineer?.id != user?.id) {
                    _state.value = _state.value.copy(
                        loggedInEngineer = user,
                        assistantMessages = emptyList(),
                        isAssistantOpen = false
                    )
                } else {
                    _state.value = _state.value.copy(loggedInEngineer = user)
                }
            }
        }
    }



    fun loadData(silent: Boolean = false) {
        viewModelScope.launch {
            val selected = _state.value.selectedDate
            val liveMonthKey = String.format(Locale.US, "%d-%02d", selected.year, selected.monthValue)
            val gridMonthKey = String.format(Locale.US, "%d-%02d", _state.value.gridYear, _state.value.gridMonth)

            val cachedLive = rosterCache[liveMonthKey]
            val cachedGrid = rosterCache[gridMonthKey]

            if (cachedLive != null && _state.value.engineers.isNotEmpty()) {
                _state.value = _state.value.copy(
                    liveRoster = cachedLive,
                    gridRoster = cachedGrid ?: cachedLive,
                    isLoading = false
                )
            } else if (!silent) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

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

            // 1.1 Refresh authoritative logged-in user profile from /api/auth/me
            val meResult = repository.getCurrentUser()
            var current = if (meResult.isSuccess) {
                val me = meResult.getOrNull()
                if (me != null) {
                    _state.value = _state.value.copy(loggedInEngineer = me)
                    me
                } else {
                    _state.value.loggedInEngineer
                }
            } else {
                _state.value.loggedInEngineer
            }

            // Merge details (like defaultWeeklyOffs) into loggedInEngineer
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
                        phone = if (!fullProfile.phone.contains("*") && fullProfile.phone.isNotBlank()) fullProfile.phone else current.phone,
                        joinDate = if (fullProfile.joinDate.isNotBlank()) fullProfile.joinDate else current.joinDate,
                        birthday = if (!fullProfile.birthday.isNullOrBlank()) fullProfile.birthday else current.birthday
                    )
                    sessionManager.setCurrentUser(mergedUser)
                    _state.value = _state.value.copy(loggedInEngineer = mergedUser)
                }
            }

            // 2. Load Live Operational Roster for selectedDate
            val liveRosterResult = repository.getRoster(liveMonthKey)
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
                rosterCache[liveMonthKey] = liveRosterMap
            }

            // 3. Load Grid Roster for Month Grid section
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
                rosterCache[gridMonthKey] = gridRosterMap
            }

            val notifs = repository.getNotifications().getOrElse { emptyList() }
            val savedDismissed = sessionManager.getDismissedNotifications()
            val loadedConstraints = repository.getConstraints().getOrDefault(com.rosterx.domain.model.OperationalConstraints())

            // Resolve yesterday's roster to handle overnight Night Shift (NS) rollovers (00:00 - 07:30 AM)
            val todayDate = LocalDate.now()
            val yesterdayDate = todayDate.minusDays(1)
            val yesterdayMonthKey = String.format(Locale.US, "%d-%02d", yesterdayDate.year, yesterdayDate.monthValue)
            val effectiveLive = if (liveRosterResult.isSuccess) liveRosterMap else (rosterCache[liveMonthKey] ?: emptyMap())
            val yesterdayRosterMap = if (yesterdayMonthKey == liveMonthKey) {
                effectiveLive
            } else {
                if (rosterCache.containsKey(yesterdayMonthKey)) {
                    rosterCache[yesterdayMonthKey] ?: emptyMap()
                } else {
                    val prevResult = repository.getRoster(yesterdayMonthKey)
                    if (prevResult.isSuccess) {
                        val raw = prevResult.getOrNull() ?: emptyMap()
                        val prevMap = mutableMapOf<String, Map<String, ShiftCode>>()
                        raw.forEach { (engId, daysMap) ->
                            val shiftMap = mutableMapOf<String, ShiftCode>()
                            daysMap.forEach { (day, shiftStr) ->
                                shiftMap[day] = ShiftCode.fromString(shiftStr)
                            }
                            prevMap[engId] = shiftMap
                        }
                        rosterCache[yesterdayMonthKey] = prevMap
                        prevMap
                    } else {
                        emptyMap()
                    }
                }
            }

            // Also fetch server diagnostics to keep cluster, database, and latency current
            val diag = repository.getServerDiagnostics()

            _state.value = _state.value.copy(
                isLoading = false,
                engineers = engineers,
                liveRoster = effectiveLive,
                yesterdayRoster = yesterdayRosterMap,
                gridRoster = if (gridRosterResult.isSuccess) gridRosterMap else (rosterCache[gridMonthKey] ?: emptyMap()),
                constraints = loadedConstraints,
                notifications = notifs,
                dismissedNotificationIds = savedDismissed,
                isConnected = engineersResult.isSuccess && diag.isConnected,
                clusterName = diag.cluster.ifBlank { "Cluster0" },
                databaseName = diag.database.ifBlank { "rosterx" },
                latencyMs = diag.latencyMs
            )

            // Auto-sync Home Screen App Widget with latest duty assignments
            val currentEng = _state.value.loggedInEngineer
            if (currentEng != null) {
                if (currentEng.role == com.rosterx.domain.model.EngineerRole.MANAGER) {
                    val today = LocalDate.now()
                    val todayDayStr = today.dayOfMonth.toString()
                    val yesterday = today.minusDays(1)
                    val yesterdayDayStr = yesterday.dayOfMonth.toString()
                    val activeEngineers = engineers.filter { it.isEligibleForShiftRoster(today) }
                    val nowTime = java.time.LocalTime.now()
                    val nowMin = nowTime.hour * 60 + nowTime.minute
                    val isPostMidnightMorning = nowMin < 450 // 00:00 to 07:30 AM

                    val onDutyItems = mutableListOf<com.rosterx.widget.WidgetDutyItem>()
                    val restItems = mutableListOf<com.rosterx.widget.WidgetDutyItem>()
                    val overnightActiveEngNames = mutableSetOf<String>()

                    // 1. Overnight Shift Rollover: If between 00:00 and 07:30 AM, yesterday's NS engineer is actively on duty!
                    if (isPostMidnightMorning) {
                        activeEngineers.forEach { eng ->
                            val yShift = yesterdayRosterMap[eng.username]?.get(yesterdayDayStr)
                                ?: yesterdayRosterMap[eng.id]?.get(yesterdayDayStr)
                                ?: yesterdayRosterMap[eng.name]?.get(yesterdayDayStr)
                                ?: yesterdayRosterMap[eng.name.uppercase()]?.get(yesterdayDayStr)
                                ?: yesterdayRosterMap[eng.name.lowercase()]?.get(yesterdayDayStr)
                                ?: effectiveLive[eng.username]?.get(yesterdayDayStr)
                                ?: effectiveLive[eng.name]?.get(yesterdayDayStr)
                                ?: com.rosterx.domain.model.ShiftCode.OFF
                            val yCode = yShift.code.uppercase()
                            if (yCode == "NS") {
                                overnightActiveEngNames.add(eng.name.lowercase())
                                overnightActiveEngNames.add(eng.id.lowercase())
                                overnightActiveEngNames.add(eng.username.lowercase())
                                onDutyItems.add(
                                    com.rosterx.widget.WidgetDutyItem(
                                        engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                        shiftCode = "NS",
                                        shiftName = "Night Shift",
                                        timing = "09:00 PM – 07:30 AM (+1)",
                                        bgHex = yShift.bgColorHex.ifBlank { "#020617" },
                                        textHex = yShift.textColorHex.ifBlank { "#FFFFFF" },
                                        isActiveNow = true,
                                        itemType = com.rosterx.widget.WidgetDutyItem.TYPE_DUTY
                                    )
                                )
                            }
                        }
                    }

                    // 2. Add today's scheduled engineers
                    activeEngineers.forEach { eng ->
                        val isOvernightActive = eng.name.lowercase() in overnightActiveEngNames ||
                                eng.id.lowercase() in overnightActiveEngNames ||
                                eng.username.lowercase() in overnightActiveEngNames

                        val shift = effectiveLive[eng.username]?.get(todayDayStr)
                            ?: effectiveLive[eng.id]?.get(todayDayStr)
                            ?: effectiveLive[eng.name]?.get(todayDayStr)
                            ?: effectiveLive[eng.name.uppercase()]?.get(todayDayStr)
                            ?: effectiveLive[eng.name.lowercase()]?.get(todayDayStr)
                            ?: com.rosterx.domain.model.ShiftCode.OFF
                        val code = shift.code.uppercase()

                        val isRest = code in setOf("OFF", "L") || code.startsWith("SW")
                        if (!isRest) {
                            val meta = com.rosterx.widget.RosterXAppWidgetProvider.resolveMeta(code)
                            val isActive = when (code) {
                                "FS" -> nowMin in 450..990
                                "FG" -> nowMin in 510..1050
                                "G"  -> nowMin in 570..1110
                                "SG" -> nowMin in 660..1200
                                "SS" -> nowMin in 780..1320
                                "NS" -> nowMin >= 1260 // Today's NS is only active after 21:00 PM tonight!
                                else -> false
                            }

                            // Avoid duplicate if already added as active overnight NS engineer
                            val alreadyAdded = onDutyItems.any {
                                it.engineerName.equals(eng.name, ignoreCase = true) ||
                                (eng.fullName.isNotBlank() && it.engineerName.equals(eng.fullName, ignoreCase = true))
                            }
                            if (!alreadyAdded) {
                                onDutyItems.add(
                                    com.rosterx.widget.WidgetDutyItem(
                                        engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                        shiftCode = code,
                                        shiftName = meta.name,
                                        timing = meta.timing,
                                        bgHex = shift.bgColorHex,
                                        textHex = shift.textColorHex,
                                        isActiveNow = isActive,
                                        itemType = com.rosterx.widget.WidgetDutyItem.TYPE_DUTY
                                    )
                                )
                            }
                        } else {
                            // If they are actively on duty overnight, do not add to restItems!
                            if (!isOvernightActive) {
                                val restDesc = when {
                                    code.startsWith("SW") -> "Shift Swap Off"
                                    code == "L" -> "Approved Leave"
                                    else -> "Scheduled Rest"
                                }
                                restItems.add(
                                    com.rosterx.widget.WidgetDutyItem(
                                        engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                        shiftCode = code,
                                        shiftName = restDesc,
                                        timing = "Rest Day",
                                        bgHex = shift.bgColorHex.ifBlank { "#FEF3C7" },
                                        textHex = shift.textColorHex.ifBlank { "#92400E" },
                                        isActiveNow = false,
                                        itemType = com.rosterx.widget.WidgetDutyItem.TYPE_REST
                                    )
                                )
                            }
                        }
                    }

                    // Sort On Duty: Active overnight NS engineer first (-180 min), then by shift start time
                    fun shiftStartMin(item: com.rosterx.widget.WidgetDutyItem): Int {
                        if (item.isActiveNow && item.shiftCode == "NS" && isPostMidnightMorning) return -180
                        return when (item.shiftCode) {
                            "FS" -> 450
                            "FG" -> 510
                            "G"  -> 570
                            "SG" -> 660
                            "SS" -> 780
                            "NS" -> 1260
                            else -> 9999
                        }
                    }
                    onDutyItems.sortBy { shiftStartMin(it) }

                    val finalWidgetList = mutableListOf<com.rosterx.widget.WidgetDutyItem>()
                    if (onDutyItems.isNotEmpty()) {
                        finalWidgetList.add(
                            com.rosterx.widget.WidgetDutyItem(
                                itemType = com.rosterx.widget.WidgetDutyItem.TYPE_HEADER,
                                sectionTitle = "⚡ ON DUTY",
                                sectionCount = if (onDutyItems.size == 1) "1 Engineer" else "${onDutyItems.size} Engineers"
                            )
                        )
                        finalWidgetList.addAll(onDutyItems)
                    }

                    if (restItems.isNotEmpty()) {
                        finalWidgetList.add(
                            com.rosterx.widget.WidgetDutyItem(
                                itemType = com.rosterx.widget.WidgetDutyItem.TYPE_HEADER,
                                sectionTitle = "☕ REST & LEAVE",
                                sectionCount = if (restItems.size == 1) "1 Engineer" else "${restItems.size} Engineers"
                            )
                        )
                        finalWidgetList.addAll(restItems)
                    }

                    val dateFormatted = today.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd"))
                    val summaryStr = "Total on Duty: ${onDutyItems.size} Engineers | Rest: ${restItems.size} Engineers"

                    com.rosterx.widget.RosterXAppWidgetProvider.saveManagerDutyRoster(
                        context = context,
                        managerName = currentEng.name,
                        dateFormatted = dateFormatted,
                        dutyList = finalWidgetList,
                        summaryText = summaryStr
                    )
                } else {
                    val engShifts = effectiveLive[currentEng.username]
                        ?: effectiveLive[currentEng.id]
                        ?: effectiveLive[currentEng.name]
                        ?: effectiveLive[currentEng.name.uppercase()]
                        ?: effectiveLive[currentEng.name.lowercase()]
                        ?: emptyMap()
                    if (engShifts.isNotEmpty()) {
                        val stringMap = engShifts.mapValues { it.value.code }
                        com.rosterx.widget.RosterXAppWidgetProvider.saveWidgetData(
                            context,
                            currentEng.name,
                            stringMap,
                            liveMonthKey
                        )
                    }
                }
            }

            if (engineersResult.isFailure) {
                _state.value = _state.value.copy(
                    errorMessage = engineersResult.exceptionOrNull()?.message ?: "Failed to load data. Please check connection."
                )
            }
        }
    }

    fun resetToCurrentDateAndMonth() {
        val today = LocalDate.now()
        val currentKey = String.format(Locale.US, "%d-%02d", today.year, today.monthValue)
        val cached = rosterCache[currentKey]
        _state.value = _state.value.copy(
            selectedDate = today,
            gridYear = today.year,
            gridMonth = today.monthValue,
            liveRoster = cached ?: _state.value.liveRoster,
            gridRoster = cached ?: _state.value.gridRoster
        )
        loadData(silent = cached != null && _state.value.engineers.isNotEmpty())
    }

    fun selectDate(date: LocalDate) {
        val oldKey = String.format(Locale.US, "%d-%02d", _state.value.selectedDate.year, _state.value.selectedDate.monthValue)
        val newKey = String.format(Locale.US, "%d-%02d", date.year, date.monthValue)
        _state.value = _state.value.copy(selectedDate = date)
        analyticsManager.logEvent("team_shifts_date_selected", mapOf("date" to date.toString()))
        if (oldKey != newKey || _state.value.liveRoster.isEmpty()) {
            loadLiveRosterOnly()
        }
    }

    private fun loadLiveRosterOnly() {
        viewModelScope.launch {
            val selected = _state.value.selectedDate
            val liveMonthKey = String.format(Locale.US, "%d-%02d", selected.year, selected.monthValue)
            val cached = rosterCache[liveMonthKey]
            if (cached != null) {
                _state.value = _state.value.copy(liveRoster = cached)
            }

            val liveRosterResult = repository.getRoster(liveMonthKey)
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
                rosterCache[liveMonthKey] = liveRosterMap
                _state.value = _state.value.copy(liveRoster = liveRosterMap)
            }
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
        val gridMonthKey = String.format(Locale.US, "%d-%02d", newYear, newMonth)
        _state.value = _state.value.copy(
            gridYear = newYear, 
            gridMonth = newMonth,
            gridRoster = rosterCache[gridMonthKey] ?: emptyMap()
        )
        analyticsManager.logEvent("month_roster_changed", mapOf("month_key" to gridMonthKey, "direction" to "prev"))
        loadGridRosterOnly()
    }

    fun nextMonth() {
        var newMonth = _state.value.gridMonth + 1
        var newYear = _state.value.gridYear
        if (newMonth > 12) {
            newMonth = 1
            newYear++
        }
        val gridMonthKey = String.format(Locale.US, "%d-%02d", newYear, newMonth)
        _state.value = _state.value.copy(
            gridYear = newYear, 
            gridMonth = newMonth,
            gridRoster = rosterCache[gridMonthKey] ?: emptyMap()
        )
        analyticsManager.logEvent("month_roster_changed", mapOf("month_key" to gridMonthKey, "direction" to "next"))
        loadGridRosterOnly()
    }

    private fun loadGridRosterOnly() {
        viewModelScope.launch {
            val gridMonthKey = String.format(Locale.US, "%d-%02d", _state.value.gridYear, _state.value.gridMonth)
            val cached = rosterCache[gridMonthKey]
            if (cached != null) {
                _state.value = _state.value.copy(gridRoster = cached, isGridLoading = false)
            } else {
                _state.value = _state.value.copy(gridRoster = emptyMap(), isGridLoading = true)
            }

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
                rosterCache[gridMonthKey] = gridRosterMap
                _state.value = _state.value.copy(
                    isGridLoading = false,
                    gridRoster = gridRosterMap
                )
            } else {
                rosterCache.remove(gridMonthKey)
                _state.value = _state.value.copy(
                    isGridLoading = false,
                    gridRoster = emptyMap()
                )
            }
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
        analyticsManager.logEvent("notification_dismissed", mapOf("notification_id" to id))
        sessionManager.dismissNotification(id)
        _state.value = _state.value.copy(
            dismissedNotificationIds = _state.value.dismissedNotificationIds + id
        )
        viewModelScope.launch {
            repository.dismissNotification(id)
        }
    }

    fun clearAllNotifications() {
        analyticsManager.logEvent("notifications_cleared_all")
        val ids = _state.value.notifications.mapIndexed { idx, n -> n.id ?: "notif_$idx" }
        sessionManager.clearAllNotifications(ids)
        _state.value = _state.value.copy(
            dismissedNotificationIds = _state.value.dismissedNotificationIds + ids
        )
        viewModelScope.launch {
            repository.dismissNotification("", dismissAll = true)
        }
    }

    fun logout() {
        _state.value = _state.value.copy(assistantMessages = emptyList(), isAssistantOpen = false)
        sessionManager.clearSession()
    }

    fun clearAssistantChat() {
        val engName = (_state.value.loggedInEngineer?.name ?: "Engineer").toProperCase()
        val welcomeMsg = listOf(
            com.rosterx.presentation.components.AssistantChatMessage(
                id = "init",
                role = "assistant",
                text = "### 🤖 RosterX Operations Assistant\n\nHello **$engName**! I have real-time visibility into the NOC roster, shift schedules, and duty handovers.\n\nAsk me anything like:\n- *\"Who all are in shift now?\"*\n- *\"When is my next night shift?\"*\n- *\"What is my shift today?\"*",
                timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            )
        )
        _state.value = _state.value.copy(assistantMessages = welcomeMsg)
    }

    fun openAssistant() {
        val msgs = if (_state.value.assistantMessages.isEmpty()) {
            val engName = (_state.value.loggedInEngineer?.name ?: "Engineer").toProperCase()
            listOf(
                com.rosterx.presentation.components.AssistantChatMessage(
                    id = "init",
                    role = "assistant",
                    text = "### 🤖 RosterX Operations Assistant\n\nHello **$engName**! I have real-time visibility into the NOC roster, shift schedules, and duty handovers.\n\nAsk me anything like:\n- *\"Who all are in shift now?\"*\n- *\"When is my next night shift?\"*\n- *\"What is my shift today?\"*",
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
            val eng = _state.value.loggedInEngineer
            val userMap = eng?.let {
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

    fun setThemeMode(mode: AppThemeMode) {
        analyticsManager.logEvent("theme_mode_changed", mapOf("mode" to mode.name))
        sessionManager.setThemeMode(mode)
    }

    fun setThemePaletteId(paletteId: String) {
        analyticsManager.logEvent("theme_palette_changed", mapOf("palette" to paletteId))
        sessionManager.setThemePaletteId(paletteId)
    }

    fun updateUserProfile(
        name: String,
        phone: String,
        email: String,
        joinDate: String,
        birthday: String,
        profilePicture: String?,
        currentPassword: String?,
        newPassword: String?,
        targetUsername: String? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val res = repository.updateUserProfile(
                name = name.takeIf { it.isNotBlank() },
                phone = phone,
                email = email,
                joinDate = joinDate,
                birthday = birthday,
                profilePicture = profilePicture,
                currentPassword = currentPassword?.takeIf { it.isNotBlank() },
                newPassword = newPassword?.takeIf { it.isNotBlank() },
                username = targetUsername
            )
            if (res.isSuccess) {
                val updatedEng = res.getOrNull()
                val loggedIn = _state.value.loggedInEngineer
                if (updatedEng != null && loggedIn != null && (targetUsername.isNullOrBlank() || targetUsername.equals(loggedIn.username, ignoreCase = true))) {
                    _state.value = _state.value.copy(loggedInEngineer = updatedEng)
                }
                if (updatedEng != null) {
                    val updatedEngineers = _state.value.engineers.map { eng ->
                        if (eng.username.equals(updatedEng.username, ignoreCase = true)) updatedEng else eng
                    }
                    _state.value = _state.value.copy(engineers = updatedEngineers, isLoading = false, successMessage = "Profile updated successfully")
                } else {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Profile updated successfully")
                }
                analyticsManager.logEvent("profile_updated", mapOf(
                    "has_photo_change" to (profilePicture != null),
                    "has_password_change" to (!newPassword.isNullOrBlank()),
                    "target_user" to (targetUsername ?: loggedIn?.username ?: "")
                ))
                onResult?.invoke(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to update profile"
                _state.value = _state.value.copy(isLoading = false, errorMessage = err)
                onResult?.invoke(false, err)
            }
        }
    }

    fun uploadProfilePicture(base64Image: String, targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val res = repository.updateProfilePicture(targetUsername, base64Image)
            if (res.isSuccess) {
                val newPic = res.getOrNull()
                val loggedIn = _state.value.loggedInEngineer
                if (loggedIn != null && (targetUsername.isNullOrBlank() || targetUsername.equals(loggedIn.username, ignoreCase = true))) {
                    val updated = loggedIn.copy(profilePicture = newPic)
                    _state.value = _state.value.copy(loggedInEngineer = updated)
                }
                val updatedEngineers = _state.value.engineers.map { eng ->
                    if (targetUsername.isNullOrBlank() && eng.username.equals(loggedIn?.username, ignoreCase = true)) {
                        eng.copy(profilePicture = newPic)
                    } else if (eng.username.equals(targetUsername, ignoreCase = true)) {
                        eng.copy(profilePicture = newPic)
                    } else {
                        eng
                    }
                }
                _state.value = _state.value.copy(engineers = updatedEngineers, isLoading = false)
                onResult?.invoke(true, null)
            } else {
                _state.value = _state.value.copy(isLoading = false)
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }

    fun removeProfilePicture(targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        uploadProfilePicture("", targetUsername, onResult)
    }

    fun updateProfileName(newName: String, targetUsername: String? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val res = repository.updateProfileName(newName, targetUsername)
            if (res.isSuccess) {
                val updatedName = res.getOrNull() ?: newName
                val loggedIn = _state.value.loggedInEngineer
                if (loggedIn != null && (targetUsername.isNullOrBlank() || targetUsername.equals(loggedIn.username, ignoreCase = true))) {
                    val updated = loggedIn.copy(name = updatedName)
                    _state.value = _state.value.copy(loggedInEngineer = updated)
                }
                val updatedEngineers = _state.value.engineers.map { eng ->
                    if (targetUsername.isNullOrBlank() && eng.username.equals(loggedIn?.username, ignoreCase = true)) {
                        eng.copy(name = updatedName)
                    } else if (eng.username.equals(targetUsername, ignoreCase = true)) {
                        eng.copy(name = updatedName)
                    } else {
                        eng
                    }
                }
                _state.value = _state.value.copy(engineers = updatedEngineers, isLoading = false)
                onResult?.invoke(true, null)
            } else {
                _state.value = _state.value.copy(isLoading = false)
                val err = res.exceptionOrNull()?.message ?: "Failed to update profile name"
                onResult?.invoke(false, err)
            }
        }
    }
}

