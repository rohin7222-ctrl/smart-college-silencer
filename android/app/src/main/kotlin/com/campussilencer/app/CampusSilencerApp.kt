package com.campussilencer.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.service.SmartSilencerWorker
import com.campussilencer.app.ui.PowerSaveThemeHelper
import java.util.concurrent.TimeUnit

class CampusSilencerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("CampusSilencerApp", "Application initialized.")

        // Apply theme immediately on cold start (Dark if battery saver ON, Light if battery saver OFF)
        val settingsRepo = AppSettingsRepository(this)
        PowerSaveThemeHelper.applyTheme(this, settingsRepo.getConfig().themePreference)

        // Schedule daily holiday sync worker with WorkManager
        scheduleHolidaySyncWorker()
    }

    private fun scheduleHolidaySyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<SmartSilencerWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyHolidaySyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
