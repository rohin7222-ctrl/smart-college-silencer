package com.campussilencer.app.data

/**
 * Data models for Smart Campus Silencer
 */
data class CampusConfig(
    val collegeName: String = "Anna University / College Campus",
    val collegeLatitude: Double = 13.0102,
    val collegeLongitude: Double = 80.2356,
    val campusRadiusMeters: Double = 400.0,
    val homeLatitude: Double = 13.0827,
    val homeLongitude: Double = 80.2707,
    val homeRadiusMeters: Double = 200.0,
    val collegeWifiSsid: String = "College-WiFi",
    val homeWifiSsid: String = "Home-WiFi",
    val vibrateOnly: Boolean = true,
    val autoRestoreAtHome: Boolean = true,
    val takingLeaveToday: Boolean = false,
    val enableWifiTrigger: Boolean = true
)

enum class SilencerState(val displayName: String) {
    AT_HOME("At Home (Ringer Normal)"),
    IN_TRANSIT("In Transit (Ringer Normal)"),
    INSIDE_CAMPUS("Inside College (Phone Muted)"),
    HOLIDAY_MODE("Holiday Mode (Mute Paused)"),
    LEAVE_MODE("Leave Mode (Mute Paused)")
}

data class HolidayItem(
    val date: String,
    val name: String,
    val isCollegeWorkingDay: Boolean
)
