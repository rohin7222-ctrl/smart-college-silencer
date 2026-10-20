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
    private lateinit var btnNotificationPermission: Button

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                handlePowerSaveModeChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsRepo = AppSettingsRepository(this)
        val initialConfig = settingsRepo.getConfig()
        PowerSaveThemeHelper.applyThemeToActivity(this, initialConfig.themePreference)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioController = AudioModeController(this)

        initViews()
        setupBottomNav()
        setupListeners()
        populateHolidaysList()

        checkForegroundPermissions()

        val config = settingsRepo.getConfig()
        if (config.isProtectionEnabled) {
            CampusAlarmScheduler.scheduleAllAlarms(this, config)
        }

        updateProtectionButtonUi(config.isProtectionEnabled)
        updateWeekdayChipsUi(config.selectedWeekdays)
        updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, config.collegeEndHour, config.collegeEndMinute)
        switchMuteOnlyAtCollege.isChecked = config.muteOnlyAtCollege
        switchLeaveToday.isChecked = config.takingLeaveToday

        tvCollegeTower.text = "Cell ID #${config.collegeCellTowerId}"
        tvConnectedTower.text = "Active Tower: #${CellTowerHelper.getCurrentCellTowerId(this)}"

        switchModeTab(config.selectedMode)
        evaluateCurrentState()
        refreshUi()
    }

    private fun initViews() {
        tvHeaderDate = findViewById(R.id.tv_header_date)
        tvBadgeStatus = findViewById(R.id.tv_badge_status)

        val headerFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        tvHeaderDate.text = headerFormat.format(Date())

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
        btnNotificationPermission = findViewById(R.id.btn_notification_permission)

        // Check if today is Saturday to show Saturday Check Card
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            val config = settingsRepo.getConfig()
            if (!config.takingLeaveToday && !config.selectedWeekdays.contains(Calendar.SATURDAY)) {
                cardSaturdayCheck.visibility = View.VISIBLE
            } else {
                cardSaturdayCheck.visibility = View.GONE
            }
        } else {
            cardSaturdayCheck.visibility = View.GONE
        }
    }

    private fun setupBottomNav() {
        tabNavDashboard.setOnClickListener { navigateToPage(0) }
        tabNavModes.setOnClickListener { navigateToPage(1) }
        tabNavHolidays.setOnClickListener { navigateToPage(2) }
        tabNavSystem.setOnClickListener { navigateToPage(3) }
    }

    private fun navigateToPage(index: Int) {
        pageDashboard.visibility = if (index == 0) View.VISIBLE else View.GONE
        pageModes.visibility = if (index == 1) View.VISIBLE else View.GONE
        pageHolidays.visibility = if (index == 2) View.VISIBLE else View.GONE
        pageSystem.visibility = if (index == 3) View.VISIBLE else View.GONE

        updateTabAppearance(tabNavDashboard, tvNavDashboard, index == 0)
        updateTabAppearance(tabNavModes, tvNavModes, index == 1)
        updateTabAppearance(tabNavHolidays, tvNavHolidays, index == 2)
        updateTabAppearance(tabNavSystem, tvNavSystem, index == 3)
    }

    private fun updateTabAppearance(tab: LinearLayout, label: TextView, isSelected: Boolean) {
        if (isSelected) {
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
                Toast.makeText(this, "🚀 Protection Shield Started! Alarms active.", Toast.LENGTH_SHORT).show()
            } else {
                CampusAlarmScheduler.cancelAllAlarms(this)
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

        // Emergency Unmute Button
        btnEmergencyUnmute.setOnClickListener {
            audioController.restoreNormalRinger()
            refreshUi()
            Toast.makeText(this, "🔊 Emergency Unmute Executed! Full volume restored.", Toast.LENGTH_LONG).show()
        }

        // Quick Test Buttons
        btnTestMute.setOnClickListener {
            val config = settingsRepo.getConfig()
            audioController.muteForCampus(config.vibrateOnly)
            refreshUi()
            Toast.makeText(this, "🔇 Tested Mute: Volume set to 0", Toast.LENGTH_SHORT).show()
        }

        btnTestUnmute.setOnClickListener {
            audioController.restoreNormalRinger()
            refreshUi()
            Toast.makeText(this, "🔊 Tested Unmute: Volume restored", Toast.LENGTH_SHORT).show()
        }

        // Modes Tab Switching
        btnTabOpt1.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.CELL_TOWER)
            switchModeTab(ShieldMode.CELL_TOWER)
        }
        btnTabOpt2.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.NOTIFY_BASED)
            switchModeTab(ShieldMode.NOTIFY_BASED)
        }
        btnTabOpt3.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE)
            switchModeTab(ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE)
        }

        // 7 Weekday Toggle Chips
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

        // Mute only when at college location switch
        switchMuteOnlyAtCollege.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setMuteOnlyAtCollege(isChecked)
            if (isChecked && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showPermissionTransparencyDialog(
                    titleEn = "📍 Location Guard Permission",
                    titleTa = "வளாக இருப்பிட அனுமதி",
                    descEn = "Required to verify you are physically inside the campus radius before muting on schedule.",
                    descTa = "அலார நேரத்தில் நீங்கள் கல்லூரி வளாகத்திற்குள் இருப்பதை உறுதி செய்து சைலன்ட் செய்ய இது பயன்படுகிறது."
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        101
                    )
                }
            } else {
                Toast.makeText(this, if (isChecked) "Location Guard: Will check campus GPS before muting" else "Mutes by schedule directly", Toast.LENGTH_SHORT).show()
            }
        }

        // Option 1 Lock Current Tower
        btnLockCurrentTower.setOnClickListener {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            settingsRepo.setCollegeCellTowerId(currentTower)
            tvCollegeTower.text = "Cell ID #$currentTower"
            Toast.makeText(this, "Saved Tower #$currentTower as College Campus!", Toast.LENGTH_LONG).show()
            evaluateCurrentState()
        }

        // Option 2 Test Morning Prompt
        btnTestMorningPrompt.setOnClickListener {
            val receiver = CampusAlarmReceiver()
            val intent = Intent(this, CampusAlarmReceiver::class.java).apply {
                action = CampusAlarmReceiver.ACTION_MORNING_PROMPT
            }
            receiver.onReceive(this, intent)
            Toast.makeText(this, "Morning Prompt Notification Triggered! Check your notification shade.", Toast.LENGTH_SHORT).show()
        }

        // Add Custom Holiday Button
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

        // Transparency-backed Permission Buttons
        btnDndPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissionTransparencyDialog(
                    titleEn = "🔕 Do Not Disturb (DND) Access",
                    titleTa = "அமைதி முறை அனுமதி",
                    descEn = "Allows Smart Campus Silencer to automatically silence ringtones and media when college starts, and safely restore sound when college ends.",
                    descTa = "கல்லூரி நேரத்தில் உங்கள் போனை தானாக சைலன்ட் செய்யவும், வகுப்பு முடிந்ததும் மீண்டும் சத்தமாக ஒலிக்கவும் இந்த அனுமதி தேவைப்படுகிறது."
                ) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            }
        }

        btnExactAlarmPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                showPermissionTransparencyDialog(
                    titleEn = "⏰ Exact Alarm Schedule Access",
                    titleTa = "துல்லிய அலார அனுமதி",
                    descEn = "Allows the app to trigger mute and unmute precisely at your college start and end times with zero battery drain.",
                    descTa = "வகுப்பு தொடங்கும் மற்றும் முடியும் சரியான நேரத்தில் அலாரத்தை இயக்கி சைலன்ட் / அன்சைலன்ட் செய்ய இது தேவைப்படுகிறது."
                ) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            } else {
                Toast.makeText(this, "Exact Alarms are supported natively on this Android version.", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatteryOptimization.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissionTransparencyDialog(
                    titleEn = "⚡ Battery Saver Exemption",
                    titleTa = "பேட்டரி சேமிப்பு விலக்கு",
                    descEn = "Prevents Android from putting scheduled campus alarms to sleep while your phone is locked.",
                    descTa = "தொலைபேசி லாக் செய்யப்பட்டிருந்தாலும் கல்லூரி அலாரங்கள் சரியான நேரத்தில் செயல்பட இது உதவுகிறது."
                ) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        }

        btnNotificationPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                showPermissionTransparencyDialog(
                    titleEn = "🔔 Notification Alerts Access",
                    titleTa = "அறிவிப்புகள் அனுமதி",
                    descEn = "Allows interactive morning prompts asking if you are attending college today, and real-time shield status alerts.",
                    descTa = "காலை கல்லூரி புறப்பாடு சரிபார்ப்பு மற்றும் ஆப் நிலவர அறிவிப்புகளை திரையில் காட்ட இது பயன்படுகிறது."
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        103
                    )
                }
            } else {
                Toast.makeText(this, "Notifications enabled natively.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionTransparencyDialog(
        titleEn: String,
        titleTa: String,
        descEn: String,
        descTa: String,
        onGrant: () -> Unit
    ) {
        val message = """
$descEn

$descTa

─────────────────────────────
🔒 100% Offline & Private:
This app works fully offline and collects no data.
இந்த ஆப் முற்றிலும் ஆஃப்லைனில் செயல்படுகிறது மற்றும் எந்த ஒரு தகவலையும் சேகரிப்பதில்லை.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("$titleEn\n$titleTa")
            .setMessage(message)
            .setPositiveButton("Continue / தொடரவும்") { _, _ ->
                onGrant()
            }
            .setNegativeButton("Cancel / ரத்து", null)
            .show()
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
            text1.textSize = 14f

            text2.text = "Date: $dateKey"
            text2.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
            text2.textSize = 12f

            if (isCustom) {
                itemView.setOnLongClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Holiday")
                        .setMessage("Remove '$name' from holiday schedule?")
                        .setPositiveButton("Delete") { _, _ ->
                            settingsRepo.removeCustomHoliday(dateKey)
                            populateHolidaysList()
                            evaluateCurrentState()
                            Toast.makeText(this, "Deleted custom holiday $dateKey", Toast.LENGTH_SHORT).show()
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

    private fun refreshUi() {
        val hasDnd = audioController.hasDndPermission()
        btnDndPermission.visibility = if (hasDnd) View.GONE else View.VISIBLE

        val canExact = CampusAlarmScheduler.canScheduleExactAlarms(this)
        btnExactAlarmPermission.visibility = if (canExact) View.GONE else View.VISIBLE

        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        btnNotificationPermission.visibility = if (hasNotif) View.GONE else View.VISIBLE

        tvRingerMode.text = "Mode: ${audioController.getCurrentRingerModeName()}"
        evaluateCurrentState()
    }

    // Step 1: Request Foreground permissions with clean transparency
    private fun checkForegroundPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        val psFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        registerReceiver(powerSaveReceiver, psFilter)

        handlePowerSaveModeChanged()
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(powerSaveReceiver)
        } catch (_: IllegalArgumentException) {}
    }
}
