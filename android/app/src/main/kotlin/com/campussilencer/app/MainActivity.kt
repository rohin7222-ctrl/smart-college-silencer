package com.campussilencer.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.HolidayDatabase
import com.campussilencer.app.data.ShieldMode
import com.campussilencer.app.data.ThemeModePreference
import com.campussilencer.app.service.CampusGeofenceService
import com.campussilencer.app.service.CellTowerHelper
import com.campussilencer.app.ui.PowerSaveThemeHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var audioController: AudioModeController
    private lateinit var settingsRepo: AppSettingsRepository

    // Top Header & Status views
    private lateinit var tvStatus: TextView
    private lateinit var tvRingerMode: TextView
    private lateinit var tvEngineLabel: TextView
    private lateinit var tvHolidayStatusBadge: TextView
    private lateinit var tvVolumePercentage: TextView
    private lateinit var progressVolume: ProgressBar
    private lateinit var imgModeIcon: ImageView

    // Battery Saver & Adaptive Theme Views
    private lateinit var layoutPowerSaveBanner: LinearLayout
    private lateinit var tvPowerSaveIcon: TextView
    private lateinit var tvPowerSaveTitle: TextView
    private lateinit var tvPowerSaveSub: TextView
    private lateinit var tvThemeModeBadge: TextView
    private lateinit var btnThemeAuto: Button
    private lateinit var btnThemeDark: Button
    private lateinit var btnThemeLight: Button

    // 3 Mode Selector Tabs
    private lateinit var btnTabOpt1: Button
    private lateinit var btnTabOpt2: Button
    private lateinit var btnTabOpt3: Button
    private lateinit var layoutOption1: LinearLayout
    private lateinit var layoutOption2: LinearLayout
    private lateinit var layoutOption3: LinearLayout

    // Option 1 (Cell Tower) Views
    private lateinit var tvConnectedTower: TextView
    private lateinit var tvCollegeTower: TextView
    private lateinit var btnLockCurrentTower: Button

    // Option 3 (Holiday & Timings) Views
    private lateinit var tvDetectedRegion: TextView
    private lateinit var tvHolidayCountInfo: TextView
    private lateinit var tvUpcomingHolidayInfo: TextView
    private lateinit var btnViewHolidays: Button
    private lateinit var btnPickStartTime: LinearLayout
    private lateinit var btnPickEndTime: LinearLayout
    private lateinit var tvStartTimeVal: TextView
    private lateinit var tvEndTimeVal: TextView

    // Universal Controls & Actions
    private lateinit var layoutLeaveCard: LinearLayout
    private lateinit var switchLeaveToday: Switch
    private lateinit var switchVibrateMode: Switch
    private lateinit var btnToggleDndPermission: Button
    private lateinit var btnStartService: Button
    private lateinit var btnTestMute: Button
    private lateinit var btnTestUnmute: Button
    private lateinit var btnEmergencyUnmute: Button

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
        super.onCreate(savedInstanceState)

        settingsRepo = AppSettingsRepository(this)
        // Ensure matching theme is active immediately on launch
        PowerSaveThemeHelper.applyTheme(this, settingsRepo.getConfig().themePreference)

        setContentView(R.layout.activity_main)

        audioController = AudioModeController(this)

        initViews()
        setupListeners()
        checkPermissions()
        refreshUi()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tv_current_status)
        tvRingerMode = findViewById(R.id.tv_ringer_mode)
        tvEngineLabel = findViewById(R.id.tv_engine_label)
        tvHolidayStatusBadge = findViewById(R.id.tv_holiday_status_badge)
        tvVolumePercentage = findViewById(R.id.tv_volume_percentage)
        progressVolume = findViewById(R.id.progress_volume)
        imgModeIcon = findViewById(R.id.img_mode_icon)

        // Battery Saver & Theme views
        layoutPowerSaveBanner = findViewById(R.id.layout_power_save_banner)
        tvPowerSaveIcon = findViewById(R.id.tv_power_save_icon)
        tvPowerSaveTitle = findViewById(R.id.tv_power_save_title)
        tvPowerSaveSub = findViewById(R.id.tv_power_save_sub)
        tvThemeModeBadge = findViewById(R.id.tv_theme_mode_badge)
        btnThemeAuto = findViewById(R.id.btn_theme_auto)
        btnThemeDark = findViewById(R.id.btn_theme_dark)
        btnThemeLight = findViewById(R.id.btn_theme_light)

        // Tabs
        btnTabOpt1 = findViewById(R.id.btn_tab_opt_1)
        btnTabOpt2 = findViewById(R.id.btn_tab_opt_2)
        btnTabOpt3 = findViewById(R.id.btn_tab_opt_3)
        layoutOption1 = findViewById(R.id.layout_option_1)
        layoutOption2 = findViewById(R.id.layout_option_2)
        layoutOption3 = findViewById(R.id.layout_option_3)

        // Option 1
        tvConnectedTower = findViewById(R.id.tv_connected_tower)
        tvCollegeTower = findViewById(R.id.tv_college_tower)
        btnLockCurrentTower = findViewById(R.id.btn_lock_current_tower)

        // Option 3
        tvDetectedRegion = findViewById(R.id.tv_detected_region)
        tvHolidayCountInfo = findViewById(R.id.tv_holiday_count_info)
        tvUpcomingHolidayInfo = findViewById(R.id.tv_upcoming_holiday_info)
        btnViewHolidays = findViewById(R.id.btn_view_holidays)
        btnPickStartTime = findViewById(R.id.btn_pick_start_time)
        btnPickEndTime = findViewById(R.id.btn_pick_end_time)
        tvStartTimeVal = findViewById(R.id.tv_start_time_val)
        tvEndTimeVal = findViewById(R.id.tv_end_time_val)

        // Universal Controls
        layoutLeaveCard = findViewById(R.id.layout_leave_card)
        switchLeaveToday = findViewById(R.id.switch_leave_today)
        switchVibrateMode = findViewById(R.id.switch_vibrate_mode)
        btnToggleDndPermission = findViewById(R.id.btn_dnd_permission)
        btnStartService = findViewById(R.id.btn_start_service)
        btnTestMute = findViewById(R.id.btn_test_mute)
        btnTestUnmute = findViewById(R.id.btn_test_unmute)
        btnEmergencyUnmute = findViewById(R.id.btn_emergency_unmute)

        val config = settingsRepo.getConfig()
        switchLeaveToday.isChecked = config.takingLeaveToday
        switchVibrateMode.isChecked = config.vibrateOnly
        updateLeaveCardStyle(config.takingLeaveToday)

        val activeTower = CellTowerHelper.getCurrentCellTowerId(this)
        tvConnectedTower.text = "Cell ID #$activeTower"
        tvCollegeTower.text = "Cell ID #${config.collegeCellTowerId}"

        updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, config.collegeEndHour, config.collegeEndMinute)

        // Holiday engine setup
        val holidayCheck = HolidayDatabase.checkHoliday()
        if (holidayCheck.first) {
            tvHolidayStatusBadge.text = "🌴 " + holidayCheck.second
            tvHolidayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
        } else {
            tvHolidayStatusBadge.text = "Working Day"
            tvHolidayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
        }

        val nextHoliday = HolidayDatabase.getNextUpcomingHoliday()
        tvUpcomingHolidayInfo.text = "🎉 Next Gov Holiday: ${nextHoliday.second} (${nextHoliday.first})"

        // Set active mode tab & power save UI
        switchModeTab(config.selectedMode)
        val isPowerSave = PowerSaveThemeHelper.isPowerSaveModeActive(this)
        updatePowerSaveBannerUi(isPowerSave, config.themePreference)
    }

    private fun setupListeners() {
        // Theme selector buttons
        btnThemeAuto.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.AUTO_BATTERY_SAVER)
            PowerSaveThemeHelper.applyTheme(this, ThemeModePreference.AUTO_BATTERY_SAVER)
            val isPs = PowerSaveThemeHelper.isPowerSaveModeActive(this)
            updatePowerSaveBannerUi(isPs, ThemeModePreference.AUTO_BATTERY_SAVER)
            val msg = if (isPs) "⚡ Battery Saver ON → Dark Mode Active" else "☀️ Battery Saver OFF → Light Mode Active"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnThemeDark.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.FORCE_DARK)
            PowerSaveThemeHelper.applyTheme(this, ThemeModePreference.FORCE_DARK)
            updatePowerSaveBannerUi(PowerSaveThemeHelper.isPowerSaveModeActive(this), ThemeModePreference.FORCE_DARK)
            Toast.makeText(this, "🌙 AMOLED Dark Mode Activated", Toast.LENGTH_SHORT).show()
        }

        btnThemeLight.setOnClickListener {
            settingsRepo.setThemePreference(ThemeModePreference.FORCE_LIGHT)
            PowerSaveThemeHelper.applyTheme(this, ThemeModePreference.FORCE_LIGHT)
            updatePowerSaveBannerUi(PowerSaveThemeHelper.isPowerSaveModeActive(this), ThemeModePreference.FORCE_LIGHT)
            Toast.makeText(this, "☀️ Luxury Light Mode Activated", Toast.LENGTH_SHORT).show()
        }

        // Tab switching between Option 1, 2, and 3
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
            Toast.makeText(this, "Option 3 Selected: One-Time Location & Auto-Holiday Engine", Toast.LENGTH_SHORT).show()
        }

        // View All Government Holidays Dialog
        btnViewHolidays.setOnClickListener {
            val sb = StringBuilder()
            HolidayDatabase.STATE_HOLIDAYS.forEach { (date, name) ->
                sb.append("• ").append(date).append(": ").append(name).append("\n\n")
            }
            AlertDialog.Builder(this)
                .setTitle("📅 Official Tamil Nadu & National Gov Holidays")
                .setMessage(sb.toString().trimEnd())
                .setPositiveButton("Close", null)
                .show()
        }

        // Option 3: Time Pickers
        btnPickStartTime.setOnClickListener {
            val config = settingsRepo.getConfig()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                settingsRepo.setCollegeTimes(hourOfDay, minute, config.collegeEndHour, config.collegeEndMinute)
                updateTimeDisplays(hourOfDay, minute, config.collegeEndHour, config.collegeEndMinute)
                Toast.makeText(this, "College Start Time updated!", Toast.LENGTH_SHORT).show()
                evaluateCurrentState()
            }, config.collegeStartHour, config.collegeStartMinute, false).show()
        }

        btnPickEndTime.setOnClickListener {
            val config = settingsRepo.getConfig()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                settingsRepo.setCollegeTimes(config.collegeStartHour, config.collegeStartMinute, hourOfDay, minute)
                updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, hourOfDay, minute)
                Toast.makeText(this, "College End Time updated!", Toast.LENGTH_SHORT).show()
                evaluateCurrentState()
            }, config.collegeEndHour, config.collegeEndMinute, false).show()
        }

        // Option 1: Lock Current Tower
        btnLockCurrentTower.setOnClickListener {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            settingsRepo.setCollegeCellTowerId(currentTower)
            tvCollegeTower.text = "Cell ID #$currentTower"
            Toast.makeText(this, "✅ Saved Tower #$currentTower as College Campus! Zero GPS muting ready.", Toast.LENGTH_LONG).show()
            evaluateCurrentState()
        }

        // Universal Leave Override
        switchLeaveToday.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setTakingLeaveToday(isChecked)
            updateLeaveCardStyle(isChecked)
            Toast.makeText(this, if (isChecked) "🏖️ Leave Mode: Phone will NEVER mute today!" else "Normal college monitoring active.", Toast.LENGTH_SHORT).show()
            evaluateCurrentState()
        }

        switchVibrateMode.setOnCheckedChangeListener { _, isChecked ->
            val config = settingsRepo.getConfig().copy(vibrateOnly = isChecked)
            settingsRepo.saveConfig(config)
            Toast.makeText(this, "Profile: " + (if (isChecked) "Vibrate" else "Silent"), Toast.LENGTH_SHORT).show()
        }

        btnToggleDndPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }

        btnStartService.setOnClickListener {
            startProtectionService()
        }

        btnTestMute.setOnClickListener {
            if (audioController.muteForCampus(switchVibrateMode.isChecked)) {
                progressVolume.progress = 0
                tvVolumePercentage.text = "0% (Muted)"
                tvVolumePercentage.setTextColor(ContextCompat.getColor(this, R.color.amber_500))
                Toast.makeText(this, "Test Success: Phone Muted for College!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }

        btnTestUnmute.setOnClickListener {
            if (audioController.restoreNormalRinger()) {
                progressVolume.progress = 80
                tvVolumePercentage.text = "80% (Active)"
                tvVolumePercentage.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                Toast.makeText(this, "Test Success: Normal Ringer Restored for Home!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }

        // Emergency 1-Hour Sound Restore
        btnEmergencyUnmute.setOnClickListener {
            audioController.restoreNormalRinger()
            progressVolume.progress = 80
            tvVolumePercentage.text = "80% (Restored)"
            tvVolumePercentage.setTextColor(ContextCompat.getColor(this, R.color.cyan_400))
            tvStatus.text = "Status: Emergency 1-Hour Sound Restored"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.cyan_400))
            Toast.makeText(this, "⏰ Ringer restored for 1 Hour! You will not miss any calls.", Toast.LENGTH_LONG).show()
            refreshUi()
        }
    }

    private fun handlePowerSaveModeChanged() {
        val config = settingsRepo.getConfig()
        val isPowerSave = PowerSaveThemeHelper.isPowerSaveModeActive(this)
        if (config.themePreference == ThemeModePreference.AUTO_BATTERY_SAVER) {
            PowerSaveThemeHelper.applyTheme(this, ThemeModePreference.AUTO_BATTERY_SAVER)
        }
        updatePowerSaveBannerUi(isPowerSave, config.themePreference)
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
                    tvPowerSaveSub.text = "Power saving detected! Pitch black AMOLED theme enabled."
                    tvThemeModeBadge.text = "DARK 🌙"
                    tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
                } else {
                    tvPowerSaveIcon.text = "☀️"
                    tvPowerSaveTitle.text = "Battery Saver: OFF • Light Mode"
                    tvPowerSaveSub.text = "Normal power detected! Luxury porcelain light theme enabled."
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
                tvPowerSaveSub.text = "Luxury clean porcelain daytime theme (User Overridden)"
                tvThemeModeBadge.text = "LIGHT ☀️"
                tvThemeModeBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            }
        }
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
                tvEngineLabel.text = "Active Engine: Option 1 (Tower Shield)"
            }
            ShieldMode.NOTIFY_BASED -> {
                btnTabOpt2.setBackgroundResource(R.drawable.bg_tab_active)
                btnTabOpt2.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                layoutOption2.visibility = View.VISIBLE
                tvEngineLabel.text = "Active Engine: Option 2 (Morning Notify)"
            }
            ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE -> {
                btnTabOpt3.setBackgroundResource(R.drawable.bg_tab_active)
                btnTabOpt3.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                layoutOption3.visibility = View.VISIBLE
                tvEngineLabel.text = "Active Engine: Option 3 (Auto Holiday & Hours)"
            }
        }
        evaluateCurrentState()
    }

    private fun updateLeaveCardStyle(isLeave: Boolean) {
        if (isLeave) {
            layoutLeaveCard.setBackgroundResource(R.drawable.bg_leave_pill_active)
        } else {
            layoutLeaveCard.setBackgroundResource(R.drawable.bg_glass_card)
        }
    }

    private fun evaluateCurrentState() {
        val config = settingsRepo.getConfig()

        if (config.takingLeaveToday) {
            tvStatus.text = "Status: Leave Mode (Mute Paused)"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
            progressVolume.progress = 80
            tvVolumePercentage.text = "80% (Loud)"
            return
        }

        when (config.selectedMode) {
            ShieldMode.ONE_TIME_HOLIDAY_SCHEDULE -> {
                val holiday = HolidayDatabase.checkHoliday()
                if (holiday.first) {
                    tvStatus.text = "Status: ${holiday.second} (Loud)"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                    imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
                    progressVolume.progress = 80
                    tvVolumePercentage.text = "80% (Normal)"
                    return
                }

                // Working day check against custom college start and end times
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                if (nowTime >= config.collegeStartTime && nowTime <= config.collegeEndTime) {
                    tvStatus.text = "Status: College Hours (Phone Muted)"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
                    imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
                    progressVolume.progress = 0
                    tvVolumePercentage.text = "0% (Muted)"
                } else {
                    tvStatus.text = "Status: Outside College Hours (Normal)"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                    imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
                    progressVolume.progress = 80
                    tvVolumePercentage.text = "80% (Normal)"
                }
            }

            ShieldMode.CELL_TOWER -> {
                val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
                tvConnectedTower.text = "Cell ID #$currentTower"
                if (currentTower == config.collegeCellTowerId) {
                    tvStatus.text = "Status: College Tower Matched (Muted)"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
                    imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
                    progressVolume.progress = 0
                    tvVolumePercentage.text = "0% (Muted)"
                } else {
                    tvStatus.text = "Status: Outside College (Home Safe)"
                    tvStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                    imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
                    progressVolume.progress = 80
                    tvVolumePercentage.text = "80% (Normal)"
                }
            }

            ShieldMode.NOTIFY_BASED -> {
                tvStatus.text = "Status: Smart Notify Active"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
                imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
                progressVolume.progress = 80
                tvVolumePercentage.text = "80% (Normal)"
            }
        }
    }

    private fun startProtectionService() {
        val serviceIntent = Intent(this, CampusGeofenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "🚀 Smart Protection Shield Started", Toast.LENGTH_SHORT).show()
    }

    private fun refreshUi() {
        val hasDnd = audioController.hasDndPermission()
        btnToggleDndPermission.visibility = if (hasDnd) View.GONE else View.VISIBLE
        tvRingerMode.text = "Current Mode: ${audioController.getCurrentRingerModeName()}"
        evaluateCurrentState()
    }

    private fun updateUiState(status: String, distance: Double) {
        tvStatus.text = "Status: $status"
        val ringerName = audioController.getCurrentRingerModeName()
        tvRingerMode.text = "Current Mode: $ringerName"

        if (status.contains("Inside", ignoreCase = true) || audioController.isCampusMuted) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
            progressVolume.progress = 0
            tvVolumePercentage.text = "0% (Muted)"
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.emerald_500))
            progressVolume.progress = 80
            tvVolumePercentage.text = "80% (Normal)"
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
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
