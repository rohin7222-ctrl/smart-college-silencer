package com.campussilencer.app

import android.Manifest
import android.app.NotificationManager
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
    private lateinit var tvDistance: TextView
    private lateinit var tvWifiStatus: TextView
    private lateinit var tvVolumePercentage: TextView
    private lateinit var progressVolume: ProgressBar
    private lateinit var imgModeIcon: ImageView

    // Mode Selector Tabs
    private lateinit var btnTabModeA: Button
    private lateinit var btnTabModeB: Button
    private lateinit var layoutPlanA: LinearLayout
    private lateinit var layoutPlanB: LinearLayout

    // Plan A Views (Cell Tower)
    private lateinit var tvConnectedTower: TextView
    private lateinit var tvCollegeTower: TextView
    private lateinit var btnLockCurrentTower: Button

    // Universal Overrides
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
        tvDistance = findViewById(R.id.tv_campus_distance)
        tvWifiStatus = findViewById(R.id.tv_wifi_status)
        tvVolumePercentage = findViewById(R.id.tv_volume_percentage)
        progressVolume = findViewById(R.id.progress_volume)
        imgModeIcon = findViewById(R.id.img_mode_icon)

        btnTabModeA = findViewById(R.id.btn_tab_mode_a)
        btnTabModeB = findViewById(R.id.btn_tab_mode_b)
        layoutPlanA = findViewById(R.id.layout_plan_a)
        layoutPlanB = findViewById(R.id.layout_plan_b)

        tvConnectedTower = findViewById(R.id.tv_connected_tower)
        tvCollegeTower = findViewById(R.id.tv_college_tower)
        btnLockCurrentTower = findViewById(R.id.btn_lock_current_tower)

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

        // Set initial tab selection
        switchModeTab(config.selectedMode)
    }

    private fun setupListeners() {
        // Tab switching between Plan A and Plan B
        btnTabModeA.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.CELL_TOWER)
            switchModeTab(ShieldMode.CELL_TOWER)
            Toast.makeText(this, "Plan A Active: Mobile Cell Tower Shield (Zero GPS)", Toast.LENGTH_SHORT).show()
        }

        btnTabModeB.setOnClickListener {
            settingsRepo.setSelectedMode(ShieldMode.TIMETABLE)
            switchModeTab(ShieldMode.TIMETABLE)
            Toast.makeText(this, "Plan B Active: Smart Timetable (08:45 AM - 04:30 PM)", Toast.LENGTH_SHORT).show()
        }

        // Plan A: Lock Current Tower
        btnLockCurrentTower.setOnClickListener {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            settingsRepo.setCollegeCellTowerId(currentTower)
            tvCollegeTower.text = "Cell ID #$currentTower"
            Toast.makeText(this, "✅ Saved Tower #$currentTower as College Campus! Zero GPS ready.", Toast.LENGTH_LONG).show()
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

    private fun switchModeTab(mode: ShieldMode) {
        if (mode == ShieldMode.CELL_TOWER) {
            btnTabModeA.setBackgroundResource(R.drawable.bg_tab_active)
            btnTabModeA.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            btnTabModeB.setBackgroundResource(R.drawable.bg_tab_inactive)
            btnTabModeB.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
            layoutPlanA.visibility = View.VISIBLE
            layoutPlanB.visibility = View.GONE
            tvDistance.text = "Active Engine: Plan A (Cell Tower)"
            tvWifiStatus.text = "Zero GPS • 100% Privacy"
        } else {
            btnTabModeB.setBackgroundResource(R.drawable.bg_tab_active)
            btnTabModeB.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            btnTabModeA.setBackgroundResource(R.drawable.bg_tab_inactive)
            btnTabModeA.setTextColor(ContextCompat.getColor(this, R.color.text_slate_400))
            layoutPlanB.visibility = View.VISIBLE
            layoutPlanA.visibility = View.GONE
            tvDistance.text = "Active Engine: Plan B (Timetable)"
            tvWifiStatus.text = "Auto-Unmute at 04:30 PM"
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

        if (config.selectedMode == ShieldMode.CELL_TOWER) {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            tvConnectedTower.text = "Cell ID #$currentTower"
            if (currentTower == config.collegeCellTowerId) {
                tvStatus.text = "Status: Inside College (Tower Matched)"
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
        } else {
            // Plan B: Timetable evaluation
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (nowTime >= config.collegeStartTime && nowTime <= config.collegeEndTime) {
                tvStatus.text = "Status: Class in Session (Phone Muted)"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_400))
                imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_400))
                progressVolume.progress = 0
                tvVolumePercentage.text = "0% (Muted)"
            } else {
                tvStatus.text = "Status: Outside Class Hours (Normal)"
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
        Toast.makeText(this, "🚀 Smart Campus Protection Shield Started", Toast.LENGTH_SHORT).show()
    }

    private fun refreshUi() {
        val hasDnd = audioController.hasDndPermission()
        btnToggleDndPermission.visibility = if (hasDnd) View.GONE else View.VISIBLE
        tvRingerMode.text = "Current Mode: ${audioController.getCurrentRingerModeName()}"
        evaluateCurrentState()
    }

    private fun updateUiState(status: String, distance: Double) {
        tvStatus.text = "Status: $status"
        tvDistance.text = "📍 Distance: ${distance.toInt()}m"
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
