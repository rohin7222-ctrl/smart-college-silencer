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
import com.campussilencer.app.data.AppSettingsRepository
import com.campussilencer.app.data.SilencerState
import com.campussilencer.app.nativebridge.NativeGeoEngine

/**
 * CampusGeofenceService (Kotlin Layer)
 * Continuous battery-optimized location monitoring service.
 * Utilizes C++ native geo-math for high performance boundary checking.
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
        Log.i(TAG, "CampusGeofenceService started.")
        audioController = AudioModeController(this)
        settingsRepository = AppSettingsRepository(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring College Location..."))

        registerLocationUpdates()
    }

    private fun registerLocationUpdates() {
        try {
            val config = settingsRepository.getConfig()
            // Initial conservative interval
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                15000L, // 15 sec
                10f,    // 10 meters
                this
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                30000L, // 30 sec fallback
                20f,
                this
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        val config = settingsRepository.getConfig()

        // Check if user manually opted out for today (e.g., Absent/Leave)
        if (config.takingLeaveToday) {
            currentState = SilencerState.LEAVE_MODE
            updateNotification("Leave Today Active: Silent mode paused.")
            broadcastState(currentState, 0.0)
            return
        }

        // 1. Calculate distance to College Campus using Native C++ engine
        val distanceToCampus = NativeGeoEngine.calculateDistance(
            location.latitude, location.longitude,
            config.collegeLatitude, config.collegeLongitude
        )
        lastDistanceToCampus = distanceToCampus

        // 2. Check if user is inside College Campus Boundary
        val isInsideCampus = NativeGeoEngine.isInsideCampus(
            location.latitude, location.longitude,
            config.collegeLatitude, config.collegeLongitude,
            config.campusRadiusMeters
        )

        // 3. Check if user reached Home
        val isInsideHome = NativeGeoEngine.isInsideCampus(
            location.latitude, location.longitude,
            config.homeLatitude, config.homeLongitude,
            config.homeRadiusMeters
        )

        Log.d(TAG, "Loc: (${location.latitude}, ${location.longitude}) -> Dist to College: ${distanceToCampus.toInt()}m, Inside: $isInsideCampus")

        if (isInsideCampus) {
            if (currentState != SilencerState.INSIDE_CAMPUS) {
                currentState = SilencerState.INSIDE_CAMPUS
                Log.i(TAG, "Entering College Campus! Activating Silent Mode...")
                audioController.muteForCampus(config.vibrateOnly)
                updateNotification("Inside College: Phone is Muted (${if (config.vibrateOnly) "Vibrate" else "Silent"})")
            }
        } else if (isInsideHome && config.autoRestoreAtHome) {
            if (currentState != SilencerState.AT_HOME) {
                currentState = SilencerState.AT_HOME
                Log.i(TAG, "Arrived Home! Restoring normal ringer...")
                audioController.restoreNormalRinger()
                updateNotification("At Home: Normal Ringer Restored")
            }
        } else {
            // User exited campus boundary and is in transit
            if (currentState == SilencerState.INSIDE_CAMPUS) {
                Log.i(TAG, "Exited College Campus! Restoring normal ringer...")
                audioController.restoreNormalRinger()
                currentState = SilencerState.IN_TRANSIT
                updateNotification("Left College: Normal Ringer Restored")
            } else if (currentState != SilencerState.AT_HOME) {
                currentState = SilencerState.IN_TRANSIT
                updateNotification("In Transit: Distance to College: ${distanceToCampus.toInt()}m")
            }
        }

        broadcastState(currentState, distanceToCampus)
    }

    private fun broadcastState(state: SilencerState, distance: Double) {
        val intent = Intent(ACTION_STATUS_UPDATED).apply {
            putExtra(EXTRA_STATUS, state.name)
            putExtra(EXTRA_DISTANCE, distance)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Campus Silencer")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Campus Silencer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors campus location to automatically mute/unmute phone"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(this)
        Log.i(TAG, "CampusGeofenceService stopped.")
    }

    // Unused location listener callbacks
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
