package com.campussilencer.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.campussilencer.app.data.CampusConfig
import java.util.Calendar

object CampusAlarmScheduler {

    private const val TAG = "CampusAlarmScheduler"
    private const val REQUEST_START = 1001
    private const val REQUEST_END = 1002
    private const val REQUEST_MORNING = 1003

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() == true
        }
        return true
    }

    fun scheduleAllAlarms(context: Context, config: CampusConfig) {
        if (!config.isProtectionEnabled) {
            cancelAllAlarms(context)
            Log.i(TAG, "Protection disabled: all campus alarms cancelled.")
            return
        }

        scheduleStartAlarm(context, config)
        scheduleEndAlarm(context, config)
        scheduleMorningPromptAlarm(context, config)
    }

    fun scheduleStartAlarm(context: Context, config: CampusConfig) {
        val triggerAtMillis = calculateNextAlarmTime(config.collegeStartHour, config.collegeStartMinute)
        setExactAlarm(context, CampusAlarmReceiver.ACTION_START_ALARM, REQUEST_START, triggerAtMillis)
        Log.i(TAG, "Scheduled Start Alarm for: ${java.util.Date(triggerAtMillis)}")
    }

    fun scheduleEndAlarm(context: Context, config: CampusConfig) {
        val triggerAtMillis = calculateNextAlarmTime(config.collegeEndHour, config.collegeEndMinute)
        setExactAlarm(context, CampusAlarmReceiver.ACTION_END_ALARM, REQUEST_END, triggerAtMillis)
        Log.i(TAG, "Scheduled End Alarm for: ${java.util.Date(triggerAtMillis)}")
    }

    fun scheduleMorningPromptAlarm(context: Context, config: CampusConfig) {
        // Morning notification at 08:00 AM
        val triggerAtMillis = calculateNextAlarmTime(8, 0)
        setExactAlarm(context, CampusAlarmReceiver.ACTION_MORNING_PROMPT, REQUEST_MORNING, triggerAtMillis)
        Log.i(TAG, "Scheduled Morning Prompt Alarm for: ${java.util.Date(triggerAtMillis)}")
    }

    fun cancelAllAlarms(context: Context) {
        cancelAlarm(context, CampusAlarmReceiver.ACTION_START_ALARM, REQUEST_START)
        cancelAlarm(context, CampusAlarmReceiver.ACTION_END_ALARM, REQUEST_END)
        cancelAlarm(context, CampusAlarmReceiver.ACTION_MORNING_PROMPT, REQUEST_MORNING)
    }

    private fun setExactAlarm(context: Context, action: String, requestCode: Int, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CampusAlarmReceiver::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: ${e.message}")
        }
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CampusAlarmReceiver::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextAlarmTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            // Already passed today, schedule for tomorrow
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
