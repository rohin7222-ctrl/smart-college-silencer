package com.campussilencer.app.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * WifiStateReceiver (Java Layer)
 * Monitors Wi-Fi connections as a zero-battery-drain fallback.
 * When connected to College Wi-Fi -> auto-mutes.
 * When connected to Home Wi-Fi -> auto-restores ringer.
 */
public class WifiStateReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiStateReceiver";
    private static final String PREF_NAME = "campus_settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wifiSyncEnabled = prefs.getBoolean("enable_wifi_trigger", true);
        if (!wifiSyncEnabled) return;

        String collegeSsid = prefs.getString("college_wifi_ssid", "College-Student-WiFi");
        String homeSsid = prefs.getString("home_wifi_ssid", "Home-WiFi");
        boolean vibrateOnly = prefs.getBoolean("vibrate_only", true);

        String currentSsid = getCurrentWifiSsid(context);
        Log.d(TAG, "Wi-Fi Event Detected. Current SSID: " + currentSsid);

        AudioModeController audioController = new AudioModeController(context);

        if (currentSsid != null && currentSsid.equalsIgnoreCase(collegeSsid)) {
            Log.i(TAG, "Connected to College Wi-Fi! Auto-muting phone...");
            audioController.muteForCampus(vibrateOnly);
        } else if (currentSsid != null && currentSsid.equalsIgnoreCase(homeSsid)) {
            Log.i(TAG, "Connected to Home Wi-Fi! Restoring normal ringer...");
            audioController.restoreNormalRinger();
        }
    }

    private String getCurrentWifiSsid(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && activeNetwork.isConnected()) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    String ssid = info.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        return ssid.substring(1, ssid.length() - 1);
                    }
                    return ssid;
                }
            }
        }
        return null;
    }
}
