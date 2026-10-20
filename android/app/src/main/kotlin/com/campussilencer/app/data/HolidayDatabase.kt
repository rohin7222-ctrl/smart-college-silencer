package com.campussilencer.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HolidayDatabase (Option 3 Implementation)
 * Stores full yyyy-MM-dd dates per year for State & National Government Holidays.
 * Supports custom user-defined holidays and selected weekday evaluation (replaces hardcoded Sunday rule).
 */
object HolidayDatabase {

    // Official State & National Public Holidays with full yyyy-MM-dd dates
    private val BASE_HOLIDAYS_MAP = linkedMapOf(
        // 2026 Holidays
        "2026-01-01" to "New Year's Day",
        "2026-01-14" to "Bhogi Festival",
        "2026-01-15" to "Pongal / Makar Sankranti",
        "2026-01-16" to "Thiruvalluvar Day",
        "2026-01-17" to "Uzhavar Thirunal",
        "2026-01-26" to "Republic Day (National Holiday)",
        "2026-02-01" to "Thaipusam Festival",
        "2026-02-15" to "Maha Shivaratri",
        "2026-03-20" to "Telugu New Year (Ugadi)",
        "2026-03-21" to "Eid-ul-Fitr (Ramzan)",
        "2026-04-03" to "Good Friday",
        "2026-04-14" to "Tamil New Year / Dr. B.R. Ambedkar Jayanti",
        "2026-04-20" to "Mahavir Jayanti",
        "2026-05-01" to "May Day (Labour Day)",
        "2026-05-28" to "Bakrid (Eid-al-Adha)",
        "2026-06-26" to "Muharram",
        "2026-08-15" to "Independence Day (National Holiday)",
        "2026-09-04" to "Krishna Jayanti (Gokulashtami)",
        "2026-09-14" to "Vinayaka Chathurthi",
        "2026-09-26" to "Milad-un-Nabi (Prophet Birthday)",
        "2026-10-02" to "Mahatma Gandhi Jayanti",
        "2026-10-19" to "Ayutha Pooja",
        "2026-10-20" to "Vijaya Dasami",
        "2026-11-08" to "Deepavali Festival",
        "2026-12-25" to "Christmas Day",

        // 2027 Holidays (Extended projection)
        "2027-01-01" to "New Year's Day",
        "2027-01-15" to "Pongal",
        "2027-01-26" to "Republic Day",
        "2027-08-15" to "Independence Day",
        "2027-10-02" to "Gandhi Jayanti",
        "2027-10-29" to "Deepavali Festival",
        "2027-12-25" to "Christmas Day"
    )

    /**
     * Checks if given date is a Government Holiday or an unselected weekday.
     * Returns Pair(isHolidayOrOff, reason)
     */
    fun checkHoliday(
        date: Date = Date(),
        selectedWeekdays: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
        customHolidays: Map<String, String> = emptyMap()
    ): Pair<Boolean, String> {
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayName = SimpleDateFormat("EEEE", Locale.US).format(date)

        // 1. Check if today is one of the user's selected college weekdays
        if (!selectedWeekdays.contains(dayOfWeek)) {
            return Pair(true, "$dayName (Non-College Day)")
        }

        // 2. Check full yyyy-MM-dd date in official holidays
        val fullDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val officialName = BASE_HOLIDAYS_MAP[fullDate]
        if (officialName != null) {
            return Pair(true, "Official Holiday: $officialName")
        }

        // 3. Check custom user-defined holidays
        val customName = customHolidays[fullDate]
        if (customName != null) {
            return Pair(true, "Custom Holiday: $customName")
        }

        return Pair(false, "Regular College Working Day")
    }

    /**
     * Returns the full combined list of holidays
     */
    fun getAllHolidays(customHolidays: Map<String, String> = emptyMap()): Map<String, String> {
        val combined = LinkedHashMap<String, String>(BASE_HOLIDAYS_MAP)
        combined.putAll(customHolidays)
        return combined
    }

    /**
     * Gets next upcoming holiday starting from given date
     */
    fun getNextUpcomingHoliday(date: Date = Date(), customHolidays: Map<String, String> = emptyMap()): Pair<String, String> {
        val currentFull = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val all = getAllHolidays(customHolidays)
        for ((dateKey, name) in all) {
            if (dateKey >= currentFull) {
                return Pair(dateKey, name)
            }
        }
        val first = all.entries.first()
        return Pair(first.key, first.value)
    }

    fun getTotalHolidaysCount(customHolidays: Map<String, String> = emptyMap()): Int {
        return BASE_HOLIDAYS_MAP.size + customHolidays.size
    }
}
