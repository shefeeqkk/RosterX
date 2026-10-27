package com.rosterx.domain.repository

import com.rosterx.domain.model.Engineer

data class ServerDiagnostics(
    val isConnected: Boolean,
    val cluster: String,
    val database: String,
    val latencyMs: Long,
    val publicUrl: String? = null,
    val apiUrl: String? = null,
    val lanUrl: String? = null,
    val logs: List<String> = emptyList()
)

data class RosterBackup(
    val backupId: String,
    val monthId: String,
    val timestamp: String,
    val createdAtEpoch: Double,
    val publishedBy: String,
    val totalEngineers: Int
)

interface RosterRepository {
    suspend fun login(identifier: String, password: String): Result<Engineer>
    suspend fun sendOtp(identifier: String): Result<String>
    suspend fun verifyOtp(identifier: String, otp: String, newPassword: String): Result<String>
    suspend fun pingServer(): Boolean
    suspend fun getServerDiagnostics(): ServerDiagnostics
    suspend fun getServerLogs(): List<String>
    suspend fun getEngineers(): Result<List<Engineer>>
    suspend fun syncEngineers(engineers: List<Engineer>): Result<Unit>
    suspend fun deleteEngineer(id: String, name: String, username: String): Result<Unit>
    
    // monthKey format: "2024-11"
    suspend fun getRoster(monthKey: String): Result<Map<String, Map<String, String>>>
    
    suspend fun saveRoster(
        monthKey: String,
        shifts: Map<String, Map<String, String>>
    ): Result<Unit>

    suspend fun getRosterBackups(monthKey: String): Result<List<RosterBackup>>
    suspend fun restoreRosterBackup(backupId: String, monthKey: String): Result<Unit>

    suspend fun getConstraints(): Result<com.rosterx.domain.model.OperationalConstraints>
    suspend fun saveConstraints(constraints: com.rosterx.domain.model.OperationalConstraints): Result<Unit>

    suspend fun getNotifications(): Result<List<com.rosterx.data.remote.NotificationDto>>
    suspend fun sendNotification(
        title: String,
        message: String,
        target: Any,
        priority: String,
        sendEmail: Boolean,
        sendInApp: Boolean = true
    ): Result<com.rosterx.data.remote.SendNotificationResponse>
}


