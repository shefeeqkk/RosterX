package com.rosterx.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.model.EngineerRole
import com.rosterx.domain.repository.RosterRepository
import com.rosterx.widget.RosterXAppWidgetProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RosterSyncEntryPoint {
    fun rosterRepository(): RosterRepository
}

class RosterSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RosterSyncEntryPoint::class.java
            )
            val repository = entryPoint.rosterRepository()

            val today = LocalDate.now()
            val currentMonthKey = String.format(Locale.US, "%d-%02d", today.year, today.monthValue)
            val yesterday = today.minusDays(1)
            val yesterdayMonthKey = String.format(Locale.US, "%d-%02d", yesterday.year, yesterday.monthValue)

            val rosterResult = repository.getRoster(currentMonthKey)
            val engineersResult = repository.getEngineers()

            if (rosterResult.isSuccess && engineersResult.isSuccess) {
                val rawRoster: Map<String, Map<String, String>> = rosterResult.getOrNull() ?: emptyMap()
                val engineers: List<Engineer> = engineersResult.getOrNull() ?: emptyList()

                var yesterdayRoster: Map<String, Map<String, String>> = rawRoster
                if (yesterdayMonthKey != currentMonthKey) {
                    try {
                        val yRes = repository.getRoster(yesterdayMonthKey)
                        if (yRes.isSuccess) {
                            yesterdayRoster = yRes.getOrNull() ?: rawRoster
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
                    if (user.role == EngineerRole.MANAGER) {
                        RosterXAppWidgetProvider.saveManagerDutyRoster(
                            context = context,
                            managerName = user.name,
                            dateFormatted = today.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                            dutyList = emptyList(),
                            summaryText = "Coverage Updated",
                            fullRoster = rawRoster,
                            yesterdayRoster = yesterdayRoster,
                            engineers = engineers,
                            monthKey = currentMonthKey
                        )
                        RosterXAppWidgetProvider.recomputeManagerDutyList(context, today, LocalTime.now())
                        RosterXAppWidgetProvider.updateAllWidgets(context)
                    } else {
                        val engShifts: Map<String, String> = rawRoster[user.username]
                            ?: rawRoster[user.id]
                            ?: rawRoster[user.name]
                            ?: emptyMap()
                        if (engShifts.isNotEmpty()) {
                            RosterXAppWidgetProvider.saveWidgetData(
                                context,
                                user.name,
                                engShifts,
                                currentMonthKey
                            )
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Throwable) {
            Log.w("RosterSyncWorker", "Background widget sync error: ${e.message}")
            Result.success()
        }
    }
}
