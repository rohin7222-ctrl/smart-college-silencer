package com.campussilencer.app.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campussilencer.app.data.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SmartSilencerWorker (Kotlin Layer -> WorkManager)
 * Daily synchronization worker that checks academic calendar holidays and
 * timetable exceptions via the Python companion backend or local rules.
 */
class SmartSilencerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmartSilencerWorker"
        private const val COMPANION_API_URL = "http://10.0.2.2:8000/api/holidays/today"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = AppSettingsRepository(applicationContext)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Log.i(TAG, "Running daily holiday check for date: $todayStr")

        try {
            // Attempt to sync with local Python companion server
            val url = URL(COMPANION_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val isHoliday = json.optBoolean("is_holiday", false)
                val holidayName = json.optString("holiday_name", "")

                if (isHoliday) {
                    Log.i(TAG, "Today is an academic holiday: $holidayName. Pausing college muting.")
                    repository.setTakingLeaveToday(true)
                } else {
                    Log.i(TAG, "Today is a regular college working day.")
                    repository.setTakingLeaveToday(false)
                }
                return@withContext Result.success()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Companion server unreachable, using offline calendar rules: ${e.message}")
        }

        // Offline Rule: If today is Sunday, college is closed
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(Date())
        if (dayOfWeek.equals("Sunday", ignoreCase = true)) {
            Log.i(TAG, "Sunday detected: Auto-pausing silent mode.")
            repository.setTakingLeaveToday(true)
        }

        Result.success()
    }
}
