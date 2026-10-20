package com.campussilencer.app.audio;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

/**
 * AudioModeController (Java Layer)
 * Handles Android system volume manipulation, Silent/Vibrate mode switching,
 * and DND (Do Not Disturb) permission checks.
 */
public class AudioModeController {

    private static final String TAG = "AudioModeController";
    private static final String PREF_NAME = "campus_audio_prefs";
    private static final String KEY_SAVED_VOLUME = "saved_ringer_volume";
    private static final String KEY_CURRENT_STATE = "is_campus_muted";

    private final Context context;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;
    private final SharedPreferences prefs;

    public AudioModeController(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks if the app has permission to toggle Do Not Disturb / Silent modes.
     * Required on Android 6.0 (API 23) and above.
     */
    public boolean hasDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    /**
     * Puts the phone in Mute / Silent / Vibrate mode.
     * Remembers the current ringtone volume so it can be restored when coming home.
     *
     * @param vibrateOnly true to switch to Vibrate mode, false for Complete Silent mode.
     * @return true if successfully muted, false if DND permission is missing.
     */
    public boolean muteForCampus(boolean vibrateOnly) {
        if (!hasDndPermission()) {
            Log.w(TAG, "Cannot mute: DND (Notification Policy) access is NOT granted!");
            return false;
        }

        // Save current volume level before muting
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        if (currentVolume > 0) {
            prefs.edit().putInt(KEY_SAVED_VOLUME, currentVolume).apply();
            Log.d(TAG, "Saved previous ringer volume: " + currentVolume);
        }

        try {
            if (vibrateOnly) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                Log.i(TAG, "Switched to VIBRATE mode successfully.");
            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Log.i(TAG, "Switched to SILENT mode successfully.");
            }
            prefs.edit().putBoolean(KEY_CURRENT_STATE, true).apply();
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while setting ringer mode", e);
            return false;
        }
    }

    /**
     * Restores normal ringer mode and loudness when user leaves college or arrives home.
     *
     * @return true if successfully restored, false otherwise.
     */
    public boolean restoreNormalRinger() {
        if (!hasDndPermission()) {
            Log.w(TAG, "Cannot restore ringer: DND access is NOT granted!");
            return false;
        }

        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            int savedVolume = prefs.getInt(KEY_SAVED_VOLUME, -1);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

            if (savedVolume > 0 && savedVolume <= maxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, savedVolume, AudioManager.FLAG_SHOW_UI);
                Log.i(TAG, "Restored saved ringer volume: " + savedVolume);
            } else {
                // Default to 70% volume if no previous volume recorded
                int defaultVol = (int) (maxVolume * 0.7);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, defaultVol, AudioManager.FLAG_SHOW_UI);
                Log.i(TAG, "Restored default ringer volume: " + defaultVol);
            }

            prefs.edit().putBoolean(KEY_CURRENT_STATE, false).apply();
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while restoring ringer mode", e);
            return false;
        }
    }

    /**
     * Checks if the app currently holds the phone in campus mute state.
     */
    public boolean isCampusMuted() {
        return prefs.getBoolean(KEY_CURRENT_STATE, false);
    }

    /**
     * Returns human-readable current phone ringer mode name.
     */
    public String getCurrentRingerModeName() {
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal (Ringing)";
            default:
                return "Unknown";
        }
    }
}
