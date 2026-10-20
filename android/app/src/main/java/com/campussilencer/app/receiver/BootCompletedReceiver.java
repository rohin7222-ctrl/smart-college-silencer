package com.campussilencer.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BootCompletedReceiver (Java Layer)
 * Listens for system boot completion and restarts the background campus
 * geofence monitoring service automatically.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            Log.i(TAG, "Device reboot completed. Starting Campus Geofence Service...");

            try {
                // Component name for Kotlin CampusGeofenceService
                Intent serviceIntent = new Intent();
                serviceIntent.setClassName(context.getPackageName(), "com.campussilencer.app.service.CampusGeofenceService");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service after boot", e);
            }
        }
    }
}
