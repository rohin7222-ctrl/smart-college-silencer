package com.campussilencer.app.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.campussilencer.app.MainActivity
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.HolidayDatabase
import com.campussilencer.app.data.ShieldMode
import java.util.Calendar

/**
 * CampusAlarmReceiver
 * Receives exact alarms for Option 3 college start & end times and Option 2 morning departure prompts.
 * Unmute ALWAYS runs on end alarm regardless of service state.
 */
class CampusAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CampusAlarmReceiver"
        const val ACTION_START_ALARM = "com.campussilencer.app.ACTION_START_ALARM"
        const val ACTION_END_ALARM = "com.campussilencer.app.ACTION_END_ALARM"
        const val ACTION_MORNING_PROMPT = "com.campussilencer.app.ACTION_MORNING_PROMPT"
        const val ACTION_CONFIRM_ATTENDANCE = "com.campussilencer.app.ACTION_CONFIRM_ATTENDANCE"
        const val ACTION_MARK_LEAVE = "com.campussilencer.app.ACTION_MARK_LEAVE"

        const val PROMPT_CHANNEL_ID = "campus_morning_prompts"
        const val NOTIFICATION_ID_PROMPT = 2002
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "CampusAlarmReceiver received action: $action")

        val settingsRepo = AppSettingsRepository(context)
        val audioController = AudioModeController(context)
        val config = settingsRepo.getConfig()

        when (action) {
            ACTION_START_ALARM -> {
                handleStartAlarm(context, settingsRepo, audioController)
                // Reschedule for next occurrence
                CampusAlarmScheduler.scheduleStartAlarm(context, config)
            }
            ACTION_END_ALARM -> {
                handleEndAlarm(context, audioController)
                // Reschedule for next occurrence
                CampusAlarmScheduler.scheduleEndAlarm(context, config)
            }
            ACTION_MORNING_PROMPT -> {
                handleMorningPrompt(context, settingsRepo)
                CampusAlarmScheduler.scheduleMorningPromptAlarm(context, config)
            }
            ACTION_CONFIRM_ATTENDANCE -> {
                // User confirmed heading to college
                dismissNotification(context, NOTIFICATION_ID_PROMPT)
                Log.i(TAG, "User confirmed college attendance today via morning prompt.")
                // If already within college hours, mute immediately
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMin = calendar.get(Calendar.MINUTE)
                val currentMinutes = currentHour * 60 + currentMin
                val startMinutes = config.collegeStartHour * 60 + config.collegeStartMinute
                val endMinutes = config.collegeEndHour * 60 + config.collegeEndMinute

                if (currentMinutes in startMinutes until endMinutes) {
                    audioController.muteForCampus(config.vibrateOnly)
                }
            }
            ACTION_MARK_LEAVE -> {
                // User marked on leave for today
                dismissNotification(context, NOTIFICATION_ID_PROMPT)
                settingsRepo.setTakingLeaveToday(true)
                Log.i(TAG, "User marked today as leave via morning prompt. Mute disabled.")
                if (audioController.isCampusMuted) {
                    audioController.restoreNormalRinger()
                }
            }
        }
    }

    private fun handleStartAlarm(
        context: Context,
        settingsRepo: AppSettingsRepository,
        audioController: AudioModeController
    ) {
        val config = settingsRepo.getConfig()

        // 1. Protection must be enabled
        if (!config.isProtectionEnabled) {
            Log.i(TAG, "Protection is paused: skipping mute.")
            return
        }

        // 2. Only Option 3 uses direct scheduled start mute
        if (config.selectedMode != ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE) {
            Log.d(TAG, "Selected mode is not Option 3, skipping time-based mute.")
            return
        }

        // 3. Check if today is in user's selected weekdays (Fix 1)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (!config.selectedWeekdays.contains(dayOfWeek)) {
            Log.i(TAG, "Today is not a selected college weekday ($dayOfWeek). Skipping mute.")
            return
        }

        // 4. Check if today is a Government Holiday or Custom Holiday (Fix 10)
        val customHolidays = settingsRepo.getCustomHolidays()
        val holidayCheck = HolidayDatabase.checkHoliday(
            calendar.time,
            config.selectedWeekdays,
            customHolidays
        )
        if (holidayCheck.first) {
            Log.i(TAG, "Today is holiday/off (${holidayCheck.second}). Skipping mute.")
            return
        }

        // 5. Check if today is on leave (Fix 2)
        if (config.takingLeaveToday) {
            Log.i(TAG, "User marked leave for today (${config.leaveDate}). Skipping mute.")
            return
        }

        // 6. Optional location check: "Mute only when at college" (Fix 5)
        if (config.muteOnlyAtCollege) {
            val isAtCollege = checkUserAtCollegeLocation(context, config)
            if (!isAtCollege) {
                Log.i(TAG, "Mute only at college is enabled and user is outside campus. Skipping mute.")
                return
            }
        }

        // 7. Perform mute
        Log.i(TAG, "Start Alarm: Auto-muting phone for college hours!")
        audioController.muteForCampus(config.vibrateOnly)
    }

    private fun handleEndAlarm(context: Context, audioController: AudioModeController) {
        // ALWAYS UNMUTE regardless of any state! (Fix 3)
        Log.i(TAG, "End Alarm: College hours finished. ALWAYS restoring normal ringer!")
        audioController.restoreNormalRinger()
    }

    private fun handleMorningPrompt(context: Context, settingsRepo: AppSettingsRepository) {
        val config = settingsRepo.getConfig()
        if (!config.isProtectionEnabled || config.selectedMode != ShieldMode.NOTIFY_BASED) {
            return
        }

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (!config.selectedWeekdays.contains(dayOfWeek)) {
            return
        }

        val customHolidays = settingsRepo.getCustomHolidays()
        val holidayCheck = HolidayDatabase.checkHoliday(calendar.time, config.selectedWeekdays, customHolidays)
        if (holidayCheck.first) {
            return
        }

        if (config.takingLeaveToday) {
            return
        }

        // Show Morning Departure Notification with Action Buttons (Fix 9)
        createPromptNotificationChannel(context)

        val confirmIntent = Intent(context, CampusAlarmReceiver::class.java).apply {
            action = ACTION_CONFIRM_ATTENDANCE
        }
        val confirmPending = PendingIntent.getBroadcast(
            context, 101, confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val leaveIntent = Intent(context, CampusAlarmReceiver::class.java).apply {
            action = ACTION_MARK_LEAVE
        }
        val leavePending = PendingIntent.getBroadcast(
            context, 102, leaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, PROMPT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🎓 College Today?")
            .setContentText("Heading to classes today? Auto-mute will guard your phone.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Yes, Going to College", confirmPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "🏖️ On Leave", leavePending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID_PROMPT, notification)
    }

    private fun checkUserAtCollegeLocation(context: Context, config: com.campussilencer.app.data.CampusConfig): Boolean {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: return true // If location unavailable, fail-open to ensure class isn't interrupted

            val results = FloatArray(1)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                config.collegeLatitude, config.collegeLongitude,
                results
            )
            return results[0] <= config.campusRadiusMeters
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for one-shot check", e)
            return true
        }
    }

    private fun createPromptNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PROMPT_CHANNEL_ID,
                "Morning College Departure Prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Interactive notifications asking whether user is attending college today"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)
    }
}
