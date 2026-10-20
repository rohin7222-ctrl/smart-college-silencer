package com.campussilencer.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.campussilencer.app.data.AppSettingsRepository;
import com.campussilencer.app.data.CampusConfig;
import com.campussilencer.app.service.CampusAlarmScheduler;

/**
 * BootCompletedReceiver (Java Layer)
 * Listens for system boot completion and reschedules exact alarms.
 * Does NOT start a location foreground service on Android 14+ to prevent crash.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            Log.i(TAG, "Device reboot completed. Rescheduling campus exact alarms...");

            try {
                AppSettingsRepository repository = new AppSettingsRepository(context);
                CampusConfig config = repository.getConfig();
                CampusAlarmScheduler.INSTANCE.scheduleAllAlarms(context, config);
                Log.i(TAG, "Alarms successfully rescheduled after boot.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to reschedule alarms after boot", e);
            }
        }
    }
}
