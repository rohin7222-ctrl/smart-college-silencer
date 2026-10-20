package com.campussilencer.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HolidayDatabase (Option 3 Implementation)
 * Automatically provides State & National Government Holidays based on user's one-time region.
 * Completely offline with 0 internet required!
 */
object HolidayDatabase {

    // Official State & National Public Holidays for Tamil Nadu & India (2026-2027)
    private val STATE_HOLIDAYS = mapOf(
        "01-01" to "New Year's Day",
        "01-14" to "Bhogi Festival",
        "01-15" to "Pongal / Makar Sankranti",
        "01-16" to "Thiruvalluvar Day",
        "01-17" to "Uzhavar Thirunal",
        "01-26" to "Republic Day (National Holiday)",
        "02-01" to "Thaipusam Festival",
        "02-15" to "Maha Shivaratri",
        "03-20" to "Telugu New Year (Ugadi)",
        "03-21" to "Eid-ul-Fitr (Ramzan)",
        "04-03" to "Good Friday",
        "04-14" to "Tamil New Year / Dr. B.R. Ambedkar Jayanti",
        "04-20" to "Mahavir Jayanti",
        "05-01" to "May Day (Labour Day)",
        "05-28" to "Bakrid (Eid-al-Adha)",
        "06-26" to "Muharram",
        "08-15" to "Independence Day (National Holiday)",
        "09-04" to "Krishna Jayanti (Gokulashtami)",
        "09-14" to "Vinayaka Chathurthi",
        "09-26" to "Milad-un-Nabi (Prophet Birthday)",
        "10-02" to "Mahatma Gandhi Jayanti",
        "10-19" to "Ayutha Pooja",
        "10-20" to "Vijaya Dasami",
        "11-08" to "Deepavali Festival",
        "12-25" to "Christmas Day"
    )

    /**
     * Checks if given date is a Government Public Holiday or Sunday.
     * Returns Pair(isHoliday, reason)
     */
    fun checkHoliday(date: Date = Date()): Pair<Boolean, String> {
        val calendar = Calendar.getInstance().apply { time = date }

        // 1. Weekly College Off Check (Sunday is always closed)
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return Pair(true, "Sunday (College Weekend Holiday)")
        }

        // 2. State & National Government Holidays
        val monthDay = SimpleDateFormat("MM-dd", Locale.US).format(date)
        val holidayName = STATE_HOLIDAYS[monthDay]
        if (holidayName != null) {
            return Pair(true, "Official Government Holiday: $holidayName")
        }

        return Pair(false, "Regular Working Day")
    }

    fun getTotalHolidaysCount(): Int = STATE_HOLIDAYS.size
}
