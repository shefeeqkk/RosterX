package com.rosterx.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics.setUserId(userId)
            Log.d(TAG, "Analytics: setUserId($userId)")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics setUserId error: ${e.message}")
        }
    }

    fun setUserRole(role: String?) {
        try {
            firebaseAnalytics.setUserProperty("user_role", role ?: "unknown")
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
            Log.d(TAG, "Analytics: Custom Event -> $eventName: $params")
        } catch (e: Exception) {
            Log.w(TAG, "Analytics logEvent error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RosterX_Analytics"
    }
}
