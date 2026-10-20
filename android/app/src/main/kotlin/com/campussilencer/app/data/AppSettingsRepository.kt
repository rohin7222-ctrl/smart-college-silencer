package com.campussilencer.app.data

import android.content.Context
import android.content.SharedPreferences

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)

    fun getConfig(): CampusConfig {
        val modeStr = prefs.getString("selected_shield_mode", ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE.name) 
            ?: ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE.name
        val mode = try {
            ShieldMode.valueOf(modeStr)
        } catch (e: Exception) {
            ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE
        }

        val themeStr = prefs.getString("theme_preference", ThemeModePreference.AUTO_BATTERY_SAVER.name)
            ?: ThemeModePreference.AUTO_BATTERY_SAVER.name
        val theme = try {
            ThemeModePreference.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeModePreference.AUTO_BATTERY_SAVER
        }

        return CampusConfig(
            collegeName = prefs.getString("college_name", "Anna University / College Campus") ?: "",
            selectedMode = mode,
            themePreference = theme,
            userRegionState = prefs.getString("user_region_state", "Tamil Nadu, India") ?: "Tamil Nadu, India",
            collegeStartTime = prefs.getString("college_start_time", "08:45") ?: "08:45",
            collegeEndTime = prefs.getString("college_end_time", "16:30") ?: "16:30",
            collegeStartHour = prefs.getInt("college_start_hour", 8),
            collegeStartMinute = prefs.getInt("college_start_minute", 45),
            collegeEndHour = prefs.getInt("college_end_hour", 16),
            collegeEndMinute = prefs.getInt("college_end_minute", 30),
            collegeCellTowerId = prefs.getString("college_tower_id", "41528") ?: "41528",
            currentConnectedTowerId = prefs.getString("current_tower_id", "41528") ?: "41528",
            collegeLatitude = prefs.getFloat("college_lat", 13.0102f).toDouble(),
            collegeLongitude = prefs.getFloat("college_lng", 80.2356f).toDouble(),
            campusRadiusMeters = prefs.getFloat("college_radius", 400.0f).toDouble(),
            homeLatitude = prefs.getFloat("home_lat", 13.0827f).toDouble(),
            homeLongitude = prefs.getFloat("home_lng", 80.2707f).toDouble(),
            homeRadiusMeters = prefs.getFloat("home_radius", 200.0f).toDouble(),
            vibrateOnly = prefs.getBoolean("vibrate_only", true),
            autoRestoreAtHome = prefs.getBoolean("auto_restore_at_home", true),
            takingLeaveToday = prefs.getBoolean("taking_leave_today", false),
            enableWifiTrigger = prefs.getBoolean("enable_wifi_trigger", false)
        )
    }

    fun saveConfig(config: CampusConfig) {
        prefs.edit().apply {
            putString("college_name", config.collegeName)
            putString("selected_shield_mode", config.selectedMode.name)
            putString("theme_preference", config.themePreference.name)
            putString("user_region_state", config.userRegionState)
            putString("college_start_time", config.collegeStartTime)
            putString("college_end_time", config.collegeEndTime)
            putInt("college_start_hour", config.collegeStartHour)
            putInt("college_start_minute", config.collegeStartMinute)
            putInt("college_end_hour", config.collegeEndHour)
            putInt("college_end_minute", config.collegeEndMinute)
            putString("college_tower_id", config.collegeCellTowerId)
            putString("current_tower_id", config.currentConnectedTowerId)
            putFloat("college_lat", config.collegeLatitude.toFloat())
            putFloat("college_lng", config.collegeLongitude.toFloat())
            putFloat("college_radius", config.campusRadiusMeters.toFloat())
            putFloat("home_lat", config.homeLatitude.toFloat())
            putFloat("home_lng", config.homeLongitude.toFloat())
            putFloat("home_radius", config.homeRadiusMeters.toFloat())
            putBoolean("vibrate_only", config.vibrateOnly)
            putBoolean("auto_restore_at_home", config.autoRestoreAtHome)
            putBoolean("taking_leave_today", config.takingLeaveToday)
            putBoolean("enable_wifi_trigger", config.enableWifiTrigger)
            apply()
        }
    }

    fun setSelectedMode(mode: ShieldMode) {
        prefs.edit().putString("selected_shield_mode", mode.name).apply()
    }

    fun setThemePreference(pref: ThemeModePreference) {
        prefs.edit().putString("theme_preference", pref.name).apply()
    }

    fun setCollegeTimes(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        val startStr = String.format("%02d:%02d", startHour, startMin)
        val endStr = String.format("%02d:%02d", endHour, endMin)
        prefs.edit().apply {
            putString("college_start_time", startStr)
            putString("college_end_time", endStr)
            putInt("college_start_hour", startHour)
            putInt("college_start_minute", startMin)
            putInt("college_end_hour", endHour)
            putInt("college_end_minute", endMin)
            apply()
        }
    }

    fun setCollegeCellTowerId(towerId: String) {
        prefs.edit().putString("college_tower_id", towerId).apply()
    }

    fun setTakingLeaveToday(takingLeave: Boolean) {
        prefs.edit().putBoolean("taking_leave_today", takingLeave).apply()
    }
}
