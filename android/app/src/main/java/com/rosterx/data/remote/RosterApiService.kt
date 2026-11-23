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
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("join_date") val joinDate: String? = null,
    @SerializedName("birthday") val birthday: String? = null,
    @SerializedName("default_weekly_offs") val defaultWeeklyOffs: List<String>? = null
)

data class CurrentUserResponse(
    val success: Boolean,
    val user: UserDto? = null,
    val message: String? = null
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

// --- Telemetry DTOs ---
data class AtlasStorageDto(
    @SerializedName("storage_size_bytes") val storageSizeBytes: Long = 0L,
    @SerializedName("data_size_bytes") val dataSizeBytes: Long = 0L,
    @SerializedName("index_size_bytes") val indexSizeBytes: Long = 0L,
    @SerializedName("total_size_bytes") val totalSizeBytes: Long = 0L,
    @SerializedName("storage_size_formatted") val storageSizeFormatted: String = "0 KB",
    @SerializedName("data_size_formatted") val dataSizeFormatted: String = "0 KB",
    @SerializedName("index_size_formatted") val indexSizeFormatted: String = "0 KB",
    @SerializedName("total_size_formatted") val totalSizeFormatted: String = "0 KB",
    @SerializedName("objects_count") val objectsCount: Int = 0,
    @SerializedName("collections_count") val collectionsCount: Int = 0
)

data class AtlasTelemetryDto(
    val status: String = "DISCONNECTED",
    val cluster: String = "Cluster0",
    val database: String = "rosterx",
    @SerializedName("ping_ms") val pingMs: Long = 0L,
    val collections: Map<String, Int> = emptyMap(),
    val storage: AtlasStorageDto = AtlasStorageDto()
)

data class RenderDeploymentDto(
    @SerializedName("service_name") val serviceName: String = "rosterx-backend",
    val status: String = "Healthy (Live)",
    @SerializedName("deployed_url") val deployedUrl: String = "https://rosterx-vc30.onrender.com",
    @SerializedName("custom_domain") val customDomain: String = "https://api.rosterx.online",
    @SerializedName("is_render_cloud") val isRenderCloud: Boolean = true,
    @SerializedName("ssl_active") val sslActive: Boolean = true
)

data class EnvironmentTelemetryDto(
    @SerializedName("python_version") val pythonVersion: String = "",
    @SerializedName("os_platform") val osPlatform: String = "",
    @SerializedName("is_render") val isRender: Boolean = false
)

data class NetworkTelemetryDto(
    val port: Int = 5000,
    @SerializedName("lan_ip") val lanIp: String = "",
    @SerializedName("public_url") val publicUrl: String? = null,
    @SerializedName("tunnel_active") val tunnelActive: Boolean = false
)

data class ServerTelemetryDataDto(
    @SerializedName("uptime_seconds") val uptimeSeconds: Double = 0.0,
    @SerializedName("uptime_formatted") val uptimeFormatted: String = "0s",
    val environment: EnvironmentTelemetryDto = EnvironmentTelemetryDto(),
    val atlas: AtlasTelemetryDto = AtlasTelemetryDto(),
    val render: RenderDeploymentDto = RenderDeploymentDto(),
    @SerializedName("memory_rss_mb") val memoryRssMb: Double = 0.0,
    val network: NetworkTelemetryDto = NetworkTelemetryDto(),
    val version: String = "",
    val timestamp: Double = 0.0
)

data class ServerTelemetryResponse(
    val success: Boolean,
    val data: ServerTelemetryDataDto?
)

data class ServerInfoResponse(
    val success: Boolean = true,
    val version: String? = null,
    @SerializedName("latest_android_version") val latestAndroidVersion: String? = null,
    @SerializedName("min_supported_version") val minSupportedVersion: String? = null,
    @SerializedName("apk_download_url") val apkDownloadUrl: String? = null
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
    @SerializedName("full_name") val fullNameSnake: String? = null,
    @SerializedName("fullName") val fullNameCamel: String? = null,
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
    @SerializedName("birthday") val birthday: String? = null,
    @SerializedName("include_in_roster") val includeInRoster: Boolean? = null,
    @SerializedName("display_order") val displayOrder: Int? = null,
    @SerializedName("swap_code") val swapCode: String? = null
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

    @GET("api/auth/me")
    suspend fun getMe(): CurrentUserResponse

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    @GET("api/server-info")
    suspend fun getServerInfo(): ServerInfoResponse

    @GET("api/test-connection")
    suspend fun pingServer(): PingResponse

    @GET("api/telemetry/server")
    suspend fun getServerTelemetry(): ServerTelemetryResponse

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

    @POST("api/users/fcm-token/unregister")
    suspend fun unregisterFcmToken(@Body body: Map<String, String>): PingResponse

    @GET("api/broadcast/templates")
    suspend fun getBroadcastTemplates(): BroadcastTemplatesResponse

    @POST("api/broadcast/templates")
    suspend fun saveBroadcastTemplate(@Body body: Map<String, String>): PingResponse

    @POST("api/broadcast/templates/delete")
    suspend fun deleteBroadcastTemplate(@Body body: Map<String, String>): PingResponse

    @POST("api/notifications/retract")
    suspend fun retractNotification(@Body body: Map<String, String>): PingResponse

    @GET("api/credentials/trackers")
    suspend fun getCredentialTrackers(): CredentialTrackersResponse

    @POST("api/credentials/trackers")
    suspend fun createCredentialTracker(@Body request: CreateCredentialTrackerRequest): CredentialTrackerOperationResponse

    @POST("api/credentials/trackers/{id}")
    suspend fun updateCredentialTracker(
        @Path("id") id: String,
        @Body request: UpdateCredentialTrackerRequest
    ): CredentialTrackerOperationResponse

    @POST("api/credentials/trackers/{id}/renew")
    suspend fun renewCredentialTracker(
        @Path("id") id: String,
        @Body request: RenewCredentialTrackerRequest = RenewCredentialTrackerRequest()
    ): CredentialTrackerOperationResponse

    @POST("api/credentials/trackers/{id}/delete")
    suspend fun deleteCredentialTracker(@Path("id") id: String): CredentialTrackerOperationResponse

    @GET("api/credentials/trackers/logs")
    suspend fun getCredentialTrackerLogs(): CredentialTrackerLogsResponse

    @GET("api/celebrations/today")
    suspend fun getTodayCelebrations(): TodayCelebrationsResponse

    @POST("api/users/calendar-sync")
    suspend fun saveCalendarSyncStatus(@Body body: CalendarSyncRequest): PingResponse

    @POST("api/users/calendar-sync/clear")
    suspend fun clearCalendarSyncStatus(@Body body: CalendarSyncClearRequest): PingResponse

    @POST("api/users/calendar-accounts")
    suspend fun saveCalendarAccounts(@Body body: Map<String, @JvmSuppressWildcards List<Map<String, Any?>>>): PingResponse

    @GET("api/leave/my-requests")
    suspend fun getMyLeaveRequests(): MyLeaveRequestsResponse

    @GET("api/leave/eligible-partners")
    suspend fun getEligibleLeavePartners(@retrofit2.http.Query("date") date: String): EligibleLeavePartnersResponse

    @GET("api/leave/admin-queue")
    suspend fun getAdminLeaveQueue(): AdminLeaveQueueResponse

    @POST("api/leave/apply")
    suspend fun applyLeaveRequest(@Body request: ApplyLeaveRequest): LeaveActionResponse

    @POST("api/leave/admin-action")
    suspend fun submitAdminLeaveAction(@Body request: AdminLeaveActionRequest): LeaveActionResponse

    @POST("api/leave/cancel")
    suspend fun cancelLeaveRequest(@Body request: CancelLeaveRequest): LeaveActionResponse

    @GET("api/leave/settings")
    suspend fun getLeaveSettings(): LeaveSettingsResponse

    @POST("api/leave/settings")
    suspend fun updateLeaveSettings(@Body request: UpdateLeaveSettingsRequest): LeaveActionResponse
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
    @SerializedName("notification_id") val notificationId: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("all") val all: Boolean? = null
)

data class CalendarSyncRequest(
    @SerializedName("month") val month: String,
    @SerializedName("count") val count: Int,
    @SerializedName("target") val target: String,
    @SerializedName("synced") val synced: Boolean = true
)

data class CalendarSyncClearRequest(
    @SerializedName("month") val month: String? = null,
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
    val sender: String? = null
)

data class ChannelDeliveryDto(
    val enabled: Boolean? = true,
    @SerializedName("sent_count") val sentCount: Int? = 0,
    val status: String? = "Delivered"
)

data class NotificationDto(
    val id: String?,
    val title: String?,
    @SerializedName("raw_message") val rawMessage: String?,
    @SerializedName("target_type") val targetType: String?,
    val priority: String?,
    val category: String? = null,
    val sender: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("timestamp") val timestamp: Double? = null,
    @SerializedName("recipients_count") val recipientsCount: Int? = 0,
    @SerializedName("emails_dispatched") val emailsDispatched: Int? = 0,
    @SerializedName("push_dispatched") val pushDispatched: Int? = 0,
    @SerializedName("fcm_delivery") val fcmDelivery: ChannelDeliveryDto? = null,
    @SerializedName("inapp_delivery") val inappDelivery: ChannelDeliveryDto? = null,
    @SerializedName("email_delivery") val emailDelivery: ChannelDeliveryDto? = null,
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
    @SerializedName("celebration_photo") val celebrationPhoto: String? = null,
    @SerializedName("celebration_phone") val celebrationPhone: String? = null
)

fun NotificationDto.isExpiredCelebration(): Boolean {
    val isCelebrationNotif = !celebrationType.isNullOrBlank() ||
        sender?.contains("Celebration", ignoreCase = true) == true ||
        category.equals("celebration", ignoreCase = true) ||
        title?.contains("Birthday", ignoreCase = true) == true ||
        title?.contains("Anniversary", ignoreCase = true) == true

    if (!isCelebrationNotif) return false

    val istZone = java.time.ZoneId.of("Asia/Kolkata")
    val todayIst = java.time.LocalDate.now(istZone)

    val notifDate: java.time.LocalDate? = try {
        val ts = timestamp
        if (ts != null && ts > 0.0) {
            java.time.Instant.ofEpochMilli((ts * 1000).toLong()).atZone(istZone).toLocalDate()
        } else null
    } catch (_: Exception) {
        null
    } ?: createdAt?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
        try {
            val utcCandidate = raw.removeSuffix(" UTC").trim()
            val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            java.time.LocalDateTime.parse(utcCandidate, fmt)
                .atZone(java.time.ZoneOffset.UTC)
                .withZoneSameInstant(istZone)
                .toLocalDate()
        } catch (_: Exception) {
            try {
                java.time.Instant.parse(raw).atZone(istZone).toLocalDate()
            } catch (_: Exception) {
                try {
                    java.time.LocalDate.parse(raw.take(10))
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    return notifDate != null && notifDate.isBefore(todayIst)
}

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
    val platform: String = "Android App",
    val history: List<Map<String, String>>? = null,
    val target_month: String? = null
)

data class AssistantChatResponse(
    val success: Boolean,
    val reply: String?,
    val model: String? = null,
    @SerializedName("grounded_time") val groundedTime: String? = null,
    val message: String? = null,
    val error: String? = null
)

// --- Credential Tracker DTOs ---
data class CredentialTrackerDto(
    val id: String,
    @SerializedName("server_name") val serverName: String,
    val username: String,
    @SerializedName("last_updated_date") val lastUpdatedDate: String,
    @SerializedName("validity_days") val validityDays: Int,
    @SerializedName("alert_threshold_days") val alertThresholdDays: Int? = 5,
    @SerializedName("expiry_date") val expiryDate: String,
    @SerializedName("alarm_start_date") val alarmStartDate: String? = null,
    @SerializedName("days_remaining") val daysRemaining: Int,
    val status: String, // "HEALTHY", "EXPIRING_SOON", "EXPIRES_TODAY", "EXPIRED"
    @SerializedName("spokesperson_emails") val spokespersonEmails: List<String> = emptyList(),
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null,
    @SerializedName("updated_by") val updatedBy: String? = null
)

data class CredentialTrackersDataDto(
    val trackers: List<CredentialTrackerDto> = emptyList(),
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("expiring_soon_count") val expiringSoonCount: Int = 0,
    @SerializedName("expired_count") val expiredCount: Int = 0
)

data class CredentialTrackersResponse(
    val success: Boolean,
    val data: CredentialTrackersDataDto? = null,
    val message: String? = null
)

data class CreateCredentialTrackerRequest(
    @SerializedName("server_name") val serverName: String,
    val username: String,
    @SerializedName("last_updated_date") val lastUpdatedDate: String,
    @SerializedName("validity_days") val validityDays: Int,
    @SerializedName("alert_threshold_days") val alertThresholdDays: Int? = 5,
    @SerializedName("spokesperson_emails") val spokespersonEmails: List<String>
)

data class UpdateCredentialTrackerRequest(
    @SerializedName("server_name") val serverName: String? = null,
    val username: String? = null,
    @SerializedName("last_updated_date") val lastUpdatedDate: String? = null,
    @SerializedName("validity_days") val validityDays: Int? = null,
    @SerializedName("alert_threshold_days") val alertThresholdDays: Int? = null,
    @SerializedName("spokesperson_emails") val spokespersonEmails: List<String>? = null
)

data class RenewCredentialTrackerRequest(
    @SerializedName("last_updated_date") val lastUpdatedDate: String? = null,
    @SerializedName("alert_threshold_days") val alertThresholdDays: Int? = null
)

data class CredentialTrackerOperationResponse(
    val success: Boolean,
    val message: String? = null,
    val tracker: CredentialTrackerDto? = null
)

data class CredentialTrackerLogDto(
    val id: String = "",
    @SerializedName("tracker_id") val trackerId: String = "",
    @SerializedName("server_name") val serverName: String = "",
    val username: String = "",
    val action: String = "",
    @SerializedName("action_label") val actionLabel: String = "",
    @SerializedName("updated_by") val updatedBy: String = "",
    @SerializedName("updated_by_name") val updatedByName: String = "",
    @SerializedName("previous_date") val previousDate: String? = null,
    @SerializedName("new_date") val newDate: String? = null,
    @SerializedName("validity_days") val validityDays: Int? = null,
    @SerializedName("expiry_date") val expiryDate: String? = null,
    val summary: String = "",
    val timestamp: Long = 0L,
    @SerializedName("datetime_str") val datetimeStr: String = ""
)

data class CredentialTrackerLogsResponse(
    val success: Boolean = false,
    val logs: List<CredentialTrackerLogDto> = emptyList(),
    val count: Int = 0,
    val message: String? = null
)

data class TodayCelebrationDto(
    val username: String,
    val name: String,
    val type: String, // "birthday", "anniversary", "both"
    val years: Int = 0,
    val photo: String? = null,
    val phone: String? = null,
    val title: String,
    @SerializedName("greeting_text") val greetingText: String
)

data class TodayCelebrationsResponse(
    val success: Boolean,
    val today: String? = null,
    val celebrations: List<TodayCelebrationDto> = emptyList(),
    val count: Int = 0
)

// --- Leave & Swap Request DTOs ---

data class ApplyLeaveRequest(
    val dates: List<String>,
    val reason: String = "",
    @SerializedName("preferred_partner") val preferredPartner: String = ""
)

data class AdminLeaveActionRequest(
    @SerializedName("request_id") val requestId: String,
    val action: String, // "swap" | "leave" | "reject"
    @SerializedName("swap_partner") val swapPartner: String = "",
    @SerializedName("swap_code") val swapCode: String = "",
    @SerializedName("admin_note") val adminNote: String = ""
)

data class LeaveActionResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("request_id") val requestId: String? = null
)

data class LeaveApprovalDto(
    @SerializedName("approved_by") val approvedBy: String? = null,
    @SerializedName("approved_by_name", alternate = ["approved_name"]) val approvedByName: String? = null,
    @SerializedName("approved_at") val approvedAt: String? = null,
    @SerializedName("action_type") val actionType: String? = null,
    @SerializedName("swap_partner") val swapPartner: String? = null,
    @SerializedName("swap_shift_code") val swapShiftCode: String? = null,
    @SerializedName("admin_note") val adminNote: String? = null,
    @SerializedName("revoked_by") val revokedBy: String? = null,
    @SerializedName("revoked_name") val revokedName: String? = null,
    @SerializedName("revoked_at") val revokedAt: String? = null
)

data class LeaveRequestDto(
    @SerializedName("request_id") val requestId: String = "",
    @SerializedName("engineer_id") val engineerId: String = "",
    @SerializedName("engineer_name") val engineerName: String = "",
    val dates: List<String> = emptyList(),
    @SerializedName("month_keys") val monthKeys: List<String> = emptyList(),
    val reason: String = "",
    @SerializedName("preferred_partner") val preferredPartner: String = "",
    @SerializedName("annual_year") val annualYear: Int = 2026,
    val daysCount: Int = 0,
    val status: String = "pending",
    @SerializedName("created_at") val createdAt: String = "",
    val approval: LeaveApprovalDto? = null,
    @SerializedName("cancelled_at") val cancelledAt: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null
)

data class MyLeaveRequestsResponse(
    val success: Boolean,
    val requests: List<LeaveRequestDto> = emptyList(),
    @SerializedName("active_count") val activeCount: Int = 0,
    @SerializedName("approved_leave_days") val approvedLeaveDays: Int = 0,
    @SerializedName("remaining_leave_balance") val remainingLeaveBalance: Int? = null,
    @SerializedName("annual_quota") val annualQuota: Int? = null,
    @SerializedName("annual_year") val annualYear: Int = 2026,
    @SerializedName("capping_enabled") val cappingEnabled: Boolean = true,
    val message: String? = null
)

data class EligiblePartnerDto(
    val username: String = "",
    val name: String = "",
    @SerializedName("profile_picture") val profilePicture: String? = null,
    val shift: String = "",
    @SerializedName("is_off") val isOff: Boolean = false
)

data class EligibleLeavePartnersResponse(
    val success: Boolean,
    val date: String? = null,
    val partners: List<EligiblePartnerDto> = emptyList(),
    val count: Int = 0,
    val message: String? = null
)

data class AdminLeaveQueueResponse(
    val success: Boolean,
    @SerializedName("pending_requests") val pendingRequests: List<LeaveRequestDto>? = emptyList(),
    @SerializedName("historical_requests") val historicalRequests: List<LeaveRequestDto>? = emptyList(),
    @SerializedName("requests") val allRequests: List<LeaveRequestDto>? = emptyList(),
    @SerializedName("pending_count") val pendingCount: Int = 0,
    @SerializedName("history_count") val historyCount: Int = 0,
    val message: String? = null
)

data class CancelLeaveRequest(
    @SerializedName("request_id") val requestId: String
)

data class LeaveSettingsResponse(
    val success: Boolean,
    @SerializedName("capping_enabled") val cappingEnabled: Boolean = true,
    @SerializedName("annual_quota") val annualQuota: Int = 14,
    val message: String? = null
)

data class UpdateLeaveSettingsRequest(
    @SerializedName("capping_enabled") val cappingEnabled: Boolean,
    @SerializedName("annual_quota") val annualQuota: Int
)
