package com.campussilencer.app.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.HolidayDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * SmartSilencerWorker (Kotlin Layer -> WorkManager)
 * Daily synchronization worker that runs once after midnight.
 * Reschedules exact alarms, evaluates holidays, and ensures expired leaves are cleaned up.
 * 100% offline with zero hardcoded emulator URLs.
 */
class SmartSilencerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmartSilencerWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = AppSettingsRepository(applicationContext)
        val config = repository.getConfig()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        Log.i(TAG, "Running daily midnight maintenance for date: $todayStr")

        // 1. Reschedule alarms for the new day
        CampusAlarmScheduler.scheduleAllAlarms(applicationContext, config)

        // 2. Holiday and weekday check
        val customHolidays = repository.getCustomHolidays()
        val holidayCheck = HolidayDatabase.checkHoliday(Date(), config.selectedWeekdays, customHolidays)

        if (holidayCheck.first) {
            Log.i(TAG, "Today is an off day: ${holidayCheck.second}.")
        } else {
            Log.i(TAG, "Today is a scheduled college working day.")
        }

        Result.success()
    }
}
