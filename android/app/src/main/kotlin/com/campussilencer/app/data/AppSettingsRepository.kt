package com.campussilencer.app.data

import android.content.Context
import android.content.SharedPreferences

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)

    fun getConfig(): CampusConfig {
        return CampusConfig(
            collegeName = prefs.getString("college_name", "Anna University / College Campus") ?: "",
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
            enableWifiTrigger = prefs.getBoolean("enable_wifi_trigger", true)
        )
    }

    fun saveConfig(config: CampusConfig) {
        prefs.edit().apply {
            putString("college_name", config.collegeName)
            putFloat("college_lat", config.collegeLatitude.toFloat())
            putFloat("college_lng", config.collegeLongitude.toFloat())
            putFloat("college_radius", config.campusRadiusMeters.toFloat())
            putFloat("home_lat", config.homeLatitude.toFloat())
            putFloat("home_lng", config.homeLongitude.toFloat())
            putFloat("home_radius", config.homeRadiusMeters.toFloat())
            putString("college_wifi_ssid", config.collegeWifiSsid)
            putString("home_wifi_ssid", config.homeWifiSsid)
            putBoolean("vibrate_only", config.vibrateOnly)
            putBoolean("auto_restore_at_home", config.autoRestoreAtHome)
            putBoolean("taking_leave_today", config.takingLeaveToday)
            putBoolean("enable_wifi_trigger", config.enableWifiTrigger)
            apply()
        }
    }

    fun setTakingLeaveToday(takingLeave: Boolean) {
        prefs.edit().putBoolean("taking_leave_today", takingLeave).apply()
    }
}
