package com.rosterx.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// --- Auth DTOs ---

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val user: UserDto?
)

data class UserDto(
    val id: String?,
    val username: String?,
    val name: String?,
    val full_name: String?,
    val email: String?,
    val phone: String?,
    val role: String?
)

data class SendOtpRequest(
    val identifier: String
)

data class SendOtpResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("delivered_to") val deliveredTo: String?
)

data class VerifyOtpRequest(
    val identifier: String,
    val otp: String,
    val newPassword: String
)

data class VerifyOtpResponse(
    val success: Boolean,
    val message: String?
)

// --- Server DTOs ---

data class PingResponse(
    val success: Boolean,
    val message: String?,
    val status: String?,
    val cluster: String? = null,
    val database: String? = null,
    @SerializedName("latency_ms") val latencyMs: Long? = null,
    @SerializedName("public_url") val publicUrl: String? = null,
    @SerializedName("api_url") val apiUrl: String? = null,
    @SerializedName("lan_url") val lanUrl: String? = null,
    val logs: List<String>? = null
)

data class ServerLogsResponse(
    val success: Boolean,
    val logs: List<String>?,
    val count: Int? = null,
    val cluster: String? = null,
    val database: String? = null
)

data class EngineersResponse(
    val success: Boolean,
    @SerializedName("documents") val documents: List<EngineerDto>?,
    @SerializedName("engineers") val engineers: List<EngineerDto>?
)

data class EngineerDto(
    @SerializedName("id") val id: String?,
    @SerializedName("engineer_id") val engineerId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("default_weekly_offs") val defaultWeeklyOffs: List<String>?,
    @SerializedName("join_date") val joinDate: String?,
    @SerializedName("resignation_date") val resignationDate: String?
)

data class RosterResponse(
    val success: Boolean,
    @SerializedName("document") val document: RosterDocDto?,
    @SerializedName("data") val data: RosterDocDto?
)

data class RosterDocDto(
    @SerializedName("_id") val id: String,
    @SerializedName("month") val month: Int?,
    @SerializedName("year") val year: Int?,
    @SerializedName("engineers") val engineers: List<EngineerRosterDto>?
)

data class EngineerRosterDto(
    @SerializedName("name") val name: String?,
    @SerializedName("engineer_id") val engineerId: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("shifts") val shifts: JsonElement?
)

data class RosterBackupDto(
    @SerializedName("backup_id") val backupId: String,
    @SerializedName("month_id") val monthId: String,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("created_at_epoch") val createdAtEpoch: Double?,
    @SerializedName("published_by") val publishedBy: String?,
    @SerializedName("total_engineers") val totalEngineers: Int?
)

data class RosterBackupsResponse(
    val success: Boolean,
    @SerializedName("month_id") val monthId: String?,
    @SerializedName("backups") val backups: List<RosterBackupDto>?,
    val count: Int?
)

data class RestoreRosterRequest(
    @SerializedName("backup_id") val backupId: String,
    @SerializedName("month_id") val monthId: String
)

data class RestoreRosterResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("month_id") val monthId: String?,
    @SerializedName("document") val document: RosterDocDto?
)

interface RosterApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    @GET("api/test-connection")
    suspend fun pingServer(): PingResponse

    @GET("api/logs")
    suspend fun getServerLogs(): ServerLogsResponse

    @GET("api/engineers")
    suspend fun getEngineers(): EngineersResponse

    @POST("api/engineers")
    suspend fun syncEngineers(@Body payload: Map<String, List<EngineerDto>>): PingResponse

    @POST("api/engineers/delete")
    suspend fun deleteEngineer(@Body payload: Map<String, String>): PingResponse

    @GET("api/roster/{monthKey}")
    suspend fun getRoster(@Path("monthKey") monthKey: String): RosterResponse

    @POST("api/roster")
    suspend fun saveRoster(@Body payload: RosterDocDto): PingResponse

    @GET("api/roster-backups/{monthKey}")
    suspend fun getRosterBackups(@Path("monthKey") monthKey: String): RosterBackupsResponse

    @POST("api/roster-backups/restore")
    suspend fun restoreRosterBackup(@Body request: RestoreRosterRequest): RestoreRosterResponse

    @GET("api/constraints")
    suspend fun getConstraints(): com.rosterx.domain.model.ConstraintsResponse

    @POST("api/constraints")
    suspend fun saveConstraints(@Body constraints: com.rosterx.domain.model.OperationalConstraints): com.rosterx.domain.model.ConstraintsResponse

    @GET("api/notifications")
    suspend fun getNotifications(): NotificationsListResponse

    @POST("api/notifications/send")
    suspend fun sendNotification(@Body request: SendNotificationRequest): SendNotificationResponse
}

// --- Notifications DTOs ---

data class SendNotificationRequest(
    val title: String,
    val message: String,
    val target: Any,
    val priority: String = "info",
    @SerializedName("send_email") val sendEmail: Boolean = false,
    @SerializedName("send_in_app") val sendInApp: Boolean = true,
    val sender: String = "System Administrator"
)

data class NotificationDto(
    val id: String?,
    val title: String?,
    @SerializedName("raw_message") val rawMessage: String?,
    @SerializedName("target_type") val targetType: String?,
    val priority: String?,
    val sender: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("recipients_count") val recipientsCount: Int? = 0,
    @SerializedName("emails_dispatched") val emailsDispatched: Int? = 0,
    @SerializedName("sample_personalized") val samplePersonalized: String? = null
)

data class SendNotificationResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("recipients_count") val recipientsCount: Int? = 0,
    @SerializedName("emails_dispatched") val emailsDispatched: Int? = 0,
    val notification: NotificationDto? = null
)

data class NotificationsListResponse(
    val success: Boolean,
    val count: Int? = 0,
    val notifications: List<NotificationDto>? = null
)


