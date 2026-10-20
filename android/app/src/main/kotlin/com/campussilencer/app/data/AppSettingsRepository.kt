package com.campussilencer.app.data

import android.content.Context
import android.content.SharedPreferences

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)

    fun getConfig(): CampusConfig {
        val modeStr = prefs.getString("selected_shield_mode", ShieldMode.CELL_TOWER.name) ?: ShieldMode.CELL_TOWER.name
        val mode = try {
            ShieldMode.valueOf(modeStr)
        } catch (e: Exception) {
            ShieldMode.CELL_TOWER
        }

        return CampusConfig(
            collegeName = prefs.getString("college_name", "Anna University / College Campus") ?: "",
            selectedMode = mode,
            collegeCellTowerId = prefs.getString("college_tower_id", "41528") ?: "41528",
            currentConnectedTowerId = prefs.getString("current_tower_id", "41528") ?: "41528",
            collegeStartTime = prefs.getString("college_start_time", "08:45") ?: "08:45",
            collegeEndTime = prefs.getString("college_end_time", "16:30") ?: "16:30",
            collegeLatitude = prefs.getFloat("college_lat", 13.0102f).toDouble(),
            collegeLongitude = prefs.getFloat("college_lng", 80.2356f).toDouble(),
            campusRadiusMeters = prefs.getFloat("college_radius", 400.0f).toDouble(),
            homeLatitude = prefs.getFloat("home_lat", 13.0827f).toDouble(),
            homeLongitude = prefs.getFloat("home_lng", 80.2707f).toDouble(),
            homeRadiusMeters = prefs.getFloat("home_radius", 200.0f).toDouble(),
            collegeWifiSsid = prefs.getString("college_wifi_ssid", "College-Student-WiFi") ?: "",
            homeWifiSsid = prefs.getString("home_wifi_ssid", "Home-WiFi") ?: "",
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
            putString("college_tower_id", config.collegeCellTowerId)
            putString("current_tower_id", config.currentConnectedTowerId)
            putString("college_start_time", config.collegeStartTime)
            putString("college_end_time", config.collegeEndTime)
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

    fun setCollegeCellTowerId(towerId: String) {
        prefs.edit().putString("college_tower_id", towerId).apply()
    }

    fun setTakingLeaveToday(takingLeave: Boolean) {
        prefs.edit().putBoolean("taking_leave_today", takingLeave).apply()
    }
}
