package com.campussilencer.app.data

/**
 * Data models for Smart Campus Silencer with Dual-Shield Modes:
 * Mode 1 (Plan A): Mobile Cell Tower ID (Zero GPS, Zero Wi-Fi, 100% Privacy)
 * Mode 2 (Plan B): Smart Academic Timetable & Departure/Home-Return Auto-Timer
 */
enum class ShieldMode(val title: String, val description: String) {
    CELL_TOWER("Mode 1: Cell Tower Shield", "Zero GPS • Zero Wi-Fi • Detects College Mobile Tower"),
    TIMETABLE("Mode 2: Smart Timetable", "Schedule-Based • Auto-Unmute at 04:30 PM & Home")
}

data class CampusConfig(
    val collegeName: String = "Anna University / College Campus",
    val selectedMode: ShieldMode = ShieldMode.CELL_TOWER,
    val collegeCellTowerId: String = "41528",
    val currentConnectedTowerId: String = "41528",
    val collegeStartTime: String = "08:45",
    val collegeEndTime: String = "16:30",
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
