package com.campussilencer.app

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.service.CampusGeofenceService

class MainActivity : AppCompatActivity() {

    private lateinit var audioController: AudioModeController
    private lateinit var settingsRepo: AppSettingsRepository

    private lateinit var tvStatus: TextView
    private lateinit var tvRingerMode: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvWifiStatus: TextView
    private lateinit var imgModeIcon: android.widget.ImageView
    private lateinit var switchLeaveToday: Switch
    private lateinit var switchVibrateMode: Switch
    private lateinit var switchWifiTrigger: Switch
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
        imgModeIcon = findViewById(R.id.img_mode_icon)
        switchLeaveToday = findViewById(R.id.switch_leave_today)
        switchVibrateMode = findViewById(R.id.switch_vibrate_mode)
        switchWifiTrigger = findViewById(R.id.switch_wifi_trigger)
        btnToggleDndPermission = findViewById(R.id.btn_dnd_permission)
        btnStartService = findViewById(R.id.btn_start_service)
        btnTestMute = findViewById(R.id.btn_test_mute)
        btnTestUnmute = findViewById(R.id.btn_test_unmute)

        val config = settingsRepo.getConfig()
        switchLeaveToday.isChecked = config.takingLeaveToday
        switchVibrateMode.isChecked = config.vibrateOnly
        switchWifiTrigger.isChecked = config.enableWifiTrigger
        tvWifiStatus.text = "📶 Wi-Fi: " + config.collegeWifiSsid
    }

    private fun setupListeners() {
        switchLeaveToday.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setTakingLeaveToday(isChecked)
            Toast.makeText(this, if (isChecked) "Mute paused for today!" else "Normal campus monitoring resumed.", Toast.LENGTH_SHORT).show()
            refreshUi()
        }

        switchVibrateMode.setOnCheckedChangeListener { _, isChecked ->
            val config = settingsRepo.getConfig().copy(vibrateOnly = isChecked)
            settingsRepo.saveConfig(config)
            Toast.makeText(this, "Profile: " + (if (isChecked) "Vibrate" else "Silent"), Toast.LENGTH_SHORT).show()
        }

        switchWifiTrigger.setOnCheckedChangeListener { _, isChecked ->
            val config = settingsRepo.getConfig().copy(enableWifiTrigger = isChecked)
            settingsRepo.saveConfig(config)
        }

        btnToggleDndPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }

        btnStartService.setOnClickListener {
            startGeofenceService()
        }

        btnTestMute.setOnClickListener {
            if (audioController.muteForCampus(switchVibrateMode.isChecked)) {
                Toast.makeText(this, "Test Success: Phone Muted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }

        btnTestUnmute.setOnClickListener {
            if (audioController.restoreNormalRinger()) {
                Toast.makeText(this, "Test Success: Normal Ringer Restored!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Grant DND permission first!", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        }
    }

    private fun startGeofenceService() {
        val serviceIntent = Intent(this, CampusGeofenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Smart Campus Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun refreshUi() {
        val hasDnd = audioController.hasDndPermission()
        btnToggleDndPermission.visibility = if (hasDnd) android.view.View.GONE else android.view.View.VISIBLE

        tvRingerMode.text = "Current Mode: ${audioController.getCurrentRingerModeName()}"

        val config = settingsRepo.getConfig()
        if (config.takingLeaveToday) {
            tvStatus.text = "Status: Leave Mode (Mute Paused)"
        }
    }

    private fun updateUiState(status: String, distance: Double) {
        tvStatus.text = "Status: $status"
        tvDistance.text = "📍 Distance to College: ${distance.toInt()} meters"
        val ringerName = audioController.getCurrentRingerModeName()
        tvRingerMode.text = "Current Mode: $ringerName"

        if (status.contains("Inside", ignoreCase = true) || audioController.isCampusMuted) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_light))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.amber_light))
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            imgModeIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_green))
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
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
