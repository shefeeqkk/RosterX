package com.rosterx.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rosterx.domain.model.Engineer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("rosterx_user_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<Engineer?>(loadSavedUser())
    val currentUser: StateFlow<Engineer?> = _currentUser.asStateFlow()

    private fun loadSavedUser(): Engineer? {
        val json = prefs.getString("saved_user", null) ?: return null
        return try {
            gson.fromJson(json, Engineer::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun setCurrentUser(user: Engineer) {
        _currentUser.value = user
        prefs.edit().putString("saved_user", gson.toJson(user)).apply()
    }

    fun clearSession() {
        _currentUser.value = null
        prefs.edit().remove("saved_user").apply()
    }

    fun hasSavedSession(): Boolean {
        return loadSavedUser() != null
    }

    fun getBaseUrl(): String {
        val saved = prefs.getString("custom_base_url", null)
        if (!saved.isNullOrBlank()) return saved

        // Automatically resolve synced internet tunnel URL from assets if present
        try {
            val assetUrl = context.assets.open("server_url.txt").bufferedReader().use { it.readText().trim() }
            if (assetUrl.isNotBlank()) {
                var clean = assetUrl
                if (!clean.endsWith("/")) clean += "/"
                if (!clean.endsWith("/api/")) clean += "api/"
                return clean
            }
        } catch (e: Exception) {
            // Assets file not present or not accessible; fall back to local LAN IP
        }

        return "https://rosterx.onrender.com/api/"
    }

    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        prefs.edit().putString("custom_base_url", cleanUrl).apply()
    }

    fun getDismissedNotifications(): Set<String> {
        return prefs.getStringSet("dismissed_notifications", emptySet()) ?: emptySet()
    }

    fun dismissNotification(id: String) {
        val current = getDismissedNotifications().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("dismissed_notifications", current).apply()
    }

    fun clearAllNotifications(ids: Collection<String>) {
        val current = getDismissedNotifications().toMutableSet()
        current.addAll(ids)
        prefs.edit().putStringSet("dismissed_notifications", current).apply()
    }
}
