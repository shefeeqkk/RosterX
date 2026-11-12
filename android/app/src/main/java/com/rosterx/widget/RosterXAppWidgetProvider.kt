package com.rosterx.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rosterx.MainActivity
import com.rosterx.R
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.isEligibleForShiftRoster
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetDutyItem(
    val engineerName: String = "",
    val shiftCode: String = "",
    val shiftName: String = "",
    val timing: String = "",
    val bgHex: String = "#334155",
    val textHex: String = "#FFFFFF",
    val isActiveNow: Boolean = false,
    val itemType: Int = TYPE_DUTY,
    val sectionTitle: String = "",
    val sectionCount: String = ""
) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_DUTY = 1
        const val TYPE_REST = 2
    }
}

class RosterXAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.rosterx.ACTION_UPDATE_WIDGET"
        private const val TAG = "RosterXWidget"
        private const val PREFS_NAME = "rosterx_widget_cache"
        private const val KEY_ENGINEER_NAME = "key_engineer_name"
        private const val KEY_SHIFTS_JSON = "key_shifts_json"
        private const val KEY_MONTH_KEY = "key_month_key"
        private const val KEY_IS_MANAGER = "key_is_manager"
        private const val KEY_IS_LOGGED_OUT = "key_is_logged_out"
        private const val KEY_MANAGER_ACTIVE_CODE = "key_manager_active_code"
        private const val KEY_MANAGER_ACTIVE_NAME = "key_manager_active_name"
        private const val KEY_MANAGER_ACTIVE_TIMING = "key_manager_active_timing"
        private const val KEY_MANAGER_ACTIVE_STAFF = "key_manager_active_staff"
        private const val KEY_MANAGER_NEXT_CODE = "key_manager_next_code"
        private const val KEY_MANAGER_NEXT_NAME = "key_manager_next_name"
        private const val KEY_MANAGER_NEXT_TIMING = "key_manager_next_timing"
        private const val KEY_MANAGER_NEXT_STAFF = "key_manager_next_staff"
        private const val KEY_MANAGER_COVERAGE_TEXT = "key_manager_coverage_text"
        private const val KEY_MANAGER_REST_TEXT = "key_manager_rest_text"
        const val KEY_MANAGER_DUTY_LIST_JSON = "key_manager_duty_list_json"
        const val KEY_MANAGER_DATE_STR = "key_manager_date_str"
        const val KEY_MANAGER_SUMMARY_STR = "key_manager_summary_str"
        const val KEY_FULL_ROSTER_JSON = "key_full_roster_json"
        const val KEY_YESTERDAY_ROSTER_JSON = "key_yesterday_roster_json"
        const val KEY_ENGINEERS_JSON = "key_engineers_json"
        const val KEY_ROSTER_MONTH_KEY = "key_roster_month_key"

        data class ShiftMeta(val name: String, val timing: String, val bg: String, val text: String, val startSec: Long, val endSec: Long)
        private val shiftRegistry = mapOf(
            "FS"  to ShiftMeta("First Shift", "07:30 AM – 04:30 PM", "#BBF7D0", "#064E3B", 27000L, 59400L),
            "FG"  to ShiftMeta("First General", "08:30 AM – 05:30 PM", "#166534", "#FFFFFF", 30600L, 63000L),
            "G"   to ShiftMeta("General Shift", "09:30 AM – 06:30 PM", "#BAE6FD", "#0C4A6E", 34200L, 66600L),
            "SG"  to ShiftMeta("Second General", "11:00 AM – 08:00 PM", "#EA580C", "#FFFFFF", 39600L, 72000L),
            "SS"  to ShiftMeta("Second Shift", "01:00 PM – 10:00 PM", "#FED7AA", "#7C2D12", 46800L, 79200L),
            "NS"  to ShiftMeta("Night Shift", "09:00 PM – 07:30 AM (+1)", "#020617", "#FFFFFF", 75600L, 113400L),
            "OFF" to ShiftMeta("Weekly Off", "Full 24h Rest", "#FEF08A", "#854D0E", 0L, 0L),
            "L"   to ShiftMeta("Approved Leave", "Leave Day", "#DC2626", "#FFFFFF", 0L, 0L)
        )

        fun resolveMeta(code: String): ShiftMeta {
            val upper = code.trim().uppercase()
            if (shiftRegistry.containsKey(upper)) return shiftRegistry[upper]!!
            if (upper.startsWith("SW")) return ShiftMeta("Swap Shift", "Covered Duty", "#FEF08A", "#854D0E", 0L, 0L)
            return ShiftMeta(code, "Assigned Duty", "#E2E8F0", "#334155", 0L, 0L)
        }

        private val gson = Gson()

        fun saveWidgetData(
            context: Context,
            engineerName: String,
            shiftsMap: Map<String, String>,
            monthKey: String,
            keepManagerMode: Boolean = false
        ) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putString(KEY_ENGINEER_NAME, engineerName)
                    .putString(KEY_SHIFTS_JSON, gson.toJson(shiftsMap))
                    .putString(KEY_MONTH_KEY, monthKey)
                    .putBoolean(KEY_IS_LOGGED_OUT, false)

                if (!keepManagerMode) {
                    editor.putBoolean(KEY_IS_MANAGER, false)
                    editor.remove(KEY_MANAGER_DUTY_LIST_JSON)
                }
                editor.apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to saveWidgetData", e)
            }
        }

        fun clearWidgetData(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_OUT, true)
                    .putBoolean(KEY_IS_MANAGER, false)
                    .putString(KEY_ENGINEER_NAME, "Signed Out")
                    .remove(KEY_SHIFTS_JSON)
                    .remove(KEY_MONTH_KEY)
                    .remove(KEY_MANAGER_DUTY_LIST_JSON)
                    .remove(KEY_MANAGER_DATE_STR)
                    .remove(KEY_MANAGER_SUMMARY_STR)
                    .remove(KEY_FULL_ROSTER_JSON)
                    .remove(KEY_YESTERDAY_ROSTER_JSON)
                    .remove(KEY_ENGINEERS_JSON)
                    .remove(KEY_ROSTER_MONTH_KEY)
                    .apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to clearWidgetData", e)
            }
        }

        fun onUserLoggedIn(context: Context, user: Engineer) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isManager = (user.role == EngineerRole.MANAGER || user.role == EngineerRole.ADMIN)
                val displayName = user.fullName.takeIf { it.isNotBlank() } ?: user.name

                val oldName = prefs.getString(KEY_ENGINEER_NAME, null)
                val isUserChanged = oldName != null && !oldName.equals(displayName, ignoreCase = true)

                val editor = prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_OUT, false)
                    .putBoolean(KEY_IS_MANAGER, isManager)
                    .putString(KEY_ENGINEER_NAME, displayName)

                if (isUserChanged) {
                    // Only purge cached schedule when switching accounts to a completely different user
                    editor.remove(KEY_MANAGER_DUTY_LIST_JSON)
                    editor.remove(KEY_SHIFTS_JSON)
                } else if (!isManager) {
                    editor.remove(KEY_MANAGER_DUTY_LIST_JSON)
                }
                editor.apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to onUserLoggedIn", e)
            }
        }

        fun findEngineerShifts(
            eng: Engineer,
            roster: Map<String, Map<String, String>>
        ): Map<String, String>? {
            // 1. Direct key lookups
            roster[eng.username]?.let { return it }
            roster[eng.id]?.let { return it }
            roster[eng.name]?.let { return it }
            if (eng.fullName.isNotBlank()) roster[eng.fullName]?.let { return it }
            roster[eng.name.uppercase()]?.let { return it }
            roster[eng.name.lowercase()]?.let { return it }

            // 2. First name lookup (e.g. "Shefeeq K K" -> "Shefeeq")
            val firstName = eng.name.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: ""
            if (firstName.isNotBlank()) {
                roster[firstName]?.let { return it }
                roster[firstName.uppercase()]?.let { return it }
                roster[firstName.lowercase()]?.let { return it }
            }

            // 3. Substring / Prefix match across all roster keys
            val engLower = eng.name.trim().lowercase()
            val firstLower = firstName.lowercase()
            for ((key, map) in roster) {
                val keyLower = key.trim().lowercase()
                if (keyLower == engLower ||
                    (firstLower.isNotBlank() && keyLower == firstLower) ||
                    engLower.startsWith(keyLower) ||
                    keyLower.startsWith(engLower)) {
                    return map
                }
            }
            return null
        }

        fun saveManagerWidgetData(
            context: Context,
            managerName: String,
            activeCode: String,
            activeName: String,
            activeTiming: String,
            activeStaff: String,
            nextCode: String,
            nextName: String,
            nextTiming: String,
            nextStaff: String,
            coverageText: String,
            restText: String
        ) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_IS_MANAGER, true)
                    .putString(KEY_ENGINEER_NAME, managerName)
                    .putString(KEY_MANAGER_ACTIVE_CODE, activeCode)
                    .putString(KEY_MANAGER_ACTIVE_NAME, activeName)
                    .putString(KEY_MANAGER_ACTIVE_TIMING, activeTiming)
                    .putString(KEY_MANAGER_ACTIVE_STAFF, activeStaff)
                    .putString(KEY_MANAGER_NEXT_CODE, nextCode)
                    .putString(KEY_MANAGER_NEXT_NAME, nextName)
                    .putString(KEY_MANAGER_NEXT_TIMING, nextTiming)
                    .putString(KEY_MANAGER_NEXT_STAFF, nextStaff)
                    .putString(KEY_MANAGER_COVERAGE_TEXT, coverageText)
                    .putString(KEY_MANAGER_REST_TEXT, restText)
                    .apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to saveManagerWidgetData", e)
            }
        }

        fun saveManagerDutyRoster(
            context: Context,
            managerName: String,
            dateFormatted: String,
            dutyList: List<WidgetDutyItem>,
            summaryText: String,
            fullRoster: Map<String, Map<String, String>>? = null,
            yesterdayRoster: Map<String, Map<String, String>>? = null,
            engineers: List<Engineer>? = null,
            monthKey: String? = null
        ) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_OUT, false)
                    .putBoolean(KEY_IS_MANAGER, true)
                    .putString(KEY_ENGINEER_NAME, managerName)
                    .putString(KEY_MANAGER_DATE_STR, dateFormatted)
                    .putString(KEY_MANAGER_DUTY_LIST_JSON, gson.toJson(dutyList))
                    .putString(KEY_MANAGER_SUMMARY_STR, summaryText)

                if (fullRoster != null) {
                    editor.putString(KEY_FULL_ROSTER_JSON, gson.toJson(fullRoster))
                }
                if (yesterdayRoster != null) {
                    editor.putString(KEY_YESTERDAY_ROSTER_JSON, gson.toJson(yesterdayRoster))
                }
                if (engineers != null) {
                    editor.putString(KEY_ENGINEERS_JSON, gson.toJson(engineers))
                }
                if (monthKey != null) {
                    editor.putString(KEY_ROSTER_MONTH_KEY, monthKey)
                }
                editor.apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to saveManagerDutyRoster", e)
            }
        }

        fun syncManagerOrAdminRoster(
            context: Context,
            managerName: String,
            engineers: List<Engineer>,
            roster: Map<String, Map<String, String>>,
            yesterdayRoster: Map<String, Map<String, String>>? = null,
            monthKey: String? = null
        ) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_OUT, false)
                    .putBoolean(KEY_IS_MANAGER, true)
                    .putString(KEY_ENGINEER_NAME, managerName)
                    .putString(KEY_FULL_ROSTER_JSON, gson.toJson(roster))
                    .putString(KEY_ENGINEERS_JSON, gson.toJson(engineers))

                if (yesterdayRoster != null) {
                    editor.putString(KEY_YESTERDAY_ROSTER_JSON, gson.toJson(yesterdayRoster))
                }
                if (monthKey != null) {
                    editor.putString(KEY_ROSTER_MONTH_KEY, monthKey)
                }
                editor.apply()

                recomputeManagerDutyList(context)
                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to syncManagerOrAdminRoster", e)
            }
        }

        fun recomputeManagerDutyList(
            context: Context,
            targetDate: LocalDate = LocalDate.now(),
            targetTime: LocalTime = LocalTime.now()
        ): Boolean {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val rosterJson = prefs.getString(KEY_FULL_ROSTER_JSON, null)
                val engJson = prefs.getString(KEY_ENGINEERS_JSON, null)
                val yesterdayRosterJson = prefs.getString(KEY_YESTERDAY_ROSTER_JSON, null)

                val dateFormatted = targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))

                if (rosterJson.isNullOrBlank() || engJson.isNullOrBlank()) {
                    // Update header date so widget never displays an outdated calendar day
                    prefs.edit().putString(KEY_MANAGER_DATE_STR, dateFormatted).apply()
                    try {
                        androidx.work.WorkManager.getInstance(context).enqueue(
                            androidx.work.OneTimeWorkRequestBuilder<com.rosterx.worker.RosterSyncWorker>().build()
                        )
                    } catch (_: Throwable) {}
                    return false
                }

                val typeRoster = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                val typeEngineers = object : TypeToken<List<Engineer>>() {}.type

                val fullRoster: Map<String, Map<String, String>> = gson.fromJson(rosterJson, typeRoster) ?: emptyMap()
                val yesterdayRoster: Map<String, Map<String, String>> = if (!yesterdayRosterJson.isNullOrBlank()) {
                    gson.fromJson(yesterdayRosterJson, typeRoster) ?: emptyMap()
                } else fullRoster

                val engineers: List<Engineer> = gson.fromJson(engJson, typeEngineers) ?: emptyList()
                val activeEngineers = engineers.filter { it.isEligibleForShiftRoster(targetDate) }

                val todayDayStr = targetDate.dayOfMonth.toString()
                val yesterday = targetDate.minusDays(1)
                val yesterdayDayStr = yesterday.dayOfMonth.toString()

                val nowMin = targetTime.hour * 60 + targetTime.minute
                val isPostMidnightMorning = nowMin < 450 // 00:00 to 07:30 AM

                val onDutyItems = mutableListOf<WidgetDutyItem>()
                val restItems = mutableListOf<WidgetDutyItem>()
                val overnightActiveEngNames = mutableSetOf<String>()

                // 1. Overnight Shift Rollover: If between 00:00 and 07:30 AM, yesterday's NS engineer is actively on duty!
                if (isPostMidnightMorning) {
                    activeEngineers.forEach { eng ->
                        val yShiftMap = findEngineerShifts(eng, yesterdayRoster) ?: findEngineerShifts(eng, fullRoster)
                        val yCode = (yShiftMap?.get(yesterdayDayStr) ?: "OFF").trim().uppercase()

                        if (yCode == "NS") {
                            overnightActiveEngNames.add(eng.name.lowercase())
                            overnightActiveEngNames.add(eng.id.lowercase())
                            overnightActiveEngNames.add(eng.username.lowercase())
                            val meta = resolveMeta("NS")
                            onDutyItems.add(
                                WidgetDutyItem(
                                    engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                    shiftCode = "NS",
                                    shiftName = "Night Shift",
                                    timing = "09:00 PM – 07:30 AM (+1)",
                                    bgHex = meta.bg,
                                    textHex = meta.text,
                                    isActiveNow = true,
                                    itemType = WidgetDutyItem.TYPE_DUTY
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

                    val shiftMap = findEngineerShifts(eng, fullRoster)
                    val rawCode = (shiftMap?.get(todayDayStr) ?: "OFF").trim().uppercase()

                    val isRest = rawCode in setOf("OFF", "L") || rawCode.startsWith("SW")
                    if (!isRest) {
                        val meta = resolveMeta(rawCode)
                        val isActive = when (rawCode) {
                            "FS" -> nowMin in 450..990
                            "FG" -> nowMin in 510..1050
                            "G"  -> nowMin in 570..1110
                            "SG" -> nowMin in 660..1200
                            "SS" -> nowMin in 780..1320
                            "NS" -> nowMin >= 1260 // Today's NS is active after 21:00 PM
                            else -> false
                        }

                        val alreadyAdded = onDutyItems.any {
                            it.engineerName.equals(eng.name, ignoreCase = true) ||
                            (eng.fullName.isNotBlank() && it.engineerName.equals(eng.fullName, ignoreCase = true))
                        }
                        if (!alreadyAdded) {
                            onDutyItems.add(
                                WidgetDutyItem(
                                    engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                    shiftCode = rawCode,
                                    shiftName = meta.name,
                                    timing = meta.timing,
                                    bgHex = meta.bg,
                                    textHex = meta.text,
                                    isActiveNow = isActive,
                                    itemType = WidgetDutyItem.TYPE_DUTY
                                )
                            )
                        }
                    } else {
                        if (!isOvernightActive) {
                            val restDesc = when {
                                rawCode.startsWith("SW") -> "Shift Swap Off"
                                rawCode == "L" -> "Approved Leave"
                                else -> "Scheduled Rest"
                            }
                            val meta = resolveMeta(rawCode)
                            restItems.add(
                                WidgetDutyItem(
                                    engineerName = eng.fullName.takeIf { it.isNotBlank() } ?: eng.name,
                                    shiftCode = rawCode,
                                    shiftName = restDesc,
                                    timing = "Rest Day",
                                    bgHex = meta.bg,
                                    textHex = meta.text,
                                    isActiveNow = false,
                                    itemType = WidgetDutyItem.TYPE_REST
                                )
                            )
                        }
                    }
                }

                // Sort On Duty: Overnight active NS engineer first, then by shift start time
                fun shiftStartMin(item: WidgetDutyItem): Int {
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

                val finalWidgetList = mutableListOf<WidgetDutyItem>()
                if (onDutyItems.isNotEmpty()) {
                    finalWidgetList.add(
                        WidgetDutyItem(
                            itemType = WidgetDutyItem.TYPE_HEADER,
                            sectionTitle = "⚡ ON DUTY",
                            sectionCount = if (onDutyItems.size == 1) "1 Engineer" else "${onDutyItems.size} Engineers"
                        )
                    )
                    finalWidgetList.addAll(onDutyItems)
                }

                if (restItems.isNotEmpty()) {
                    finalWidgetList.add(
                        WidgetDutyItem(
                            itemType = WidgetDutyItem.TYPE_HEADER,
                            sectionTitle = "☕ REST & LEAVE",
                            sectionCount = if (restItems.size == 1) "1 Engineer" else "${restItems.size} Engineers"
                        )
                    )
                    finalWidgetList.addAll(restItems)
                }

                val summaryStr = "Total on Duty: ${onDutyItems.size} Engineers | Rest: ${restItems.size} Engineers"

                prefs.edit()
                    .putString(KEY_MANAGER_DATE_STR, dateFormatted)
                    .putString(KEY_MANAGER_SUMMARY_STR, summaryStr)
                    .putString(KEY_MANAGER_DUTY_LIST_JSON, gson.toJson(finalWidgetList))
                    .apply()

                return true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to recomputeManagerDutyList", e)
                return false
            }
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, RosterXAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, RosterXAppWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to updateAllWidgets", e)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_WIDGET,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, RosterXAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null) {
                    for (appWidgetId in appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_rosterx)

            // Detect widget dimensions for responsive layout adaptation
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val isCompact = minWidth in 1..210

            // 1. PendingIntent: Launch MainActivity on widget click
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingLaunch = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingLaunch)

            // 2. Load cached data from SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isLoggedOut = prefs.getBoolean(KEY_IS_LOGGED_OUT, false)
            var engineerName = prefs.getString(KEY_ENGINEER_NAME, null)
            val isManager = prefs.getBoolean(KEY_IS_MANAGER, false)
            val shiftsJson = prefs.getString(KEY_SHIFTS_JSON, null)
            val dutyListJson = prefs.getString(KEY_MANAGER_DUTY_LIST_JSON, null)

            // Fallback: Check user session if engineer name not in widget cache
            if (!isLoggedOut && engineerName.isNullOrBlank()) {
                val sessionPrefs = context.getSharedPreferences("rosterx_user_session", Context.MODE_PRIVATE)
                val userJson = sessionPrefs.getString("saved_user", null)
                if (!userJson.isNullOrBlank()) {
                    try {
                        val userObj = gson.fromJson(userJson, Engineer::class.java)
                        if (userObj != null && userObj.name.isNotBlank()) {
                            engineerName = userObj.name
                        }
                    } catch (ignored: Exception) {}
                }
            }

            // A. SECURE SIGNED-OUT STATE: Display locked placeholder
            if (isLoggedOut || engineerName == "Signed Out") {
                views.setTextViewText(R.id.widget_engineer_name, "Signed Out")
                views.setTextViewText(R.id.widget_today_title, "SESSION")
                views.setTextViewText(R.id.widget_today_badge, "LOCK")
                views.setInt(R.id.widget_today_badge_bg, "setColorFilter", Color.parseColor("#475569"))
                views.setTextColor(R.id.widget_today_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_today_name, "Tap to Sign In")
                views.setTextViewText(R.id.widget_today_timing, "Open RosterX app")

                views.setTextViewText(R.id.widget_tomorrow_title, "ROSTER")
                views.setTextViewText(R.id.widget_tomorrow_badge, "AUTH")
                views.setInt(R.id.widget_tomorrow_badge_bg, "setColorFilter", Color.parseColor("#475569"))
                views.setTextColor(R.id.widget_tomorrow_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_tomorrow_name, "Sign in required")
                views.setTextViewText(R.id.widget_tomorrow_timing, "Private schedule")

                views.setTextViewText(R.id.widget_timer_text, "🔒 Sign in to access your NOC shifts")
                views.setTextViewText(R.id.widget_rest_text, "RosterX Secure Portal")

                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            // B. OPERATIONS RADAR WIDGET VIEW: Strictly for Managers/Admins
            if (isManager) {
                val managerViews = RemoteViews(context.packageName, R.layout.widget_rosterx_manager)

                // Launch MainActivity on widget tap
                managerViews.setOnClickPendingIntent(R.id.widget_root, pendingLaunch)

                // Dynamically recompute today's duty roster for current date & time
                val today = LocalDate.now()
                val todayFmt = today.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                val recomputed = recomputeManagerDutyList(context, today, LocalTime.now())

                val dateStr = prefs.getString(KEY_MANAGER_DATE_STR, todayFmt) ?: todayFmt
                val updatedDutyJson = prefs.getString(KEY_MANAGER_DUTY_LIST_JSON, null)
                val summaryStr = if (updatedDutyJson.isNullOrBlank()) {
                    "NOC Operations Radar • Supervisory Desk Access"
                } else {
                    prefs.getString(KEY_MANAGER_SUMMARY_STR, "Today's Coverage: Active Duty") ?: "Today's Coverage: Active Duty"
                }

                managerViews.setTextViewText(R.id.widget_manager_date, dateStr)
                managerViews.setTextViewText(R.id.widget_manager_summary, summaryStr)

                val serviceIntent = Intent(context, RosterXWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                managerViews.setRemoteAdapter(R.id.widget_manager_list, serviceIntent)
                managerViews.setEmptyView(R.id.widget_manager_list, R.id.widget_manager_empty)

                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_manager_list)
                appWidgetManager.updateAppWidget(appWidgetId, managerViews)

                // Schedule next boundary alarm (midnight or shift start/end)
                scheduleNextUpdateAlarm(context)
                return
            }

            // C. PERSONAL ENGINEER WIDGET: Loading / Syncing placeholder
            if (shiftsJson.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_engineer_name, engineerName ?: "Engineer")
                views.setTextViewText(R.id.widget_today_title, "TODAY")
                views.setTextViewText(R.id.widget_today_badge, "SYNC")
                views.setInt(R.id.widget_today_badge_bg, "setColorFilter", Color.parseColor("#0284C7"))
                views.setTextColor(R.id.widget_today_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_today_name, "Syncing Schedule...")
                views.setTextViewText(R.id.widget_today_timing, "Fetching live shifts")

                views.setTextViewText(R.id.widget_tomorrow_title, "TOMORROW")
                views.setTextViewText(R.id.widget_tomorrow_badge, "SYNC")
                views.setInt(R.id.widget_tomorrow_badge_bg, "setColorFilter", Color.parseColor("#0284C7"))
                views.setTextColor(R.id.widget_tomorrow_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_tomorrow_name, "Connecting to Atlas...")
                views.setTextViewText(R.id.widget_tomorrow_timing, "Please wait a moment")

                views.setTextViewText(R.id.widget_timer_text, "Connecting to RosterX live gateway...")
                views.setTextViewText(R.id.widget_rest_text, "Schedule update in progress")

                appWidgetManager.updateAppWidget(appWidgetId, views)
                scheduleNextUpdateAlarm(context)
                return
            }

            views.setTextViewText(R.id.widget_engineer_name, engineerName ?: "Engineer")

            val type = object : TypeToken<Map<String, String>>() {}.type
            val shiftsMap: Map<String, String> = try {
                gson.fromJson(shiftsJson, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val todayNum = today.dayOfMonth
            val tomorrowNum = tomorrow.dayOfMonth

            val todayCode = (shiftsMap[todayNum.toString()] ?: "OFF").trim().uppercase()
            val tomorrowCode = (shiftsMap[tomorrowNum.toString()] ?: "OFF").trim().uppercase()

            // 3. Shift metadata resolution
            val todayMeta = resolveMeta(todayCode)
            val tomorrowMeta = resolveMeta(tomorrowCode)

            // Today Card (responsive titles to prevent multi-line wrap on narrow 2x2 widgets)
            val todayTitleStr = if (isCompact) "TODAY (${String.format(Locale.US, "%02d", todayNum)})" else "TODAY (${today.format(DateTimeFormatter.ofPattern("MMM dd"))})"
            views.setTextViewText(R.id.widget_today_title, todayTitleStr)
            views.setTextViewText(R.id.widget_today_badge, todayCode)
            views.setInt(R.id.widget_today_badge_bg, "setColorFilter", Color.parseColor(todayMeta.bg))
            views.setTextColor(R.id.widget_today_badge, Color.parseColor(todayMeta.text))
            views.setTextViewText(R.id.widget_today_name, todayMeta.name)
            views.setTextViewText(R.id.widget_today_timing, todayMeta.timing)

            // Tomorrow Card
            val tmrwTitleStr = if (isCompact) "TMRW (${String.format(Locale.US, "%02d", tomorrowNum)})" else "TOMORROW (${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd"))})"
            views.setTextViewText(R.id.widget_tomorrow_title, tmrwTitleStr)
            views.setTextViewText(R.id.widget_tomorrow_badge, tomorrowCode)
            views.setInt(R.id.widget_tomorrow_badge_bg, "setColorFilter", Color.parseColor(tomorrowMeta.bg))
            views.setTextColor(R.id.widget_tomorrow_badge, Color.parseColor(tomorrowMeta.text))
            views.setTextViewText(R.id.widget_tomorrow_name, tomorrowMeta.name)
            views.setTextViewText(R.id.widget_tomorrow_timing, tomorrowMeta.timing)

            // 4. Widget Shift Status Display (Static times without minutes to avoid stale widget countdowns)
            val now = LocalTime.now()
            val curSec = now.toSecondOfDay().toLong()

            fun extractTimingBounds(timingStr: String): Pair<String, String> {
                val cleaned = timingStr.replace("(+1)", "").trim()
                val parts = cleaned.split("–", "-").map { it.trim() }
                val start = parts.getOrNull(0) ?: "09:00"
                val end = parts.getOrNull(1) ?: "18:00"
                return Pair(start, end)
            }

            var shiftTimerText = "Scheduled Rest Day (OFF)"
            val activeWindow = shiftRegistry[todayCode]
            var nextAlarmEpochMillis: Long? = null

            if (activeWindow != null && activeWindow.endSec > 0) {
                var adjCur = curSec
                if (todayCode == "NS" && curSec < 27000L) {
                    adjCur += 86400L
                }

                val (startTime, endTime) = extractTimingBounds(activeWindow.timing)

                if (adjCur in activeWindow.startSec until activeWindow.endSec) {
                    val remainingSec = activeWindow.endSec - adjCur
                    shiftTimerText = "Shift Ends at $endTime"
                    // Schedule alarm right when shift ends so widget seamlessly updates
                    nextAlarmEpochMillis = System.currentTimeMillis() + (remainingSec * 1000L)
                } else if (adjCur < activeWindow.startSec) {
                    val untilStartSec = activeWindow.startSec - adjCur
                    shiftTimerText = "Duty Starts at $startTime"
                    // Schedule alarm right when shift begins so widget seamlessly updates
                    nextAlarmEpochMillis = System.currentTimeMillis() + (untilStartSec * 1000L)
                }
            }

            if (shiftTimerText == "Scheduled Rest Day (OFF)" || (activeWindow != null && curSec >= activeWindow.endSec && todayCode != "NS")) {
                var nextCode: String? = null
                var daysAhead = 0

                for (d in (todayNum + 1)..today.lengthOfMonth()) {
                    val candidate = (shiftsMap[d.toString()] ?: "").trim().uppercase()
                    if (shiftRegistry.containsKey(candidate) && candidate != "OFF" && candidate != "L") {
                        nextCode = candidate
                        daysAhead = d - todayNum
                        break
                    }
                }

                if (nextCode != null && shiftRegistry.containsKey(nextCode)) {
                    val nextMeta = shiftRegistry[nextCode]!!
                    val (nextStart, _) = extractTimingBounds(nextMeta.timing)
                    val secUntilMidnight = (86400L - curSec).coerceAtLeast(0)
                    val interSec = (daysAhead - 1) * 86400L
                    val totalSec = secUntilMidnight + interSec + nextMeta.startSec
                    nextAlarmEpochMillis = System.currentTimeMillis() + (totalSec * 1000L)

                    shiftTimerText = if (daysAhead == 1) {
                        "Next Shift: Tomorrow at $nextStart"
                    } else {
                        val nextDate = today.plusDays(daysAhead.toLong())
                        val nextDateStr = nextDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                        "Next Shift: $nextDateStr at $nextStart"
                    }
                }
            }
            views.setTextViewText(R.id.widget_timer_text, shiftTimerText)

            // 5. Dynamic Upcoming Rest Day Calculation
            val todayIsRest = todayCode == "OFF" || todayCode.startsWith("SW") || todayCode == "L"
            var restDayText = if (isCompact) "Next Rest: Next Cycle" else "Next Rest Day: Scheduled Next Cycle"

            if (todayIsRest) {
                restDayText = if (isCompact) "Rest: Today" else "Rest Day: Today"
            } else {
                var restDaysAway: Int? = null
                var restDateFmt: String? = null

                for (d in (todayNum + 1)..today.lengthOfMonth()) {
                    val s = (shiftsMap[d.toString()] ?: "").trim().uppercase()
                    if (s == "OFF" || s.startsWith("SW") || s == "L") {
                        restDaysAway = d - todayNum
                        val dObj = today.withDayOfMonth(d)
                        restDateFmt = dObj.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                        break
                    }
                }

                if (restDaysAway == 1) {
                    restDayText = if (isCompact) "Next Rest: Tomorrow" else "Next Rest Day: Tomorrow ($restDateFmt)"
                } else if (restDaysAway == 2) {
                    restDayText = if (isCompact) "Next Rest: In 2 Days" else "Next Rest Day: In 2 Days ($restDateFmt)"
                } else if (restDaysAway != null) {
                    restDayText = if (isCompact) "Next Rest: In $restDaysAway Days" else "Next Rest Day: In $restDaysAway Days ($restDateFmt)"
                }
            }
            views.setTextViewText(R.id.widget_rest_text, restDayText)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // 6. Schedule transition & midnight alarm
            scheduleNextUpdateAlarm(context, nextAlarmEpochMillis)
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error inside updateWidget for id $appWidgetId", e)
        }
    }

    private fun scheduleNextUpdateAlarm(context: Context, targetEpochMillis: Long? = null) {
        try {
            val nowMillis = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()

            // 1. Midnight rollover alarm (fires 2 seconds after midnight)
            val nextMidnight = today.plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli() + 2000L

            val candidates = mutableListOf<Long>()
            candidates.add(nextMidnight)

            if (targetEpochMillis != null && targetEpochMillis > nowMillis) {
                candidates.add(targetEpochMillis)
            }

            // 2. Standard shift boundary transitions today
            val boundaryMinutes = listOf(450, 510, 570, 660, 780, 990, 1050, 1110, 1200, 1260, 1320)
            for (bm in boundaryMinutes) {
                val boundaryTime = today.atTime(bm / 60, bm % 60).atZone(zone).toInstant().toEpochMilli() + 1000L
                if (boundaryTime > nowMillis) {
                    candidates.add(boundaryTime)
                }
            }

            val nextAlarm = candidates.filter { it > nowMillis }.minOrNull() ?: nextMidnight
            scheduleAlarm(context, nextAlarm)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scheduleNextUpdateAlarm", e)
        }
    }

    private fun scheduleAlarm(context: Context, epochMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, RosterXAppWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, epochMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, epochMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule transition alarm", e)
        }
    }
}
