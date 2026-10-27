package com.rosterx.data.repository

import com.google.gson.Gson
import com.rosterx.data.remote.*
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.domain.repository.RosterRepository
import com.rosterx.domain.repository.ServerDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosterRepositoryImpl @Inject constructor(
    private val apiService: RosterApiService
) : RosterRepository {

    private val gson = Gson()

    override suspend fun login(identifier: String, password: String): Result<Engineer> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(identifier, password))
            if (response.success && response.user != null) {
                val eng = Engineer(
                    id = response.user.id ?: "",
                    name = response.user.name ?: "",
                    fullName = response.user.full_name ?: response.user.name ?: "",
                    email = response.user.email ?: "",
                    username = response.user.username ?: "",
                    phone = response.user.phone ?: "",
                    role = try { EngineerRole.valueOf((response.user.role ?: "ENGINEER").uppercase()) } catch (e: Exception) { EngineerRole.ENGINEER },
                    status = EngineerStatus.ACTIVE,
                    defaultWeeklyOffs = emptyList(),
                    joinDate = ""
                )
                Result.success(eng)
            } else {
                Result.failure(Exception(response.message ?: "Invalid credentials"))
            }
        } catch (e: HttpException) {
            val errorMsg = parseErrorMessage(e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendOtp(identifier: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendOtp(SendOtpRequest(identifier))
            if (response.success) {
                val dest = response.deliveredTo ?: "registered email"
                Result.success("Verification code sent to $dest")
            } else {
                Result.failure(Exception(response.message ?: "Failed to dispatch OTP"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(identifier: String, otp: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyOtp(VerifyOtpRequest(identifier, otp, newPassword))
            if (response.success) {
                Result.success(response.message ?: "Password reset successfully!")
            } else {
                Result.failure(Exception(response.message ?: "Invalid verification code"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pingServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.pingServer()
            response.success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getServerDiagnostics(): ServerDiagnostics = withContext(Dispatchers.IO) {
        try {
            val t0 = System.currentTimeMillis()
            val response = apiService.pingServer()
            val totalRoundtrip = System.currentTimeMillis() - t0
            val latency = response.latencyMs ?: totalRoundtrip
            ServerDiagnostics(
                isConnected = response.success,
                cluster = response.cluster ?: "Cluster0",
                database = response.database ?: "rosterx",
                latencyMs = latency,
                publicUrl = response.publicUrl,
                apiUrl = response.apiUrl,
                lanUrl = response.lanUrl,
                logs = response.logs ?: emptyList()
            )
        } catch (e: Exception) {
            ServerDiagnostics(
                isConnected = false,
                cluster = "Cluster0",
                database = "rosterx",
                latencyMs = 0L,
                logs = listOf("[ERROR] Connection check failed: ${e.localizedMessage ?: "Server unreachable"}")
            )
        }
    }

    override suspend fun getServerLogs(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getServerLogs()
            response.logs ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getEngineers(): Result<List<Engineer>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEngineers()
            val list = response.documents ?: response.engineers ?: emptyList()
            if (response.success || list.isNotEmpty()) {
                val domainEngineers = list.map { it.toDomain() }
                Result.success(domainEngineers)
            } else {
                Result.failure(Exception("Failed to load engineers from server"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncEngineers(engineers: List<Engineer>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dtoList = engineers.map { it.toDto() }
            val payload = mapOf("engineers" to dtoList)
            val response = apiService.syncEngineers(payload)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync engineers: ${response.message}"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEngineer(id: String, name: String, username: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("id" to id, "name" to name, "username" to username)
            val response = apiService.deleteEngineer(payload)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete engineer"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRoster(monthKey: String): Result<Map<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRoster(monthKey)
            val doc = response.document ?: response.data
            
            if (doc?.engineers != null) {
                val rosterMap = mutableMapOf<String, Map<String, String>>()
                
                doc.engineers.forEach { engRoster ->
                    val dayMap = mutableMapOf<String, String>()
                    
                    val shiftsElement = engRoster.shifts
                    if (shiftsElement != null) {
                        if (shiftsElement.isJsonArray) {
                            shiftsElement.asJsonArray.forEachIndexed { index, element ->
                                if (!element.isJsonNull) {
                                    dayMap[(index + 1).toString()] = element.asString
                                }
                            }
                        } else if (shiftsElement.isJsonObject) {
                            shiftsElement.asJsonObject.entrySet().forEach { entry ->
                                if (!entry.value.isJsonNull) {
                                    dayMap[entry.key] = entry.value.asString
                                }
                            }
                        }
                    }

                    engRoster.engineerId?.let { if (it.isNotBlank()) rosterMap[it] = dayMap }
                    engRoster.id?.let { if (it.isNotBlank()) rosterMap[it] = dayMap }
                    engRoster.name?.let { name ->
                        if (name.isNotBlank()) {
                            rosterMap[name] = dayMap
                            rosterMap[name.uppercase()] = dayMap
                            rosterMap[name.lowercase()] = dayMap
                            
                            val firstName = name.trim().split(" ")[0].lowercase()
                            rosterMap[firstName] = dayMap
                        }
                    }
                }
                
                Result.success(rosterMap)
            } else {
                Result.failure(Exception("Roster '$monthKey' not found in database"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveRoster(monthKey: String, shifts: Map<String, Map<String, String>>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val parts = monthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()

            val engineers = getEngineers().getOrNull() ?: emptyList()
            val engineerList = if (engineers.isNotEmpty()) {
                engineers.map { eng ->
                    val dayMap = shifts[eng.username]
                        ?: shifts[eng.id]
                        ?: shifts[eng.name]
                        ?: shifts[eng.name.uppercase()]
                        ?: shifts[eng.name.lowercase()]
                        ?: emptyMap()
                    EngineerRosterDto(
                        name = eng.name,
                        engineerId = eng.id,
                        id = eng.id,
                        shifts = gson.toJsonTree(dayMap)
                    )
                }
            } else {
                shifts.map { (engId, dayMap) ->
                    EngineerRosterDto(
                        name = null,
                        engineerId = engId,
                        id = engId,
                        shifts = gson.toJsonTree(dayMap)
                    )
                }
            }

            val payload = RosterDocDto(
                id = monthKey,
                year = year,
                month = month,
                engineers = engineerList
            )
            val response = apiService.saveRoster(payload)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save roster: ${response.message}"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRosterBackups(monthKey: String): Result<List<com.rosterx.domain.repository.RosterBackup>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRosterBackups(monthKey)
            if (response.success && response.backups != null) {
                val list = response.backups.map { dto ->
                    com.rosterx.domain.repository.RosterBackup(
                        backupId = dto.backupId,
                        monthId = dto.monthId,
                        timestamp = dto.timestamp ?: "Unknown",
                        createdAtEpoch = dto.createdAtEpoch ?: 0.0,
                        publishedBy = dto.publishedBy ?: "Admin",
                        totalEngineers = dto.totalEngineers ?: 0
                    )
                }
                Result.success(list)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreRosterBackup(backupId: String, monthKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.restoreRosterBackup(RestoreRosterRequest(backupId, monthKey))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Restore failed"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConstraints(): Result<com.rosterx.domain.model.OperationalConstraints> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConstraints()
            if (response.success && response.constraints != null) {
                Result.success(response.constraints)
            } else {
                Result.success(com.rosterx.domain.model.OperationalConstraints())
            }
        } catch (e: Exception) {
            Result.success(com.rosterx.domain.model.OperationalConstraints())
        }
    }

    override suspend fun saveConstraints(constraints: com.rosterx.domain.model.OperationalConstraints): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.saveConstraints(constraints)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to save operational policies"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotifications(): Result<List<com.rosterx.data.remote.NotificationDto>> {
        return try {
            val response = apiService.getNotifications()
            if (response.success) {
                Result.success(response.notifications ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendNotification(
        title: String,
        message: String,
        target: Any,
        priority: String,
        sendEmail: Boolean,
        sendInApp: Boolean
    ): Result<com.rosterx.data.remote.SendNotificationResponse> {
        return try {
            val req = com.rosterx.data.remote.SendNotificationRequest(
                title = title,
                message = message,
                target = target,
                priority = priority,
                sendEmail = sendEmail,
                sendInApp = sendInApp
            )
            val response = apiService.sendNotification(req)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Failed to dispatch notification"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    private fun parseErrorMessage(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val errorJson = gson.fromJson(errorBody, Map::class.java)
                (errorJson["message"] as? String) ?: e.message()
            } else {
                e.message()
            }
        } catch (ex: Exception) {
            e.message()
        }
    }

    // --- Mappers ---

    private fun EngineerDto.toDomain(): Engineer {
        val resolvedId = id ?: engineerId ?: ""
        val resolvedName = name ?: ""
        val resolvedUsername = username ?: if (resolvedName.isNotBlank()) resolvedName.split(" ")[0].lowercase() else resolvedId
        
        return Engineer(
            id = resolvedId,
            name = resolvedName,
            fullName = resolvedName,
            email = email ?: "",
            username = resolvedUsername,
            phone = phone ?: "", // Properly map phone number from database!
            role = try { EngineerRole.valueOf((role ?: "ENGINEER").uppercase()) } catch (e: Exception) { EngineerRole.ENGINEER },
            status = try { EngineerStatus.valueOf((status ?: "ACTIVE").uppercase()) } catch (e: Exception) { EngineerStatus.ACTIVE },
            defaultWeeklyOffs = defaultWeeklyOffs ?: emptyList(),
            joinDate = joinDate ?: "",
            resignationDate = resignationDate
        )
    }

    private fun Engineer.toDto(): EngineerDto {
        return EngineerDto(
            id = id,
            engineerId = id,
            name = name,
            username = username,
            email = email,
            phone = phone,
            role = role.name.lowercase(),
            status = status.name.lowercase(),
            defaultWeeklyOffs = defaultWeeklyOffs,
            joinDate = joinDate,
            resignationDate = resignationDate
        )
    }
}
