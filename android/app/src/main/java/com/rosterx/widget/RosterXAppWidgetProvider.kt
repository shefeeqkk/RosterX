package com.rosterx.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rosterx.MainActivity
import com.rosterx.R
import com.rosterx.domain.model.Engineer
import java.time.LocalDate
import java.time.LocalTime
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

        private val gson = Gson()

        fun saveWidgetData(context: Context, engineerName: String, shiftsMap: Map<String, String>, monthKey: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_ENGINEER_NAME, engineerName)
                    .putString(KEY_SHIFTS_JSON, gson.toJson(shiftsMap))
                    .putString(KEY_MONTH_KEY, monthKey)
                    .apply()

                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to saveWidgetData", e)
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

            if (shiftsJson.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_engineer_name, engineerName ?: "NOC Operations")
                views.setTextViewText(R.id.widget_today_badge, "OK")
                views.setTextViewText(R.id.widget_today_name, "Tap to Sync Shifts")
                views.setTextViewText(R.id.widget_today_timing, "Open RosterX app")
                views.setTextViewText(R.id.widget_tomorrow_badge, "OK")
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
            data class ShiftMeta(val name: String, val timing: String, val bg: String, val text: String, val startSec: Long, val endSec: Long)
            val shiftRegistry = mapOf(
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
                if (shiftRegistry.containsKey(code)) return shiftRegistry[code]!!
                if (code.startsWith("SW")) return ShiftMeta("Swap Shift", "Covered Duty", "#FEF08A", "#854D0E", 0L, 0L)
                return ShiftMeta(code, "Assigned Duty", "#E2E8F0", "#334155", 0L, 0L)
            }

            val todayMeta = resolveMeta(todayCode)
            val tomorrowMeta = resolveMeta(tomorrowCode)

            // Today Card
            views.setTextViewText(R.id.widget_today_title, "TODAY (${today.format(DateTimeFormatter.ofPattern("MMM dd"))})")
            views.setTextViewText(R.id.widget_today_badge, todayCode)
            views.setInt(R.id.widget_today_badge, "setBackgroundColor", Color.parseColor(todayMeta.bg))
            views.setTextColor(R.id.widget_today_badge, Color.parseColor(todayMeta.text))
            views.setTextViewText(R.id.widget_today_name, todayMeta.name)
            views.setTextViewText(R.id.widget_today_timing, todayMeta.timing)

            // Tomorrow Card
            views.setTextViewText(R.id.widget_tomorrow_title, "TOMORROW (${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd"))})")
            views.setTextViewText(R.id.widget_tomorrow_badge, tomorrowCode)
            views.setInt(R.id.widget_tomorrow_badge, "setBackgroundColor", Color.parseColor(tomorrowMeta.bg))
            views.setTextColor(R.id.widget_tomorrow_badge, Color.parseColor(tomorrowMeta.text))
            views.setTextViewText(R.id.widget_tomorrow_name, tomorrowMeta.name)
            views.setTextViewText(R.id.widget_tomorrow_timing, tomorrowMeta.timing)

            // 4. Live Duty Countdown Calculation
            val now = LocalTime.now()
            val curSec = now.toSecondOfDay().toLong()

            fun formatDelta(sec: Long): String {
                val safe = sec.coerceAtLeast(0)
                val h = safe / 3600
                val m = (safe % 3600) / 60
                return String.format(Locale.US, "%02dh %02dm", h, m)
            }

            var shiftTimerText = "No upcoming duty scheduled"
            val activeWindow = shiftRegistry[todayCode]

            if (activeWindow != null && activeWindow.endSec > 0) {
                var adjCur = curSec
                if (todayCode == "NS" && curSec < 27000L) {
                    adjCur += 86400L
                }

                if (adjCur in activeWindow.startSec until activeWindow.endSec) {
                    shiftTimerText = "Shift Ends in: ${formatDelta(activeWindow.endSec - adjCur)}"
                } else if (adjCur < activeWindow.startSec) {
                    shiftTimerText = "Duty Starts in: ${formatDelta(activeWindow.startSec - adjCur)}"
                }
            }

            if (shiftTimerText == "No upcoming duty scheduled" || (activeWindow != null && curSec >= activeWindow.endSec && todayCode != "NS")) {
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
                    val secUntilMidnight = (86400L - curSec).coerceAtLeast(0)
                    val interSec = (daysAhead - 1) * 86400L
                    val totalSec = secUntilMidnight + interSec + nextMeta.startSec
                    shiftTimerText = "Next Shift Starts in: ${formatDelta(totalSec)}"
                }
            }
            views.setTextViewText(R.id.widget_timer_text, shiftTimerText)

            // 5. Dynamic Upcoming Rest Day Calculation
            val todayIsRest = todayCode == "OFF" || todayCode.startsWith("SW") || todayCode == "L"
            var restDayText = "Next Rest Day: Scheduled Next Cycle"

            if (todayIsRest) {
                restDayText = "Rest Day: Today"
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
                    restDayText = "Next Rest Day: Tomorrow ($restDateFmt)"
                } else if (restDaysAway == 2) {
                    restDayText = "Next Rest Day: In 2 Days ($restDateFmt)"
                } else if (restDaysAway != null) {
                    restDayText = "Next Rest Day: In $restDaysAway Days ($restDateFmt)"
                }
            }
            views.setTextViewText(R.id.widget_rest_text, restDayText)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error inside updateWidget for id $appWidgetId", e)
        }
    }
}
