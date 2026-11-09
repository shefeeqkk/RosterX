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

    private fun toProperCase(str: String): String {
        val clean = str.trim()
        if (clean.isBlank()) return clean
        return clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length == 1) word.uppercase()
                else word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    private fun loadSavedUser(): Engineer? {
        val json = prefs.getString("saved_user", null) ?: return null
        return try {
            val user = gson.fromJson(json, Engineer::class.java)
            if (user != null && user.name.isNotBlank()) {
                val proper = toProperCase(user.name)
                user.copy(name = proper, fullName = proper)
            } else {
                user
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setCurrentUser(user: Engineer) {
        val proper = toProperCase(user.name)
        val normalized = user.copy(name = proper, fullName = proper)
        _currentUser.value = normalized
        prefs.edit().putString("saved_user", gson.toJson(normalized)).apply()
    }

    fun updateCurrentUserProfilePicture(picture: String?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(profilePicture = picture)
        setCurrentUser(updated)
    }

    fun updateCurrentUserName(newName: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(name = newName)
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

    companion object {
        const val DEFAULT_PRODUCTION_URL = "https://api.rosterx.online/"
    }

    fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim().replace(",", "")
        if (clean.isBlank()) return DEFAULT_PRODUCTION_URL
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
        if (!saved.isNullOrBlank()) {
            val clean = sanitizeBaseUrl(saved)
            // Auto-migrate legacy onrender or trycloudflare URLs to official custom domain
            if (clean.contains("onrender.com") || clean.contains("trycloudflare.com")) {
                prefs.edit().putString("custom_base_url", DEFAULT_PRODUCTION_URL).apply()
                return DEFAULT_PRODUCTION_URL
            }
            return clean
        }

        // Automatically resolve synced internet tunnel URL from assets if present
        try {
            val assetUrl = context.assets.open("server_url.txt").bufferedReader().use { it.readText().trim() }
            if (assetUrl.isNotBlank()) {
                return sanitizeBaseUrl(assetUrl)
            }
        } catch (e: Exception) {
            // Assets file not present or not accessible; fall back to official domain
        }

        return DEFAULT_PRODUCTION_URL
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
