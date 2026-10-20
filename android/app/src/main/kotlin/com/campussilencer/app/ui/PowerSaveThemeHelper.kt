package com.campussilencer.app.ui

import android.content.Context
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import com.campussilencer.app.data.ThemeModePreference

object PowerSaveThemeHelper {

    /**
     * Checks if Battery Saver / Power Saving Mode is actively ON in Android system
     */
    fun isPowerSaveModeActive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }

    /**
     * Resolves the target night mode based on preference and live battery saver state:
     * - AUTO_BATTERY_SAVER: Power save ON -> MODE_NIGHT_YES (Dark), OFF -> MODE_NIGHT_NO (Light)
     * - FORCE_DARK: MODE_NIGHT_YES
     * - FORCE_LIGHT: MODE_NIGHT_NO
     */
    fun resolveNightMode(context: Context, preference: ThemeModePreference): Int {
        return when (preference) {
            ThemeModePreference.FORCE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeModePreference.FORCE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeModePreference.AUTO_BATTERY_SAVER -> {
                if (isPowerSaveModeActive(context)) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            }
        }
    }

    /**
     * Applies the theme if changed, returns true if night mode changed
     */
    fun applyTheme(context: Context, preference: ThemeModePreference): Boolean {
        val targetMode = resolveNightMode(context, preference)
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
            return true
        }
        return false
    }
}
