package com.rosterx

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rosterx.worker.RosterSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class RosterXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleWidgetBackgroundSync()
    }

    private fun scheduleWidgetBackgroundSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = PeriodicWorkRequestBuilder<RosterSyncWorker>(
                30, TimeUnit.MINUTES,
                10, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RosterXWidgetSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        } catch (e: Throwable) {
            Log.e("RosterXApp", "Failed to schedule background widget sync", e)
        }
    }
}
