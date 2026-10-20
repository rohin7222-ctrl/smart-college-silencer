package com.campussilencer.app.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import com.campussilencer.app.data.AppSettingsRepository;
import com.campussilencer.app.data.CampusConfig;
import com.campussilencer.app.data.HolidayDatabase;

import java.util.Calendar;
import java.util.Date;

/**
 * WifiStateReceiver (Java Layer)
 * Provides Wi-Fi network callback and broadcast handling with consistent preferences.
 * Respects selected college weekdays, holidays, and active leaves.
 */
public class WifiStateReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiStateReceiver";
    private static ConnectivityManager.NetworkCallback activeCallback = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        checkWifiStateAndEvaluate(context);
    }

    public static void checkWifiStateAndEvaluate(Context context) {
        AppSettingsRepository settingsRepo = new AppSettingsRepository(context);
        CampusConfig config = settingsRepo.getConfig();

        if (!config.isProtectionEnabled() || !config.getEnableWifiTrigger()) {
            return;
        }

        // Respect selected college weekdays
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (!config.getSelectedWeekdays().contains(dayOfWeek)) {
            Log.d(TAG, "Today is not a selected college day. Wi-Fi trigger ignored.");
            return;
        }

        // Respect leave today
        if (config.getTakingLeaveToday()) {
            Log.d(TAG, "Leave active today. Wi-Fi trigger ignored.");
            return;
        }

        // Respect holidays
        if (HolidayDatabase.INSTANCE.checkHoliday(new Date(), config.getSelectedWeekdays(), settingsRepo.getCustomHolidays()).getFirst()) {
            Log.d(TAG, "Holiday today. Wi-Fi trigger ignored.");
            return;
        }

        String currentSsid = getCurrentWifiSsid(context);
        Log.d(TAG, "Wi-Fi check: current SSID = " + currentSsid);
        if (currentSsid == null) return;

        AudioModeController audioController = new AudioModeController(context);

        if (currentSsid.equalsIgnoreCase(config.getCollegeWifiSsid())) {
            Log.i(TAG, "Connected to College Wi-Fi (" + currentSsid + ")! Auto-muting...");
            audioController.muteForCampus(config.getVibrateOnly());
        } else if (currentSsid.equalsIgnoreCase(config.getHomeWifiSsid())) {
            Log.i(TAG, "Connected to Home Wi-Fi (" + currentSsid + ")! Restoring normal ringer...");
            audioController.restoreNormalRinger();
        }
    }

    public static synchronized void registerDynamicNetworkCallback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activeCallback == null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

            activeCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    checkWifiStateAndEvaluate(context);
                }

                @Override
                public void onLost(Network network) {
                    checkWifiStateAndEvaluate(context);
                }
            };

            try {
                cm.registerNetworkCallback(request, activeCallback);
                Log.i(TAG, "Dynamic Wi-Fi network callback registered successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to register network callback", e);
            }
        }
    }

    public static synchronized void unregisterDynamicNetworkCallback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activeCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                try {
                    cm.unregisterNetworkCallback(activeCallback);
                } catch (Exception ignored) {}
            }
            activeCallback = null;
        }
    }

    private static String getCurrentWifiSsid(Context context) {
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
        return null;
    }
}
