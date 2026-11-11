package com.rosterx.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rosterx.MainActivity
import com.rosterx.R
import com.rosterx.data.local.UserSessionManager
import com.rosterx.data.remote.RosterApiService
import com.rosterx.analytics.AnalyticsManager
import com.rosterx.util.RosterXSoundManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RosterXFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var sessionManager: UserSessionManager

    @Inject
    lateinit var apiService: RosterApiService

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM device token: $token")
        sessionManager.setFcmToken(token)
        
        // Dispatch token to backend if user is authenticated
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenSaved = sessionManager.getToken()
                if (!tokenSaved.isNullOrBlank()) {
                    apiService.registerFcmToken(mapOf("token" to token))
                    Log.d(TAG, "FCM token successfully registered with RosterX server")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register FCM token with server: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Client-Side Identity Filter:
        // 1. If device is signed out (no active user session), drop notification silently
        val currentUser = sessionManager.getCurrentUser()
        if (currentUser == null) {
            Log.d(TAG, "Dropping FCM push: Device has no active user session (signed out).")
            return
        }

        // 2. If message is targeted to a specific username, ensure it matches active logged-in user
        val targetUser = remoteMessage.data["target_user"]?.trim()?.lowercase()
        if (!targetUser.isNullOrBlank() && targetUser != "everyone" && targetUser != "all") {
            val activeUsername = currentUser.username.trim().lowercase()
            if (targetUser != activeUsername) {
                Log.d(TAG, "Dropping FCM push: Target recipient '$targetUser' does not match active logged-in user '$activeUsername'.")
                return
            }
        }

        // 1. Extract payload from data or notification object
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "Operations Alert"

        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: "New operational broadcast received."

        val priority = remoteMessage.data["priority"] ?: "info"
        val notificationId = remoteMessage.data["id"] ?: "notif_${System.currentTimeMillis()}"

        // 2. Display native Android notification panel alert
        showSystemNotification(title, body, priority, notificationId)

        // 3. Play soothing Warm Crystal Chime in-app immediately
        try {
            RosterXSoundManager.playNotificationChime(applicationContext)
        } catch (_: Exception) {}

        // 4. Log event to Firebase Analytics
        try {
            analyticsManager.logNotificationReceived(title, priority)
        } catch (_: Exception) {}

        // 4. Instantly refresh home screen widgets & sync in background
        try {
            com.rosterx.widget.RosterXAppWidgetProvider.updateAllWidgets(applicationContext)
            val syncNow = androidx.work.OneTimeWorkRequestBuilder<com.rosterx.worker.RosterSyncWorker>().build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(syncNow)
        } catch (_: Exception) {}
    }

    private fun showSystemNotification(
        title: String,
        body: String,
        priority: String,
        notificationId: String
    ) {
        val channelId = CHANNEL_ID_BROADCASTS
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NOTIF_ID", notificationId)
            putExtra("EXTRA_NAV_TAB", 0) // Default to Operations Dashboard
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RosterXSoundManager.getNotificationSoundUri(this)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (priority.equals("urgent", ignoreCase = true)) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_HIGH
                }
            )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Android O Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete legacy channel if present to migrate sound
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID_BROADCASTS_LEGACY)
            } catch (_: Exception) {}

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Operations & Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live shift updates, urgent operational alerts, and team broadcasts"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationTag = "RosterX_$notificationId"
        notificationManager.notify(notificationTag, System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "RosterX_FCM"
        const val CHANNEL_ID_BROADCASTS_LEGACY = "rosterx_operational_broadcasts"
        const val CHANNEL_ID_BROADCASTS = "rosterx_operational_broadcasts_v2"
    }
}
