package com.campussilencer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.campussilencer.app.MainActivity
import com.campussilencer.app.audio.AudioModeController
import com.campussilencer.app.audio.WifiStateReceiver
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.HolidayDatabase
import com.campussilencer.app.data.ShieldMode
import com.campussilencer.app.data.SilencerState
import com.campussilencer.app.nativebridge.NativeGeoEngine
import java.util.Calendar
import java.util.Date

/**
 * CampusGeofenceService (Kotlin Layer)
 * Battery-optimized background monitoring for Option 1 (Cell Tower) and Geofencing.
 * Option 3 exact alarms run independently via CampusAlarmScheduler/CampusAlarmReceiver.
 */
class CampusGeofenceService : Service(), LocationListener {

    companion object {
        private const val TAG = "CampusGeofenceService"
        const val CHANNEL_ID = "campus_silencer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STATUS_UPDATED = "com.campussilencer.app.STATUS_UPDATED"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DISTANCE = "extra_distance"
    }

    private lateinit var audioController: AudioModeController
    private lateinit var settingsRepository: AppSettingsRepository
    private var locationManager: LocationManager? = null

    private var currentState: SilencerState = SilencerState.IN_TRANSIT
    private var lastDistanceToCampus: Double = 0.0

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CampusGeofenceService initialized.")
        audioController = AudioModeController(this)
        settingsRepository = AppSettingsRepository(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Active College Protection Shield Running"))

        val config = settingsRepository.getConfig()
        if (config.enableWifiTrigger) {
            WifiStateReceiver.registerDynamicNetworkCallback(this)
        }

        // Only register continuous GPS location updates if Option 1 or GPS fallback is needed
        if (config.selectedMode == ShieldMode.CELL_TOWER || config.muteOnlyAtCollege) {
            registerLocationUpdates()
        }
    }

    private fun registerLocationUpdates() {
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                20000L, // 20 sec
                15f,    // 15 meters
                this
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                45000L, // 45 sec fallback
                25f,
                this
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for updates", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        val config = settingsRepository.getConfig()

        if (!config.isProtectionEnabled) {
            currentState = SilencerState.PROTECTION_PAUSED
            updateNotification("Protection Paused (Off)")
            broadcastState(currentState, 0.0)
            return
        }

        if (config.takingLeaveToday) {
            currentState = SilencerState.LEAVE_MODE
            updateNotification("🏖️ Leave Today Active: Silent mode paused.")
            broadcastState(currentState, 0.0)
            return
        }

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (!config.selectedWeekdays.contains(dayOfWeek)) {
            currentState = SilencerState.HOLIDAY_MODE
            updateNotification("Non-College Day: Normal Ringer Active")
            broadcastState(currentState, 0.0)
            return
        }

        val holidayCheck = HolidayDatabase.checkHoliday(
            calendar.time,
            config.selectedWeekdays,
            settingsRepository.getCustomHolidays()
        )
        if (holidayCheck.first) {
            currentState = SilencerState.HOLIDAY_MODE
            updateNotification("🌴 ${holidayCheck.second}: Ringer Active")
            broadcastState(currentState, 0.0)
            return
        }

        // Option 1: Mobile Cell Tower ID Detection (Zero GPS)
        if (config.selectedMode == ShieldMode.CELL_TOWER) {
            val currentTower = CellTowerHelper.getCurrentCellTowerId(this)
            val isAtCollegeTower = (currentTower == config.collegeCellTowerId)

            if (isAtCollegeTower) {
                if (!audioController.isCampusMuted) {
                    currentState = SilencerState.INSIDE_CAMPUS
                    Log.i(TAG, "Option 1: College Cell Tower Detected! Auto-muting...")
                    audioController.muteForCampus(config.vibrateOnly)
                    updateNotification("Inside College (Tower #$currentTower): Phone Muted")
                }
            } else {
                if (audioController.isCampusMuted) {
                    Log.i(TAG, "Option 1: Departed College Tower. Restoring ringer...")
                    audioController.restoreNormalRinger()
                    currentState = SilencerState.AT_HOME
                    updateNotification("Left College: Normal Ringer Restored")
                }
            }
            broadcastState(currentState, 0.0)
            return
        }

        // C++ Native Geo-Math Distance Calculation as fallback
        val distToCampus = NativeGeoEngine.calculateDistance(
            location.latitude, location.longitude,
            config.collegeLatitude, config.collegeLongitude
        )
        lastDistanceToCampus = distToCampus

        if (distToCampus <= config.campusRadiusMeters) {
            if (!audioController.isCampusMuted) {
                currentState = SilencerState.INSIDE_CAMPUS
                audioController.muteForCampus(config.vibrateOnly)
                updateNotification("Inside College: Phone Muted")
            }
        } else {
            if (audioController.isCampusMuted) {
                audioController.restoreNormalRinger()
                currentState = SilencerState.AT_HOME
                updateNotification("Outside College: Normal Ringer Restored")
            }
        }
        broadcastState(currentState, distToCampus)
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiStateReceiver.unregisterDynamicNetworkCallback(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
        Log.i(TAG, "CampusGeofenceService stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastState(state: SilencerState, distance: Double) {
        val intent = Intent(ACTION_STATUS_UPDATED).apply {
            putExtra(EXTRA_STATUS, state.displayName)
            putExtra(EXTRA_DISTANCE, distance)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Campus Silencer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Campus Silencer Protection Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of campus silent shield protection"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
