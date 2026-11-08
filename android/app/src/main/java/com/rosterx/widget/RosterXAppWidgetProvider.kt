package com.rosterx.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rosterx.MainActivity
import com.rosterx.R
import com.rosterx.domain.model.Engineer
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RosterXAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.rosterx.ACTION_UPDATE_WIDGET"
        private const val TAG = "RosterXWidget"
        private const val PREFS_NAME = "rosterx_widget_cache"
        private const val KEY_ENGINEER_NAME = "key_engineer_name"
        private const val KEY_SHIFTS_JSON = "key_shifts_json"
        private const val KEY_MONTH_KEY = "key_month_key"
        private const val KEY_IS_MANAGER = "key_is_manager"
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

        data class ShiftMeta(val name: String, val timing: String, val bg: String, val text: String, val startSec: Long, val endSec: Long)
        private val shiftRegistry = mapOf(
            "FS"  to ShiftMeta("First Shift", "07:30 – 16:30", "#BBF7D0", "#064E3B", 27000L, 59400L),
            "FG"  to ShiftMeta("First General", "08:30 – 17:30", "#166534", "#FFFFFF", 30600L, 63000L),
            "G"   to ShiftMeta("General Shift", "09:30 – 18:30", "#BAE6FD", "#0C4A6E", 34200L, 66600L),
            "SG"  to ShiftMeta("Second General", "11:00 – 20:00", "#EA580C", "#FFFFFF", 39600L, 72000L),
            "SS"  to ShiftMeta("Second Shift", "13:00 – 22:00", "#FED7AA", "#7C2D12", 46800L, 79200L),
            "NS"  to ShiftMeta("Night Shift", "21:00 – 07:30 (+1)", "#020617", "#FFFFFF", 75600L, 113400L),
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

        fun saveWidgetData(context: Context, engineerName: String, shiftsMap: Map<String, String>, monthKey: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_IS_MANAGER, false)
                    .putString(KEY_ENGINEER_NAME, engineerName)
                    .putString(KEY_SHIFTS_JSON, gson.toJson(shiftsMap))
                    .putString(KEY_MONTH_KEY, monthKey)
                    .apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to saveWidgetData", e)
            }
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
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
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
            var engineerName = prefs.getString(KEY_ENGINEER_NAME, null)
            val isManager = prefs.getBoolean(KEY_IS_MANAGER, false)
            val shiftsJson = prefs.getString(KEY_SHIFTS_JSON, null)

            // Fallback: Check user session if engineer name not in widget cache
            if (engineerName.isNullOrBlank()) {
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

            // MANAGER WIDGET VIEW: Display Live Active Duty & Shift Handoff
            if (isManager) {
                val managerName = engineerName ?: "Operations Manager"
                val activeCode = prefs.getString(KEY_MANAGER_ACTIVE_CODE, "SS") ?: "SS"
                val activeTiming = prefs.getString(KEY_MANAGER_ACTIVE_TIMING, "13:00 – 22:00") ?: "13:00 – 22:00"
                val activeStaff = prefs.getString(KEY_MANAGER_ACTIVE_STAFF, "Staff On Duty") ?: "Staff On Duty"
                val nextCode = prefs.getString(KEY_MANAGER_NEXT_CODE, "NS") ?: "NS"
                val nextTiming = prefs.getString(KEY_MANAGER_NEXT_TIMING, "21:00 – 07:30 (+1)") ?: "21:00 – 07:30 (+1)"
                val nextStaff = prefs.getString(KEY_MANAGER_NEXT_STAFF, "Upcoming Duty") ?: "Upcoming Duty"
                val coverageText = prefs.getString(KEY_MANAGER_COVERAGE_TEXT, "Today's Coverage: Live NOC Radar") ?: "Today's Coverage: Live NOC Radar"
                val restText = prefs.getString(KEY_MANAGER_REST_TEXT, "Scheduled Rest Days: Live") ?: "Scheduled Rest Days: Live"

                views.setTextViewText(R.id.widget_engineer_name, "$managerName (Manager)")

                val activeMeta = resolveMeta(activeCode)
                val nextMeta = resolveMeta(nextCode)

                // Left card: ACTIVE DUTY NOW
                views.setTextViewText(R.id.widget_today_title, if (isCompact) "ACTIVE NOW" else "ACTIVE DUTY NOW")
                views.setTextViewText(R.id.widget_today_badge, activeCode)
                views.setInt(R.id.widget_today_badge_bg, "setColorFilter", Color.parseColor(activeMeta.bg))
                views.setTextColor(R.id.widget_today_badge, Color.parseColor(activeMeta.text))
                views.setTextViewText(R.id.widget_today_name, activeStaff)
                views.setTextViewText(R.id.widget_today_timing, activeTiming)

                // Right card: UPCOMING SHIFT
                views.setTextViewText(R.id.widget_tomorrow_title, if (isCompact) "UPCOMING" else "UPCOMING SHIFT")
                views.setTextViewText(R.id.widget_tomorrow_badge, nextCode)
                views.setInt(R.id.widget_tomorrow_badge_bg, "setColorFilter", Color.parseColor(nextMeta.bg))
                views.setTextColor(R.id.widget_tomorrow_badge, Color.parseColor(nextMeta.text))
                views.setTextViewText(R.id.widget_tomorrow_name, nextStaff)
                views.setTextViewText(R.id.widget_tomorrow_timing, nextTiming)

                // Bottom indicators
                views.setTextViewText(R.id.widget_timer_text, coverageText)
                views.setTextViewText(R.id.widget_rest_text, restText)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            if (shiftsJson.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_engineer_name, engineerName ?: "NOC Operations")
                views.setTextViewText(R.id.widget_today_badge, "OK")
                views.setInt(R.id.widget_today_badge_bg, "setColorFilter", Color.parseColor("#10B981"))
                views.setTextColor(R.id.widget_today_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_today_name, "Tap to Sync Shifts")
                views.setTextViewText(R.id.widget_today_timing, "Open RosterX app")
                views.setTextViewText(R.id.widget_tomorrow_badge, "OK")
                views.setInt(R.id.widget_tomorrow_badge_bg, "setColorFilter", Color.parseColor("#10B981"))
                views.setTextColor(R.id.widget_tomorrow_badge, Color.WHITE)
                views.setTextViewText(R.id.widget_tomorrow_name, "Sync Active Roster")
                views.setTextViewText(R.id.widget_tomorrow_timing, "Tap widget to refresh")
                views.setTextViewText(R.id.widget_timer_text, "Tap widget to sync schedule")
                views.setTextViewText(R.id.widget_rest_text, "Next Rest Day: Tap to sync")
                appWidgetManager.updateAppWidget(appWidgetId, views)
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

            // 6. Schedule transition alarm when applicable
            if (nextAlarmEpochMillis != null && nextAlarmEpochMillis > System.currentTimeMillis()) {
                scheduleAlarm(context, nextAlarmEpochMillis)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error inside updateWidget for id $appWidgetId", e)
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
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, epochMillis, pendingIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule transition alarm", e)
        }
    }
}
