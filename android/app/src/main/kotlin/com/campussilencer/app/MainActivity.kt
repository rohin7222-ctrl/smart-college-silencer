package com.campussilencer.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.CampusConfig
import com.campussilencer.app.data.HolidayDatabase
import com.campussilencer.app.data.ShieldMode
import com.campussilencer.app.data.ThemeModePreference
import com.campussilencer.app.service.CampusAlarmReceiver
import com.campussilencer.app.service.CampusAlarmScheduler
import com.campussilencer.app.service.CampusGeofenceService
import com.campussilencer.app.service.CellTowerHelper
import com.campussilencer.app.ui.PowerSaveThemeHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var audioController: AudioModeController
    private lateinit var settingsRepo: AppSettingsRepository

    // Top Header Views
    private lateinit var tvHeaderDate: TextView
    private lateinit var tvBadgeStatus: TextView

    // Bottom Navigation & Pages
    private lateinit var pageDashboard: ScrollView
    private lateinit var pageModes: ScrollView
    private lateinit var pageHolidays: ScrollView
    private lateinit var pageSystem: ScrollView

    private lateinit var tabNavDashboard: LinearLayout
    private lateinit var tabNavModes: LinearLayout
    private lateinit var tabNavHolidays: LinearLayout
    private lateinit var tabNavSystem: LinearLayout
    private lateinit var tvNavDashboard: TextView
    private lateinit var tvNavModes: TextView
    private lateinit var tvNavHolidays: TextView
    private lateinit var tvNavSystem: TextView

    // Dashboard Views (Page 1)
    private lateinit var cardProtectionToggle: LinearLayout
    private lateinit var tvProtectionTitle: TextView
    private lateinit var tvProtectionIndicator: TextView
    private lateinit var tvProtectionSub: TextView
    private lateinit var btnToggleProtection: Button

    private lateinit var cardSaturdayCheck: LinearLayout
    private lateinit var btnSatHaveCollege: Button
    private lateinit var btnSatNoLeave: Button

    private lateinit var tvTodayStatusBadge: TextView
    private lateinit var tvTodayDetailSub: TextView

    private lateinit var imgModeIcon: ImageView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvRingerMode: TextView
    private lateinit var tvVolumePercentage: TextView
    private lateinit var progressVolume: ProgressBar

    private lateinit var layoutLeaveCard: LinearLayout
    private lateinit var switchLeaveToday: Switch

    private lateinit var btnEmergencyUnmute: Button
    private lateinit var btnTestMute: Button
    private lateinit var btnTestUnmute: Button

    // Modes & Timetable Views (Page 2)
    private lateinit var btnTabOpt1: Button
    private lateinit var btnTabOpt2: Button
    private lateinit var btnTabOpt3: Button
    private lateinit var layoutOption1: LinearLayout
    private lateinit var layoutOption2: LinearLayout
    private lateinit var layoutOption3: LinearLayout

    // 7 Weekday Chips
    private lateinit var chipMon: Button
    private lateinit var chipTue: Button
    private lateinit var chipWed: Button
    private lateinit var chipThu: Button
    private lateinit var chipFri: Button
    private lateinit var chipSat: Button
    private lateinit var chipSun: Button
    private val weekdayChips = mutableMapOf<Int, Button>()

    private lateinit var btnPickStartTime: LinearLayout
    private lateinit var btnPickEndTime: LinearLayout
    private lateinit var tvStartTimeVal: TextView
    private lateinit var tvEndTimeVal: TextView
    private lateinit var switchMuteOnlyAtCollege: Switch

    private lateinit var tvConnectedTower: TextView
    private lateinit var tvCollegeTower: TextView
    private lateinit var btnLockCurrentTower: Button
    private lateinit var btnTestMorningPrompt: Button

    // Holidays Views (Page 3)
    private lateinit var tvDetectedRegion: TextView
    private lateinit var tvNextHolidayBanner: TextView
    private lateinit var btnAddCustomHoliday: Button
    private lateinit var layoutHolidayItems: LinearLayout

    // System Views (Page 4)
    private lateinit var tvPowerSaveIcon: TextView
    private lateinit var tvPowerSaveTitle: TextView
    private lateinit var tvPowerSaveSub: TextView
    private lateinit var tvThemeModeBadge: TextView
    private lateinit var btnThemeAuto: Button
    private lateinit var btnThemeDark: Button
    private lateinit var btnThemeLight: Button

    private lateinit var btnDndPermission: Button
    private lateinit var btnExactAlarmPermission: Button
    private lateinit var btnBatteryOptimization: Button
    private lateinit var btnBgLocationPermission: Button

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CampusGeofenceService.ACTION_STATUS_UPDATED) {
                val status = intent.getStringExtra(CampusGeofenceService.EXTRA_STATUS) ?: "Unknown"
                val distance = intent.getDoubleExtra(CampusGeofenceService.EXTRA_DISTANCE, 0.0)
                updateUiState(status, distance)
            }
        }
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                handlePowerSaveModeChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsRepo = AppSettingsRepository(this)
        // Force the theme onto this Activity before inflating views
        PowerSaveThemeHelper.applyThemeToActivity(this, settingsRepo.getConfig().themePreference)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioController = AudioModeController(this)

        initViews()
        setupNavigation()
        setupListeners()
        checkForegroundPermissions()
        refreshUi()

        // Schedule exact alarms
        val config = settingsRepo.getConfig()
        CampusAlarmScheduler.scheduleAllAlarms(this, config)
    }

    private fun initViews() {
        tvHeaderDate = findViewById(R.id.tv_header_date)
        tvBadgeStatus = findViewById(R.id.tv_badge_status)

        // Pages
        pageDashboard = findViewById(R.id.page_dashboard)
        pageModes = findViewById(R.id.page_modes)
        pageHolidays = findViewById(R.id.page_holidays)
        pageSystem = findViewById(R.id.page_system)

        // Nav tabs
        tabNavDashboard = findViewById(R.id.tab_nav_dashboard)
        tabNavModes = findViewById(R.id.tab_nav_modes)
        tabNavHolidays = findViewById(R.id.tab_nav_holidays)
        tabNavSystem = findViewById(R.id.tab_nav_system)
        tvNavDashboard = findViewById(R.id.tv_nav_dashboard)
        tvNavModes = findViewById(R.id.tv_nav_modes)
        tvNavHolidays = findViewById(R.id.tv_nav_holidays)
        tvNavSystem = findViewById(R.id.tv_nav_system)

        // Dashboard
        cardProtectionToggle = findViewById(R.id.card_protection_toggle)
        tvProtectionTitle = findViewById(R.id.tv_protection_title)
        tvProtectionIndicator = findViewById(R.id.tv_protection_indicator)
        tvProtectionSub = findViewById(R.id.tv_protection_sub)
        btnToggleProtection = findViewById(R.id.btn_toggle_protection)

        cardSaturdayCheck = findViewById(R.id.card_saturday_check)
        btnSatHaveCollege = findViewById(R.id.btn_sat_have_college)
        btnSatNoLeave = findViewById(R.id.btn_sat_no_leave)

        tvTodayStatusBadge = findViewById(R.id.tv_today_status_badge)
        tvTodayDetailSub = findViewById(R.id.tv_today_detail_sub)

        imgModeIcon = findViewById(R.id.img_mode_icon)
        tvCurrentStatus = findViewById(R.id.tv_current_status)
        tvRingerMode = findViewById(R.id.tv_ringer_mode)
        tvVolumePercentage = findViewById(R.id.tv_volume_percentage)
        progressVolume = findViewById(R.id.progress_volume)

        layoutLeaveCard = findViewById(R.id.layout_leave_card)
        switchLeaveToday = findViewById(R.id.switch_leave_today)

        btnEmergencyUnmute = findViewById(R.id.btn_emergency_unmute)
        btnTestMute = findViewById(R.id.btn_test_mute)
        btnTestUnmute = findViewById(R.id.btn_test_unmute)

        // Modes
        btnTabOpt1 = findViewById(R.id.btn_tab_opt_1)
        btnTabOpt2 = findViewById(R.id.btn_tab_opt_2)
        btnTabOpt3 = findViewById(R.id.btn_tab_opt_3)
        layoutOption1 = findViewById(R.id.layout_option_1)
        layoutOption2 = findViewById(R.id.layout_option_2)
        layoutOption3 = findViewById(R.id.layout_option_3)

        // Chips
        chipMon = findViewById(R.id.chip_mon)
        chipTue = findViewById(R.id.chip_tue)
        chipWed = findViewById(R.id.chip_wed)
        chipThu = findViewById(R.id.chip_thu)
        chipFri = findViewById(R.id.chip_fri)
        chipSat = findViewById(R.id.chip_sat)
        chipSun = findViewById(R.id.chip_sun)

        weekdayChips[Calendar.MONDAY] = chipMon
        weekdayChips[Calendar.TUESDAY] = chipTue
        weekdayChips[Calendar.WEDNESDAY] = chipWed
        weekdayChips[Calendar.THURSDAY] = chipThu
        weekdayChips[Calendar.FRIDAY] = chipFri
        weekdayChips[Calendar.SATURDAY] = chipSat
        weekdayChips[Calendar.SUNDAY] = chipSun

        btnPickStartTime = findViewById(R.id.btn_pick_start_time)
        btnPickEndTime = findViewById(R.id.btn_pick_end_time)
        tvStartTimeVal = findViewById(R.id.tv_start_time_val)
        tvEndTimeVal = findViewById(R.id.tv_end_time_val)
        switchMuteOnlyAtCollege = findViewById(R.id.switch_mute_only_at_college)

        tvConnectedTower = findViewById(R.id.tv_connected_tower)
        tvCollegeTower = findViewById(R.id.tv_college_tower)
        btnLockCurrentTower = findViewById(R.id.btn_lock_current_tower)
        btnTestMorningPrompt = findViewById(R.id.btn_test_morning_prompt)

        // Holidays
        tvDetectedRegion = findViewById(R.id.tv_detected_region)
        tvNextHolidayBanner = findViewById(R.id.tv_next_holiday_banner)
        btnAddCustomHoliday = findViewById(R.id.btn_add_custom_holiday)
        layoutHolidayItems = findViewById(R.id.layout_holiday_items)

        // System
        tvPowerSaveIcon = findViewById(R.id.tv_power_save_icon)
        tvPowerSaveTitle = findViewById(R.id.tv_power_save_title)
        tvPowerSaveSub = findViewById(R.id.tv_power_save_sub)
        tvThemeModeBadge = findViewById(R.id.tv_theme_mode_badge)
        btnThemeAuto = findViewById(R.id.btn_theme_auto)
        btnThemeDark = findViewById(R.id.btn_theme_dark)
        btnThemeLight = findViewById(R.id.btn_theme_light)

        btnDndPermission = findViewById(R.id.btn_dnd_permission)
        btnExactAlarmPermission = findViewById(R.id.btn_exact_alarm_permission)
        btnBatteryOptimization = findViewById(R.id.btn_battery_optimization)
        btnBgLocationPermission = findViewById(R.id.btn_bg_location_permission)

        // Date Display
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)
        tvHeaderDate.text = fullDateFormat.format(Date())

        val config = settingsRepo.getConfig()
        updateProtectionButtonUi(config.isProtectionEnabled)
        updateWeekdayChipsUi(config.selectedWeekdays)
        updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, config.collegeEndHour, config.collegeEndMinute)
        switchMuteOnlyAtCollege.isChecked = config.muteOnlyAtCollege
        switchLeaveToday.isChecked = config.takingLeaveToday

        val activeTower = CellTowerHelper.getCurrentCellTowerId(this)
        tvConnectedTower.text = "Cell ID #$activeTower"
        tvCollegeTower.text = "Cell ID #${config.collegeCellTowerId}"

        // Saturday check visibility
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            cardSaturdayCheck.visibility = View.VISIBLE
        } else {
            cardSaturdayCheck.visibility = View.GONE
        }

        switchModeTab(config.selectedMode)
        populateHolidaysList()
        updatePowerSaveBannerUi(PowerSaveThemeHelper.isPowerSaveModeActive(this), config.themePreference)
    }

    private fun setupNavigation() {
        tabNavDashboard.setOnClickListener { switchPage(0) }
        tabNavModes.setOnClickListener { switchPage(1) }
        tabNavHolidays.setOnClickListener { switchPage(2) }
        tabNavSystem.setOnClickListener { switchPage(3) }
    }

    private fun switchPage(pageIndex: Int) {
        pageDashboard.visibility = if (pageIndex == 0) View.VISIBLE else View.GONE
        pageModes.visibility = if (pageIndex == 1) View.VISIBLE else View.GONE
        pageHolidays.visibility = if (pageIndex == 2) View.VISIBLE else View.GONE
        pageSystem.visibility = if (pageIndex == 3) View.VISIBLE else View.GONE

        // Update tab styling
        setTabStyle(tabNavDashboard, tvNavDashboard, pageIndex == 0)
        setTabStyle(tabNavModes, tvNavModes, pageIndex == 1)
        setTabStyle(tabNavHolidays, tvNavHolidays, pageIndex == 2)
        setTabStyle(tabNavSystem, tvNavSystem, pageIndex == 3)
    }

    private fun setTabStyle(tab: LinearLayout, label: TextView, isActive: Boolean) {
        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_tab_active)
            label.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_inactive)
            label.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
        }
    }

    private fun setupListeners() {
        // Start / Stop Master Protection Shield Toggle
        btnToggleProtection.setOnClickListener {
            val config = settingsRepo.getConfig()
            val newEnabled = !config.isProtectionEnabled
            settingsRepo.setProtectionEnabled(newEnabled)
            updateProtectionButtonUi(newEnabled)

            if (newEnabled) {
                CampusAlarmScheduler.scheduleAllAlarms(this, settingsRepo.getConfig())
                if (config.selectedMode == ShieldMode.CELL_TOWER || config.muteOnlyAtCollege) {
                    startProtectionService()
                }
                Toast.makeText(this, "🚀 Protection Shield Started! Alarms & monitoring active.", Toast.LENGTH_SHORT).show()
            } else {
                CampusAlarmScheduler.cancelAllAlarms(this)
                stopService(Intent(this, CampusGeofenceService::class.java))
                audioController.restoreNormalRinger()
                Toast.makeText(this, "🛑 Protection Stopped! Alarms cancelled and ringer restored.", Toast.LENGTH_SHORT).show()
            }
            refreshUi()
        }

        // Saturday Action Buttons
        btnSatHaveCollege.setOnClickListener {
            settingsRepo.setTakingLeaveToday(false)
            val config = settingsRepo.getConfig()
            val updatedDays = config.selectedWeekdays.toMutableSet().apply { add(Calendar.SATURDAY) }
            settingsRepo.setSelectedWeekdays(updatedDays)
            updateWeekdayChipsUi(updatedDays)
            cardSaturdayCheck.visibility = View.GONE
            Toast.makeText(this, "✅ Saturday college attendance confirmed! Shield armed.", Toast.LENGTH_SHORT).show()
            evaluateCurrentState()
        }

        btnSatNoLeave.setOnClickListener {
            settingsRepo.setTakingLeaveToday(true)
            cardSaturdayCheck.visibility = View.GONE
            audioController.restoreNormalRinger()
            Toast.makeText(this, "🏖️ Weekend off confirmed! Phone will NOT mute today.", Toast.LENGTH_SHORT).show()
            evaluateCurrentState()
        }

        // Leave Today Toggle (Auto-expires at midnight!)
        switchLeaveToday.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setTakingLeaveToday(isChecked)
            if (isChecked) {
                audioController.restoreNormalRinger()
                Toast.makeText(this, "🏖️ Leave active today! Auto-expires at midnight.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Normal college monitoring active.", Toast.LENGTH_SHORT).show()
            }
            evaluateCurrentState()
        }

        // Emergency 1-Hour Sound Restore
        btnEmergencyUnmute.setOnClickListener {
            audioController.restoreNormalRinger()
            Toast.makeText(this, "⏰ Ringer restored for 1 Hour! You will not miss any calls.", Toast.LENGTH_LONG).show()
            refreshUi()
        }

        // Test Buttons
        btnTestMute.setOnClickListener {
            if (audioController.muteForCampus(settingsRepo.getConfig().vibrateOnly)) {
                Toast.makeText(this, "Test Success: Phone Muted (Vibrate)! Calls will buzz.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }

        btnTestUnmute.setOnClickListener {
            if (audioController.restoreNormalRinger()) {
                val savedVol = audioController.savedVolume
                Toast.makeText(this, "Test Success: Restored previous ringer mode & volume ($savedVol)!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }

        // Mode Switching
        btnTabOpt1.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.CELL_TOWER)
            switchModeTab(ShieldMode.CELL_TOWER)
            Toast.makeText(this, "Option 1 Selected: Cell Tower Shield (Zero GPS)", Toast.LENGTH_SHORT).show()
        }

        btnTabOpt2.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.NOTIFY_BASED)
            switchModeTab(ShieldMode.NOTIFY_BASED)
            Toast.makeText(this, "Option 2 Selected: Morning Smart Notify", Toast.LENGTH_SHORT).show()
        }

        btnTabOpt3.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE)
            switchModeTab(ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE)
            Toast.makeText(this, "Option 3 Selected: Auto Holiday & Weekday Timetable", Toast.LENGTH_SHORT).show()
        }

        // Weekday Chips Click Listeners (Fix 1)
        weekdayChips.forEach { (dayInt, chipBtn) ->
            chipBtn.setOnClickListener {
                val currentSet = settingsRepo.getConfig().selectedWeekdays.toMutableSet()
                if (currentSet.contains(dayInt)) {
                    if (currentSet.size > 1) {
                        currentSet.remove(dayInt)
                    } else {
                        Toast.makeText(this, "Must select at least 1 college working day!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } else {
                    currentSet.add(dayInt)
                }
                settingsRepo.setSelectedWeekdays(currentSet)
                updateWeekdayChipsUi(currentSet)
                CampusAlarmScheduler.scheduleAllAlarms(this, settingsRepo.getConfig())
                evaluateCurrentState()
            }
        }

        // Option 3 Time Pickers
        btnPickStartTime.setOnClickListener {
            val config = settingsRepo.getConfig()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                settingsRepo.setCollegeTimes(hourOfDay, minute, config.collegeEndHour, config.collegeEndMinute)
                updateTimeDisplays(hourOfDay, minute, config.collegeEndHour, config.collegeEndMinute)
                CampusAlarmScheduler.scheduleAllAlarms(this, settingsRepo.getConfig())
                Toast.makeText(this, "College Start Time updated!", Toast.LENGTH_SHORT).show()
                evaluateCurrentState()
            }, config.collegeStartHour, config.collegeStartMinute, false).show()
        }

        btnPickEndTime.setOnClickListener {
            val config = settingsRepo.getConfig()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                settingsRepo.setCollegeTimes(config.collegeStartHour, config.collegeStartMinute, hourOfDay, minute)
                updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, hourOfDay, minute)
                CampusAlarmScheduler.scheduleAllAlarms(this, settingsRepo.getConfig())
                Toast.makeText(this, "College End Time updated!", Toast.LENGTH_SHORT).show()
                evaluateCurrentState()
            }, config.collegeEndHour, config.collegeEndMinute, false).show()
        }

        // Mute only when at college location switch (Fix 5)
        switchMuteOnlyAtCollege.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setMuteOnlyAtCollege(isChecked)
            Toast.makeText(this, if (isChecked) "Location Guard: Will check campus GPS before muting" else "Mutes by schedule directly", Toast.LENGTH_SHORT).show()
        }

        // Option 1 Lock Current Tower
        btnLockCurrentTower.setOnClickListener {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            settingsRepo.setCollegeCellTowerId(currentTower)
            tvCollegeTower.text = "Cell ID #$currentTower"
            Toast.makeText(this, "Saved Tower #$currentTower as College Campus!", Toast.LENGTH_LONG).show()
            evaluateCurrentState()
        }

        // Option 2 Test Morning Prompt (Fix 9)
        btnTestMorningPrompt.setOnClickListener {
            val receiver = CampusAlarmReceiver()
            val intent = Intent(this, CampusAlarmReceiver::class.java).apply {
                action = CampusAlarmReceiver.ACTION_MORNING_PROMPT
            }
            receiver.onReceive(this, intent)
            Toast.makeText(this, "Morning Prompt Notification Triggered! Check your notification shade.", Toast.LENGTH_SHORT).show()
        }

        // Add Custom Holiday Button (Fix 10)
        btnAddCustomHoliday.setOnClickListener {
            showAddHolidayDialog()
        }

        // System Page Theme Buttons
        btnThemeAuto.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.AUTO_BATTERY_SAVER)
            PowerSaveThemeHelper.applyThemeToActivity(this, ThemeModePreference.AUTO_BATTERY_SAVER)
            val isPs = PowerSaveThemeHelper.isPowerSaveModeActive(this)
            updatePowerSaveBannerUi(isPs, ThemeModePreference.AUTO_BATTERY_SAVER)
            val msg = if (isPs) "⚡ Battery Saver ON → Dark Mode Active" else "☀️ Battery Saver OFF → Light Mode Active"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnThemeDark.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.FORCE_DARK)
            PowerSaveThemeHelper.applyThemeToActivity(this, ThemeModePreference.FORCE_DARK)
            updatePowerSaveBannerUi(PowerSaveThemeHelper.isPowerSaveModeActive(this), ThemeModePreference.FORCE_DARK)
            Toast.makeText(this, "🌙 AMOLED Dark Mode Activated", Toast.LENGTH_SHORT).show()
        }

        btnThemeLight.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.FORCE_LIGHT)
            PowerSaveThemeHelper.applyThemeToActivity(this, ThemeModePreference.FORCE_LIGHT)
            updatePowerSaveBannerUi(PowerSaveThemeHelper.isPowerSaveModeActive(this), ThemeModePreference.FORCE_LIGHT)
            Toast.makeText(this, "☀️ Luxury Light Mode Activated", Toast.LENGTH_SHORT).show()
        }

        // Permission Buttons
        btnDndPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }

        btnExactAlarmPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } else {
                Toast.makeText(this, "Exact Alarms are supported natively on this Android version.", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatteryOptimization.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        btnBgLocationPermission.setOnClickListener {
            requestBackgroundLocationPermission()
        }
    }

    private fun updateProtectionButtonUi(isEnabled: Boolean) {
        if (isEnabled) {
            tvProtectionTitle.text = "🟢 Protection Shield Active"
            tvProtectionIndicator.text = "ARMED"
            tvProtectionIndicator.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            tvProtectionSub.text = "Auto-muting on your selected college days & hours is active."
            btnToggleProtection.text = "🛑 STOP PROTECTION SHIELD"
            btnToggleProtection.setBackgroundResource(R.drawable.bg_stop_glow_button)
            tvBadgeStatus.text = "● ON"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
        } else {
            tvProtectionTitle.text = "🔴 Protection Shield Paused"
            tvProtectionIndicator.text = "OFF"
            tvProtectionIndicator.setTextColor(ContextCompat.getColor(this, R.color.rose_500))
            tvProtectionSub.text = "Alarms cancelled. Phone will NOT auto-mute."
            btnToggleProtection.text = "🚀 START PROTECTION SHIELD"
            btnToggleProtection.setBackgroundResource(R.drawable.bg_amber_glow_button)
            btnToggleProtection.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            tvBadgeStatus.text = "● OFF"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(this, R.color.rose_500))
        }
    }

    private fun updateWeekdayChipsUi(selectedDays: Set<Int>) {
        weekdayChips.forEach { (dayInt, button) ->
            if (selectedDays.contains(dayInt)) {
                button.setBackgroundResource(R.drawable.bg_chip_weekday_active)
                button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            } else {
                button.setBackgroundResource(R.drawable.bg_chip_weekday_inactive)
                button.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
            }
        }
    }

    private fun showAddHolidayDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            val input = EditText(this).apply {
                hint = "Holiday Name (e.g. Sports Day / Annual Fest)"
            }
            AlertDialog.Builder(this)
                .setTitle("Holiday for $dateStr")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val name = input.text.toString().trim().ifEmpty { "College Holiday" }
                    settingsRepo.addCustomHoliday(dateStr, name)
                    populateHolidaysList()
                    evaluateCurrentState()
                    Toast.makeText(this, "Added custom holiday: $name on $dateStr", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun populateHolidaysList() {
        layoutHolidayItems.removeAllViews()
        val customHolidays = settingsRepo.getCustomHolidays()
        val allHolidays = HolidayDatabase.getAllHolidays(customHolidays)

        val nextHoliday = HolidayDatabase.getNextUpcomingHoliday(Date(), customHolidays)
        tvNextHolidayBanner.text = "🎉 Next Holiday: ${nextHoliday.second} (${nextHoliday.first})"

        val inflater = LayoutInflater.from(this)
        for ((dateKey, name) in allHolidays) {
            val isCustom = customHolidays.containsKey(dateKey)
            val itemView = inflater.inflate(android.R.layout.simple_list_item_2, layoutHolidayItems, false)
            val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            text1.text = name + (if (isCustom) " (Custom)" else "")
            text1.setTextColor(ContextCompat.getColor(this, if (isCustom) R.color.amber_400 else R.color.text_white))
            text1.textSize = 13f

            text2.text = dateKey
            text2.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
            text2.textSize = 11f

            if (isCustom) {
                itemView.setOnLongClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Remove Custom Holiday")
                        .setMessage("Do you want to delete '$name' ($dateKey)?")
                        .setPositiveButton("Delete") { _, _ ->
                            settingsRepo.removeCustomHoliday(dateKey)
                            populateHolidaysList()
                            evaluateCurrentState()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            layoutHolidayItems.addView(itemView)
        }
    }

    private fun switchModeTab(mode: ShieldMode) {
        btnTabOpt1.setBackgroundResource(R.drawable.bg_tab_inactive)
        btnTabOpt1.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
        btnTabOpt2.setBackgroundResource(R.drawable.bg_tab_inactive)
        btnTabOpt2.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
        btnTabOpt3.setBackgroundResource(R.drawable.bg_tab_inactive)
        btnTabOpt3.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))

        layoutOption1.visibility = View.GONE
        layoutOption2.visibility = View.GONE
        layoutOption3.visibility = View.GONE

        when (mode) {
            ShieldMode.CELL_TOWER -> {
                btnTabOpt1.setBackgroundResource(R.drawable.bg_tab_active)
                btnTabOpt1.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                layoutOption1.visibility = View.VISIBLE
            }
            ShieldMode.NOTIFY_BASED -> {
                btnTabOpt2.setBackgroundResource(R.drawable.bg_tab_active)
                btnTabOpt2.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                layoutOption2.visibility = View.VISIBLE
            }
            ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE -> {
                btnTabOpt3.setBackgroundResource(R.drawable.bg_tab_active)
                btnTabOpt3.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                layoutOption3.visibility = View.VISIBLE
            }
        }
        evaluateCurrentState()
    }

    private fun evaluateCurrentState() {
        val config = settingsRepo.getConfig()

        if (!config.isProtectionEnabled) {
            tvCurrentStatus.text = "Status: Protection Shield Paused"
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.rose_500))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.rose_500))
            updateVolumeDisplay()
            return
        }

        if (config.takingLeaveToday) {
            tvCurrentStatus.text = "Status: Leave Today (Mute Paused)"
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
            updateVolumeDisplay()
            return
        }

        val customHolidays = settingsRepo.getCustomHolidays()
        val holidayCheck = HolidayDatabase.checkHoliday(Date(), config.selectedWeekdays, customHolidays)
        tvTodayStatusBadge.text = if (holidayCheck.first) "🌴 " + holidayCheck.second else "📚 Regular College Working Day"
        tvTodayStatusBadge.setTextColor(ContextCompat.getColor(this, if (holidayCheck.first) R.color.amber_400 else R.color.emerald_500))

        if (holidayCheck.first) {
            tvCurrentStatus.text = "Status: ${holidayCheck.second} (Loud)"
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
            updateVolumeDisplay()
            return
        }

        // Active working day check against custom college start and end times
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        if (nowTime >= config.collegeStartTime && nowTime <= config.collegeEndTime) {
            tvCurrentStatus.text = "Status: College Hours (Phone Muted)"
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
            progressVolume.progress = 0
            tvVolumePercentage.text = "0% (Muted)"
        } else {
            tvCurrentStatus.text = "Status: Outside College Hours (Normal)"
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
            updateVolumeDisplay()
        }
    }

    private fun updateVolumeDisplay() {
        val currentVol = audioController.currentVolume
        val maxVol = audioController.maxVolume
        val savedVol = audioController.savedVolume
        val pct = if (maxVol > 0) (currentVol * 100) / maxVol else 75
        progressVolume.progress = pct
        tvVolumePercentage.text = if (savedVol >= 0) "User Saved Vol: $savedVol / $maxVol ($pct%)" else "System Vol: $pct%"
        tvVolumePercentage.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
    }

    private fun updateTimeDisplays(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        val startFormatted = formatTime12H(startHour, startMin)
        val endFormatted = formatTime12H(endHour, endMin)
        tvStartTimeVal.text = "$startFormatted ✏️"
        tvEndTimeVal.text = "$endFormatted ✏️"
    }

    private fun formatTime12H(hour: Int, min: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%02d:%02d %s", h12, min, amPm)
    }

    private fun updatePowerSaveBannerUi(isPowerSave: Boolean, preference: ThemeModePreference) {
        when (preference) {
            ThemeModePreference.AUTO_BATTERY_SAVER -> {
                btnThemeAuto.setBackgroundResource(R.drawable.bg_tab_active)
                btnThemeAuto.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                btnThemeDark.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeDark.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
                btnThemeLight.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeLight.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))

                if (isPowerSave) {
                    tvPowerSaveIcon.text = "⚡"
                    tvPowerSaveTitle.text = "Battery Saver: ON • Dark Mode"
                    tvPowerSaveSub.text = "Power saving detected! Pitch black AMOLED theme active."
                    tvThemeModeBadge.text = "DARK 🌙"
                    tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
                } else {
                    tvPowerSaveIcon.text = "☀️"
                    tvPowerSaveTitle.text = "Battery Saver: OFF • Light Mode"
                    tvPowerSaveSub.text = "Normal power detected! Clean porcelain luxury theme active."
                    tvThemeModeBadge.text = "LIGHT ☀️"
                    tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                }
            }
            ThemeModePreference.FORCE_DARK -> {
                btnThemeAuto.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeAuto.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
                btnThemeDark.setBackgroundResource(R.drawable.bg_tab_active)
                btnThemeDark.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                btnThemeLight.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeLight.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))

                tvPowerSaveIcon.text = "🌙"
                tvPowerSaveTitle.text = "Forced Dark Mode Active"
                tvPowerSaveSub.text = "Pitch black high contrast theme (User Overridden)"
                tvThemeModeBadge.text = "DARK 🌙"
                tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            }
            ThemeModePreference.FORCE_LIGHT -> {
                btnThemeAuto.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeAuto.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
                btnThemeDark.setBackgroundResource(R.drawable.bg_tab_inactive)
                btnThemeDark.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
                btnThemeLight.setBackgroundResource(R.drawable.bg_tab_active)
                btnThemeLight.setTextColor(ContextCompat.getColor(this, android.R.color.black))

                tvPowerSaveIcon.text = "☀️"
                tvPowerSaveTitle.text = "Forced Light Mode Active"
                tvPowerSaveSub.text = "Clean daytime porcelain theme (User Overridden)"
                tvThemeModeBadge.text = "LIGHT ☀️"
                tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            }
        }
    }

    private fun handlePowerSaveModeChanged() {
        val config = settingsRepo.getConfig()
        val isPowerSave = PowerSaveThemeHelper.isPowerSaveModeActive(this)
        if (config.themePreference == ThemeModePreference.AUTO_BATTERY_SAVER) {
            PowerSaveThemeHelper.applyThemeToActivity(this, ThemeModePreference.AUTO_BATTERY_SAVER)
        }
        updatePowerSaveBannerUi(isPowerSave, config.themePreference)
    }

    private fun startProtectionService() {
        val serviceIntent = Intent(this, CampusGeofenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun refreshUi() {
        val hasDnd = audioController.hasDndPermission()
        btnDndPermission.visibility = if (hasDnd) View.GONE else View.VISIBLE
        tvRingerMode.text = "Mode: ${audioController.getCurrentRingerModeName()}"
        evaluateCurrentState()
    }

    private fun updateUiState(status: String, distance: Double) {
        tvCurrentStatus.text = "Status: $status"
        tvRingerMode.text = "Mode: ${audioController.getCurrentRingerModeName()}"

        if (status.contains("Inside", ignoreCase = true) || audioController.isCampusMuted) {
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
            progressVolume.progress = 0
            tvVolumePercentage.text = "0% (Muted)"
        } else {
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
            updateVolumeDisplay()
        }
    }

    // Step 1: Request Foreground permissions (Fix 6)
    private fun checkForegroundPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
        }
    }

    // Step 2: Request Background Location separately on Android 10+ (Fix 6)
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                    .setTitle("Background Location Required")
                    .setMessage("To auto-mute your phone when arriving at college campus while your phone is locked or in your pocket, please select 'Allow all the time' on the next screen.")
                    .setPositiveButton("Continue") { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 102)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(this, "Background location is already granted!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Background location is granted via location permission.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            // Once foreground is granted, check if background location is needed
            val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    btnBgLocationPermission.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(CampusGeofenceService.ACTION_STATUS_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }

        val psFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        registerReceiver(powerSaveReceiver, psFilter)

        handlePowerSaveModeChanged()
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (_: IllegalArgumentException) {}
        try {
            unregisterReceiver(powerSaveReceiver)
        } catch (_: IllegalArgumentException) {}
    }
}
