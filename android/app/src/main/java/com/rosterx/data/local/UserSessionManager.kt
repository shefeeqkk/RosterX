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

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("rosterx_user_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<Engineer?>(loadSavedUser())
    val currentUser: StateFlow<Engineer?> = _currentUser.asStateFlow()

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _themePaletteId = MutableStateFlow(loadSavedThemePaletteId())
    val themePaletteId: StateFlow<String> = _themePaletteId.asStateFlow()

    private fun loadSavedThemeMode(): AppThemeMode {
        val modeStr = prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM"
        return try {
            AppThemeMode.valueOf(modeStr)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun loadSavedThemePaletteId(): String {
        return prefs.getString("app_theme_palette_id", "tory_blue") ?: "tory_blue"
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun setThemePaletteId(paletteId: String) {
        _themePaletteId.value = paletteId
        prefs.edit().putString("app_theme_palette_id", paletteId).apply()
    }

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

    fun updateCurrentUserProfilePicture(picture: String?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(profilePicture = picture)
        setCurrentUser(updated)
    }

    fun getAuthToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun getToken(): String? {
        return getAuthToken()
    }

    fun setAuthToken(token: String?) {
        if (!token.isNullOrBlank()) {
            prefs.edit().putString("auth_token", token).apply()
        } else {
            prefs.edit().remove("auth_token").apply()
        }
    }

    fun clearSession() {
        _currentUser.value = null
        prefs.edit()
            .remove("saved_user")
            .remove("auth_token")
            .apply()
    }

    fun hasSavedSession(): Boolean {
        return loadSavedUser() != null
    }

    fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (clean.isBlank()) return "https://rosterx-vc30.onrender.com/"
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean
        }
        while (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        while (clean.endsWith("/api")) {
            clean = clean.dropLast(4)
        }
        return clean + "/"
    }

    fun getBaseUrl(): String {
        val saved = prefs.getString("custom_base_url", null)
        if (!saved.isNullOrBlank()) return sanitizeBaseUrl(saved)

        // Automatically resolve synced internet tunnel URL from assets if present
        try {
            val assetUrl = context.assets.open("server_url.txt").bufferedReader().use { it.readText().trim() }
            if (assetUrl.isNotBlank()) {
                return sanitizeBaseUrl(assetUrl)
            }
        } catch (e: Exception) {
            // Assets file not present or not accessible; fall back to Render URL
        }

        return "https://rosterx-vc30.onrender.com/"
    }

    fun setBaseUrl(url: String) {
        val cleanUrl = sanitizeBaseUrl(url)
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
