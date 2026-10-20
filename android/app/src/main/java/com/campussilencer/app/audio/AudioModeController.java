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
 * remembers and restores user's exact previous ringer mode and ringer volume.
 */
public class AudioModeController {

    private static final String TAG = "AudioModeController";
    private static final String PREF_NAME = "campus_audio_prefs";
    private static final String KEY_SAVED_VOLUME = "saved_ringer_volume";
    private static final String KEY_SAVED_RINGER_MODE = "saved_ringer_mode";
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
     * Puts the phone in Mute / Vibrate mode.
     * Remembers the current ringtone mode and volume so it can be restored exactly when leaving college.
     *
     * @param vibrateOnly true to switch to Vibrate mode (incoming calls buzz in pocket), false for Complete Silent.
     * @return true if successfully muted, false if DND permission is missing.
     */
    public boolean muteForCampus(boolean vibrateOnly) {
        if (!hasDndPermission()) {
            Log.w(TAG, "Cannot mute: DND (Notification Policy) access is NOT granted!");
            return false;
        }

        // Save current ringer mode and volume before muting
        int currentRingerMode = audioManager.getRingerMode();
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

        // Only save if not already in campus muted state
        if (!isCampusMuted()) {
            prefs.edit()
                .putInt(KEY_SAVED_RINGER_MODE, currentRingerMode)
                .putInt(KEY_SAVED_VOLUME, currentVolume)
                .apply();
            Log.d(TAG, "Saved previous ringer mode: " + currentRingerMode + ", volume: " + currentVolume);
        }

        try {
            if (vibrateOnly) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                Log.i(TAG, "Switched to VIBRATE mode (calls will buzz, zero sound).");
            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Log.i(TAG, "Switched to SILENT mode.");
            }
            prefs.edit().putBoolean(KEY_CURRENT_STATE, true).apply();
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while setting ringer mode", e);
            return false;
        }
    }

    /**
     * Restores previous ringer mode and exact volume level.
     *
     * @return true if successfully restored, false otherwise.
     */
    public boolean restoreNormalRinger() {
        if (!hasDndPermission()) {
            Log.w(TAG, "Cannot restore ringer: DND access is NOT granted!");
            return false;
        }

        try {
            int savedRingerMode = prefs.getInt(KEY_SAVED_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
            int savedVolume = prefs.getInt(KEY_SAVED_VOLUME, -1);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

            audioManager.setRingerMode(savedRingerMode);

            if (savedRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                if (savedVolume >= 0 && savedVolume <= maxVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, savedVolume, 0);
                    Log.i(TAG, "Restored exact saved ringer volume: " + savedVolume);
                } else {
                    int defaultVol = (int) (maxVolume * 0.7);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, defaultVol, 0);
                    Log.i(TAG, "Restored default ringer volume: " + defaultVol);
                }
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

    public int getCurrentVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_RING);
    }

    public int getMaxVolume() {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    public int getSavedVolume() {
        return prefs.getInt(KEY_SAVED_VOLUME, -1);
    }
}
