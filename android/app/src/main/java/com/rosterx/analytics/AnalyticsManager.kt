package com.rosterx.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AnalyticsEventRecord(
    val id: String = UUID.randomUUID().toString(),
    val eventName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeLabel: String = "",
    val parameters: Map<String, String> = emptyMap()
)

data class FirebaseAnalyticsSummary(
    val sessionStartTime: Long = System.currentTimeMillis(),
    val currentUserId: String = "admin",
    val currentUserRole: String = "ADMIN",
    val totalEventsCount: Int = 0,
    val screenViewsCount: Int = 0,
    val loginsCount: Int = 0,
    val broadcastsCount: Int = 0,
    val lastEventName: String = "Session Initialized",
    val recentEvents: List<AnalyticsEventRecord> = emptyList()
)

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    private val sessionStartEpoch = System.currentTimeMillis()
    private var activeUserId: String = "admin"
    private var activeUserRole: String = "ADMIN"
    private val eventHistory = mutableListOf<AnalyticsEventRecord>()
    private var screenViews = 0
    private var logins = 0
    private var broadcasts = 0

    private val istDateFormat by lazy {
        SimpleDateFormat("hh:mm:ss a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }

    private val _summaryState = MutableStateFlow(FirebaseAnalyticsSummary(sessionStartTime = sessionStartEpoch))
    val summaryState: StateFlow<FirebaseAnalyticsSummary> = _summaryState.asStateFlow()

    private fun recordLocalEvent(name: String, params: Map<String, String> = emptyMap()) {
        val now = System.currentTimeMillis()
        val timeStr = istDateFormat.format(Date(now))
        val record = AnalyticsEventRecord(
            eventName = name,
            timestamp = now,
            timeLabel = timeStr,
            parameters = params
        )
        synchronized(eventHistory) {
            eventHistory.add(0, record)
            if (eventHistory.size > 40) {
                eventHistory.removeAt(eventHistory.size - 1)
            }
            _summaryState.value = FirebaseAnalyticsSummary(
                sessionStartTime = sessionStartEpoch,
                currentUserId = activeUserId,
                currentUserRole = activeUserRole,
                totalEventsCount = eventHistory.size,
                screenViewsCount = screenViews,
                loginsCount = logins,
                broadcastsCount = broadcasts,
                lastEventName = name,
                recentEvents = eventHistory.toList()
            )
        }
    }

    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics.setUserId(userId)
            activeUserId = userId ?: "admin"
            recordLocalEvent("set_user_id", mapOf("user_id" to (userId ?: "anonymous")))
            Log.d(TAG, "Analytics: setUserId($userId)")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics setUserId error: ${e.message}")
        }
    }

    fun setUserRole(role: String?) {
        try {
            firebaseAnalytics.setUserProperty("user_role", role ?: "unknown")
            activeUserRole = role ?: "ADMIN"
            recordLocalEvent("set_user_role", mapOf("role" to (role ?: "unknown")))
            Log.d(TAG, "Analytics: setUserRole($role)")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics setUserRole error: ${e.message}")
        }
    }

    fun logScreenView(screenName: String, screenClass: String = "ComposeScreen") {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            screenViews++
            recordLocalEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf("screen_name" to screenName, "class" to screenClass))
            Log.d(TAG, "Analytics: ScreenView -> $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logScreenView error: ${e.message}")
        }
    }

    fun logLogin(username: String, role: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "app_credentials")
                putString("username", username)
                putString("role", role)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
            setUserId(username)
            setUserRole(role)
            logins++
            recordLocalEvent(FirebaseAnalytics.Event.LOGIN, mapOf("username" to username, "role" to role))
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logLogin error: ${e.message}")
        }
    }

    fun logBroadcastDispatched(title: String, priority: String, recipientCount: Int) {
        try {
            val bundle = Bundle().apply {
                putString("broadcast_title", title.take(50))
                putString("priority", priority)
                putInt("recipient_count", recipientCount)
            }
            firebaseAnalytics.logEvent("broadcast_dispatched", bundle)
            broadcasts++
            recordLocalEvent("broadcast_dispatched", mapOf("title" to title.take(30), "priority" to priority, "recipients" to recipientCount.toString()))
            Log.d(TAG, "Analytics: broadcast_dispatched -> $title ($priority, $recipientCount recipients)")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logBroadcastDispatched error: ${e.message}")
        }
    }

    fun logNotificationReceived(title: String, priority: String) {
        try {
            val bundle = Bundle().apply {
                putString("notification_title", title.take(50))
                putString("priority", priority)
            }
            firebaseAnalytics.logEvent("notification_received", bundle)
            recordLocalEvent("notification_received", mapOf("title" to title.take(30), "priority" to priority))
            Log.d(TAG, "Analytics: notification_received -> $title")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logNotificationReceived error: ${e.message}")
        }
    }

    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val bundle = Bundle().apply {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value?.toString() ?: "")
                    }
                }
            }
            firebaseAnalytics.logEvent(eventName, bundle)
            val strParams = params.mapValues { it.value?.toString() ?: "" }
            recordLocalEvent(eventName, strParams)
            Log.d(TAG, "Analytics: Custom Event -> $eventName: $params")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logEvent error: ${e.message}")
        }
    }

    fun getSummary(): FirebaseAnalyticsSummary = _summaryState.value

    companion object {
        private const val TAG = "RosterX_Analytics"
    }
}
