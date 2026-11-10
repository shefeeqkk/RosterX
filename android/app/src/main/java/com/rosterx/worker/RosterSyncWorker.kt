package com.rosterx.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.rosterx.data.remote.EngineerDto
import com.rosterx.data.remote.RosterApiService
import com.rosterx.data.remote.RosterDocDto
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.model.EngineerStatus
import com.rosterx.widget.RosterXAppWidgetProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RosterSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val baseUrl = getBaseUrl(context)
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val apiService = retrofit.create(RosterApiService::class.java)

            val today = LocalDate.now()
            val currentMonthKey = String.format(Locale.US, "%d-%02d", today.year, today.monthValue)
            val yesterday = today.minusDays(1)
            val yesterdayMonthKey = String.format(Locale.US, "%d-%02d", yesterday.year, yesterday.monthValue)

            val rosterResp = apiService.getRoster(currentMonthKey)
            val engineersResp = apiService.getEngineers()

            if (rosterResp.success && engineersResp.success) {
                val rawRoster = parseRosterMap(rosterResp.document ?: rosterResp.data)
                val rawEngineers = engineersResp.documents ?: engineersResp.engineers ?: emptyList()
                val engineers = rawEngineers.map { it.toDomain() }

                var yesterdayRoster = rawRoster
                if (yesterdayMonthKey != currentMonthKey) {
                    try {
                        val yResp = apiService.getRoster(yesterdayMonthKey)
                        if (yResp.success) {
                            yesterdayRoster = parseRosterMap(yResp.document ?: yResp.data)
                        }
                    } catch (_: Exception) {}
                }

                val sessionPrefs = context.getSharedPreferences("rosterx_user_session", Context.MODE_PRIVATE)
                val userJson = sessionPrefs.getString("saved_user", null)
                val gson = Gson()
                val user: Engineer? = if (!userJson.isNullOrBlank()) {
                    try { gson.fromJson(userJson, Engineer::class.java) } catch (_: Exception) { null }
                } else null

                if (user != null) {
                    // 1. Always save team Operations Radar duty roster cache for all users
                    RosterXAppWidgetProvider.saveManagerDutyRoster(
                        context = context,
                        managerName = user.fullName.takeIf { it.isNotBlank() } ?: user.name,
                        dateFormatted = today.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                        dutyList = emptyList(),
                        summaryText = "Coverage Updated",
                        fullRoster = rawRoster,
                        yesterdayRoster = yesterdayRoster,
                        engineers = engineers,
                        monthKey = currentMonthKey
                    )
                    RosterXAppWidgetProvider.recomputeManagerDutyList(context, today, LocalTime.now())

                    // 2. Always save individual shifts using flexible name matching
                    val engShifts = RosterXAppWidgetProvider.findEngineerShifts(user, rawRoster) ?: emptyMap()
                    if (engShifts.isNotEmpty()) {
                        RosterXAppWidgetProvider.saveWidgetData(
                            context = context,
                            engineerName = user.fullName.takeIf { it.isNotBlank() } ?: user.name,
                            shiftsMap = engShifts,
                            monthKey = currentMonthKey,
                            keepManagerMode = true
                        )
                    }

                    RosterXAppWidgetProvider.updateAllWidgets(context)
                }
            }
            Result.success()
        } catch (e: Throwable) {
            Log.w("RosterSyncWorker", "Background widget sync error: ${e.message}")
            Result.success()
        }
    }

    private fun getBaseUrl(ctx: Context): String {
        return try {
            val text = ctx.assets.open("server_url.txt").bufferedReader().use { it.readText().trim() }
            if (text.isNotBlank()) {
                if (text.endsWith("/")) text else "$text/"
            } else "https://api.rosterx.online/"
        } catch (_: Exception) {
            "https://api.rosterx.online/"
        }
    }

    private fun parseRosterMap(doc: RosterDocDto?): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, Map<String, String>>()
        doc?.engineers?.forEach { engRoster ->
            val dayMap = mutableMapOf<String, String>()
            val shiftsElem = engRoster.shifts
            if (shiftsElem != null && shiftsElem.isJsonObject) {
                shiftsElem.asJsonObject.entrySet().forEach { entry ->
                    if (!entry.value.isJsonNull) {
                        dayMap[entry.key] = entry.value.asString
                    }
                }
            }
            engRoster.engineerId?.let { if (it.isNotBlank()) result[it] = dayMap }
            engRoster.id?.let { if (it.isNotBlank()) result[it] = dayMap }
            engRoster.username?.let { if (it.isNotBlank()) {
                result[it] = dayMap
                result[it.lowercase()] = dayMap
            }}
            engRoster.name?.let { name ->
                if (name.isNotBlank()) {
                    result[name] = dayMap
                    result[name.uppercase()] = dayMap
                    result[name.lowercase()] = dayMap
                    val firstName = name.trim().split(" ")[0]
                    result[firstName] = dayMap
                    result[firstName.uppercase()] = dayMap
                    result[firstName.lowercase()] = dayMap
                }
            }
        }
        return result
    }

    private fun EngineerDto.toDomain(): Engineer = Engineer(
        id = this.id?.ifBlank { null } ?: this.engineerId?.ifBlank { null } ?: this.username ?: "",
        name = this.name ?: "",
        fullName = this.name ?: "",
        username = this.username ?: "",
        email = this.email ?: "",
        phone = this.phone ?: "",
        role = try { EngineerRole.valueOf((this.role ?: "ENGINEER").uppercase()) } catch (_: Exception) { EngineerRole.ENGINEER },
        status = EngineerStatus.ACTIVE,
        defaultWeeklyOffs = this.defaultWeeklyOffs ?: emptyList(),
        joinDate = this.joinDate ?: "",
        resignationDate = this.resignationDate,
        originalUsername = this.originalUsername,
        profilePicture = this.profilePicture,
        birthday = this.birthday
    )
}
