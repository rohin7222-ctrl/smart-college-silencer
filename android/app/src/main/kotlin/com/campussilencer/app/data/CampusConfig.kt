package com.campussilencer.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data models for Smart Campus Silencer with 3 Smart Options:
 * Option 1: 📡 Mobile Cell Tower Shield (Cell Tower ID based, zero GPS)
 * Option 2: 🔔 Morning Smart Notify / Departure Check (Prompt based)
 * Option 3: 🗓️ Timetable & Auto-Holiday Engine + Weekday Selector + Custom Hours
 */
enum class ShieldMode(val title: String, val subtitle: String) {
    CELL_TOWER("Option 1: Tower Shield", "Zero GPS • Zero Wi-Fi • Mobile Tower Match"),
    NOTIFY_BASED("Option 2: Morning Notify", "Morning Departure Notification (08:00 AM)"),
    ONE_TIME_HOLIDAY_SCHEDULE("Option 3: Auto Holiday & Hours", "One-Time Location • Gov Holidays • Custom Hours")
}

enum class ThemeModePreference(val displayName: String) {
    AUTO_BATTERY_SAVER("Auto (Battery Saver Sync)"),
    FORCE_DARK("AMOLED Dark Mode"),
    FORCE_LIGHT("Luxury Light Mode")
}

data class CampusConfig(
    val collegeName: String = "Anna University / College Campus",
    val selectedMode: ShieldMode = ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE,
    val themePreference: ThemeModePreference = ThemeModePreference.AUTO_BATTERY_SAVER,
    val isProtectionEnabled: Boolean = true,
    val selectedWeekdays: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    val userRegionState: String = "Tamil Nadu, India",
    val collegeStartTime: String = "08:45",
    val collegeEndTime: String = "16:30",
    val collegeStartHour: Int = 8,
    val collegeStartMinute: Int = 45,
    val collegeEndHour: Int = 16,
    val collegeEndMinute: Int = 30,
    val muteOnlyAtCollege: Boolean = false,
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
    val leaveDate: String = "",
    val enableWifiTrigger: Boolean = false
) {
    // Computed property: leave automatically expires at midnight!
    val takingLeaveToday: Boolean
        get() {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return leaveDate == todayStr
        }
}

enum class SilencerState(val displayName: String) {
    AT_HOME("At Home (Ringer Loud)"),
    IN_TRANSIT("In Transit (Ringer Loud)"),
    INSIDE_CAMPUS("Inside College (Phone Muted)"),
    HOLIDAY_MODE("Holiday Mode (Mute Paused)"),
    LEAVE_MODE("Leave Mode (Mute Paused)"),
    PROTECTION_PAUSED("Protection Paused (Off)")
}

data class HolidayItem(
    val date: String,
    val name: String,
    val isCollegeWorkingDay: Boolean
)
