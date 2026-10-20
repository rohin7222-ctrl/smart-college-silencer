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
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.HolidayDatabase
import com.campussilencer.app.data.ShieldMode
import com.campussilencer.app.service.CampusGeofenceService
import com.campussilencer.app.service.CellTowerHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var audioController: AudioModeController
    private lateinit var settingsRepo: AppSettingsRepository

    // Top status views
    private lateinit var tvStatus: TextView
    private lateinit var tvRingerMode: TextView
    private lateinit var tvEngineLabel: TextView
    private lateinit var tvHolidayStatusBadge: TextView
    private lateinit var tvVolumePercentage: TextView
    private lateinit var progressVolume: ProgressBar
    private lateinit var imgModeIcon: ImageView

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
    private lateinit var btnPickStartTime: LinearLayout
    private lateinit var btnPickEndTime: LinearLayout
    private lateinit var tvStartTimeVal: TextView
    private lateinit var tvEndTimeVal: TextView

    // Universal Controls
    private lateinit var layoutLeaveCard: LinearLayout
    private lateinit var switchLeaveToday: Switch
    private lateinit var switchVibrateMode: Switch
    private lateinit var btnToggleDndPermission: Button
    private lateinit var btnStartService: Button
    private lateinit var btnTestMute: Button
    private lateinit var btnTestUnmute: Button

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CampusGeofenceService.ACTION_STATUS_UPDATED) {
                val status = intent.getStringExtra(CampusGeofenceService.EXTRA_STATUS) ?: "Unknown"
                val distance = intent.getDoubleExtra(CampusGeofenceService.EXTRA_DISTANCE, 0.0)
                updateUiState(status, distance)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioController = AudioModeController(this)
        settingsRepo = AppSettingsRepository(this)

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
        btnPickStartTime = findViewById(R.id.btn_pick_start_time)
        btnPickEndTime = findViewById(R.id.btn_pick_end_time)
        tvStartTimeVal = findViewById(R.id.tv_start_time_val)
        tvEndTimeVal = findViewById(R.id.tv_end_time_val)

        // Universal
        layoutLeaveCard = findViewById(R.id.layout_leave_card)
        switchLeaveToday = findViewById(R.id.switch_leave_today)
        switchVibrateMode = findViewById(R.id.switch_vibrate_mode)
        btnToggleDndPermission = findViewById(R.id.btn_dnd_permission)
        btnStartService = findViewById(R.id.btn_start_service)
        btnTestMute = findViewById(R.id.btn_test_mute)
        btnTestUnmute = findViewById(R.id.btn_test_unmute)

        val config = settingsRepo.getConfig()
        switchLeaveToday.isChecked = config.takingLeaveToday
        switchVibrateMode.isChecked = config.vibrateOnly
        updateLeaveCardStyle(config.takingLeaveToday)

        val activeTower = CellTowerHelper.getCurrentCellTowerId(this)
        tvConnectedTower.text = "Cell ID #$activeTower"
        tvCollegeTower.text = "Cell ID #${config.collegeCellTowerId}"

        updateTimeDisplays(config.collegeStartHour, config.collegeStartMinute, config.collegeEndHour, config.collegeEndMinute)

        // Display current Holiday status for Option 3
        val holidayCheck = HolidayDatabase.checkHoliday()
        if (holidayCheck.first) {
            tvHolidayStatusBadge.text = "🌴 " + holidayCheck.second
            tvHolidayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
        } else {
            tvHolidayStatusBadge.text = "Working Day"
            tvHolidayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_500))
        }

        // Set active mode tab
        switchModeTab(config.selectedMode)
    }

    private fun setupListeners() {
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
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (_: IllegalArgumentException) {}
    }
}
