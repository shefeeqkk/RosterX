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
    val user: UserDto?,
    val token: String? = null
)

data class UserDto(
    val id: String?,
    val username: String?,
    val name: String?,
    val full_name: String?,
    val email: String?,
    val phone: String?,
    val role: String?,
    @SerializedName("profile_picture") val profilePicture: String? = null
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
    @SerializedName("resignation_date") val resignationDate: String?,
    @SerializedName("password") val password: String? = null,
    @SerializedName("original_username") val originalUsername: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("birthday") val birthday: String? = null
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
    @SerializedName("username") val username: String? = null,
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
    suspend fun syncEngineers(@Body payload: SyncEngineersRequest): PingResponse

    @POST("api/engineers/delete")
    suspend fun deleteEngineer(@Body payload: DeleteEngineerRequest): PingResponse

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

    @POST("api/notifications/dismiss")
    suspend fun dismissNotification(@Body body: DismissNotificationRequest): PingResponse

    @POST("api/chat/assistant")
    suspend fun queryAssistant(@Body request: AssistantChatRequest): AssistantChatResponse

    @POST("api/users/profile-picture")
    suspend fun updateProfilePicture(@Body request: UpdateProfilePictureRequest): UpdateProfilePictureResponse

    @POST("api/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UpdateProfileResponse

    @POST("api/users/profile")
    suspend fun updateProfileName(@Body request: UpdateProfileNameRequest): UpdateProfileNameResponse

    @POST("api/users/fcm-token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): PingResponse

    @GET("api/broadcast/templates")
    suspend fun getBroadcastTemplates(): BroadcastTemplatesResponse

    @POST("api/notifications/retract")
    suspend fun retractNotification(@Body body: Map<String, String>): PingResponse
}

data class UpdateProfilePictureRequest(
    @SerializedName("username") val username: String? = null,
    @SerializedName("profile_picture") val profilePicture: String?
)

data class UpdateProfilePictureResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("join_date") val joinDate: String? = null,
    @SerializedName("birthday") val birthday: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("current_password") val currentPassword: String? = null,
    @SerializedName("password") val password: String? = null
)

data class UpdateProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val name: String? = null,
    val user: EngineerDto? = null
)

data class UpdateProfileNameRequest(
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String? = null
)

data class UpdateProfileNameResponse(
    val success: Boolean,
    val message: String? = null,
    val name: String? = null,
    val user: EngineerDto? = null
)

data class SyncEngineersRequest(
    @SerializedName("engineers") val engineers: List<EngineerDto>
)

data class DeleteEngineerRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("engineer_id") val engineerId: String? = null
)

data class DismissNotificationRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("all") val all: Boolean? = null
)

// --- Notifications DTOs ---
data class SendNotificationRequest(
    val title: String,
    val message: String,
    val target: Any,
    val priority: String = "info",
    @SerializedName("send_email") val sendEmail: Boolean = false,
    @SerializedName("send_in_app") val sendInApp: Boolean = true,
    @SerializedName("send_push") val sendPush: Boolean = true,
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
    @SerializedName("push_dispatched") val pushDispatched: Int? = 0,
    @SerializedName("sample_personalized") val samplePersonalized: String? = null,
    @SerializedName("is_retracted") val isRetracted: Boolean? = false,
    @SerializedName("read_count") val readCount: Int? = 0,
    @SerializedName("pending_count") val pendingCount: Int? = 0,
    @SerializedName("read_by") val readBy: List<String>? = null,
    @SerializedName("pending") val pending: List<String>? = null,
    @SerializedName("celebration_type") val celebrationType: String? = null,
    @SerializedName("celebration_user") val celebrationUser: String? = null,
    @SerializedName("celebration_name") val celebrationName: String? = null,
    @SerializedName("celebration_years") val celebrationYears: Int? = null,
    @SerializedName("celebration_photo") val celebrationPhoto: String? = null
)

data class BroadcastTemplateDto(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val message: String? = null,
    val priority: String? = null,
    @SerializedName("target_default") val targetDefault: String? = null
)

data class BroadcastTemplatesResponse(
    val success: Boolean,
    val templates: List<BroadcastTemplateDto>? = null
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
    val notifications: List<NotificationDto>? = null,
    @SerializedName("dismissed_notification_ids") val dismissedNotificationIds: List<String>? = null
)

// --- AI Operations Assistant DTOs ---
data class AssistantChatRequest(
    val message: String,
    val current_user: Map<String, Any?>? = null,
    val platform: String = "Android App"
)

data class AssistantChatResponse(
    val success: Boolean,
    val reply: String?,
    val model: String? = null,
    @SerializedName("grounded_time") val groundedTime: String? = null,
    val message: String? = null
)
