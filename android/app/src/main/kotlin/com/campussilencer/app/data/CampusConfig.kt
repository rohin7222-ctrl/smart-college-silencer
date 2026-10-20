package com.campussilencer.app.data

/**
 * Data models for Smart Campus Silencer with 3 Smart Options:
 * Option 1: 📡 Mobile Cell Tower Shield (Cell Tower ID based, zero GPS)
 * Option 2: 🔔 Morning Smart Notify / Departure Check (Prompt based)
 * Option 3: 🗓️ One-Time Location & Auto-Holiday Engine + Custom Hours
 */
enum class ShieldMode(val title: String, val subtitle: String) {
    CELL_TOWER("Option 1: Tower Shield", "Zero GPS • Zero Wi-Fi • Mobile Tower Match"),
    NOTIFY_BASED("Option 2: Morning Notify", "Morning Departure Notification (08:00 AM)"),
    ONE_TIME_HOLIDAY_SCHEDULE("Option 3: Auto Holiday & Hours", "One-Time Location • Gov Holidays • Custom Hours")
}

data class CampusConfig(
    val collegeName: String = "Anna University / College Campus",
    val selectedMode: ShieldMode = ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE,
    val userRegionState: String = "Tamil Nadu, India",
    val collegeStartTime: String = "08:45",
    val collegeEndTime: String = "16:30",
    val collegeStartHour: Int = 8,
    val collegeStartMinute: Int = 45,
    val collegeEndHour: Int = 16,
    val collegeEndMinute: Int = 30,
    val collegeCellTowerId: String = "41528",
    val currentConnectedTowerId: String = "41528",
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
    val enableWifiTrigger: Boolean = false
)

enum class SilencerState(val displayName: String) {
    AT_HOME("At Home (Ringer Loud)"),
    IN_TRANSIT("In Transit (Ringer Loud)"),
    INSIDE_CAMPUS("Inside College (Phone Muted)"),
    HOLIDAY_MODE("Holiday Mode (Mute Paused)"),
    LEAVE_MODE("Leave Mode (Mute Paused)")
}

data class HolidayItem(
    val date: String,
    val name: String,
    val isCollegeWorkingDay: Boolean
)
