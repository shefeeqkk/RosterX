package com.rosterx.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePasswordManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isGooglePasswordManagerEnabled(): Boolean {
        return prefs.getBoolean(KEY_GPM_ENABLED, true)
    }

    fun setGooglePasswordManagerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GPM_ENABLED, enabled).apply()
    }

    private fun hashCredentials(username: String, password: String): String {
        val input = "${username.trim().lowercase()}:$password"
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isCredentialsAlreadySaved(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        val key = "last_saved_hash_${username.trim().lowercase()}"
        val savedHash = prefs.getString(key, null)
        val currentHash = hashCredentials(username, password)
        return savedHash == currentHash
    }

    fun recordSavedCredentials(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return
        val key = "last_saved_hash_${username.trim().lowercase()}"
        prefs.edit().putString(key, hashCredentials(username, password)).apply()
        Log.d(TAG, "Google Password Manager: Recorded credential cache for '${username.trim().lowercase()}'.")
    }

    /**
     * Triggers the native Google Password Manager bottom sheet prompt
     * asking the user: "Save password to Google Password Manager?"
     */
    suspend fun saveCredentials(
        activity: Activity,
        username: String,
        password: String
    ): Boolean = withContext(Dispatchers.Main) {
        if (!isGooglePasswordManagerEnabled() || username.isBlank() || password.isBlank()) {
            Log.d(TAG, "Google Password Manager: Save skipped (enabled=${isGooglePasswordManagerEnabled()}, user=${username.isNotBlank()})")
            return@withContext false
        }
        if (isCredentialsAlreadySaved(username, password)) {
            Log.i(TAG, "Google Password Manager: Credentials for '$username' already saved and unchanged. Skipping save prompt.")
            return@withContext true
        }
        try {
            Log.i(TAG, "Google Password Manager: Launching CreatePasswordRequest for '$username'...")
            val credentialManager = CredentialManager.create(activity)
            val request = CreatePasswordRequest(id = username.trim(), password = password)
            val response = credentialManager.createCredential(activity, request)
            Log.i(TAG, "Google Password Manager: Successfully stored credentials for $username! (response=$response)")
            recordSavedCredentials(username, password)
            true
        } catch (e: CreateCredentialCancellationException) {
            // User dismissed or tapped "Not now" - mark as handled so we don't nag repeatedly for the same password
            Log.i(TAG, "Google Password Manager: User dismissed save prompt. Recording cache to respect choice.")
            recordSavedCredentials(username, password)
            false
        } catch (e: CreateCredentialException) {
            Log.w(TAG, "Google Password Manager: Save failed (${e.type}): ${e.message}", e)
            false
        } catch (e: Throwable) {
            Log.w(TAG, "Google Password Manager: Unexpected error saving credentials: ${e.message}", e)
            false
        }
    }

    /**
     * Prompts Google Password Manager 1-Tap bottom sheet to retrieve saved credentials
     */
    suspend fun getSavedCredentials(
        activity: Activity,
        onDismissed: () -> Unit = {},
        onCredentialsRetrieved: (username: String, password: String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        if (!isGooglePasswordManagerEnabled()) {
            onDismissed()
            return@withContext false
        }
        try {
            val credentialManager = CredentialManager.create(activity)
            val passwordOption = GetPasswordOption()
            val getCredRequest = GetCredentialRequest(listOf(passwordOption))
            val result = credentialManager.getCredential(activity, getCredRequest)
            val credential = result.credential
            if (credential is PasswordCredential) {
                val username = credential.id
                val password = credential.password
                Log.d(TAG, "Google Password Manager: Retrieved credentials for $username")
                recordSavedCredentials(username, password)
                onCredentialsRetrieved(username, password)
                return@withContext true
            }
            onDismissed()
            false
        } catch (e: GetCredentialCancellationException) {
            // User dismissed the 1-tap sheet - continue normally and signal caller
            Log.d(TAG, "Google Password Manager: 1-Tap retrieval dismissed by user.")
            onDismissed()
            false
        } catch (e: GetCredentialException) {
            Log.d(TAG, "Google Password Manager: No saved credentials found or unavailable: ${e.message}")
            onDismissed()
            false
        } catch (e: Throwable) {
            Log.w(TAG, "Google Password Manager: Error during credential retrieval: ${e.message}")
            onDismissed()
            false
        }
    }

    companion object {
        private const val TAG = "RosterX_GPM"
        private const val PREFS_NAME = "rosterx_gpm_prefs"
        private const val KEY_GPM_ENABLED = "gpm_autofill_enabled"
    }
}
