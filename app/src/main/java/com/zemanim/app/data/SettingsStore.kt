package com.zemanim.app.data

import android.content.Context
import android.content.SharedPreferences
import com.zemanim.app.engine.Location

/**
 * All user settings, persisted in SharedPreferences (no DataStore, no Room —
 * keep the app light and fully offline). Mirrors the web app's settings.
 */
data class AppSettings(
    val location: Location = Location(40.7128, -74.006, "America/New_York", 10.0, "New York"),
    val candleMin: Int = 18,
    val havdalah: String = "8.5",
    val il: Boolean = false,
    val hour12: Boolean = true,
    val showMga: Boolean = true,
    val showExtraDawn: Boolean = true,
    /** "auto", "light" or "dark" */
    val theme: String = "auto"
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zemanim", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            location = Location(
                lat = prefs.getFloat("lat", 40.7128f).toDouble(),
                lng = prefs.getFloat("lng", -74.006f).toDouble(),
                tz = prefs.getString("tz", "America/New_York") ?: "America/New_York",
                elevM = prefs.getFloat("elevM", 10f).toDouble(),
                name = prefs.getString("locName", "New York") ?: "New York"
            ),
            candleMin = prefs.getInt("candleMin", 18),
            havdalah = prefs.getString("havdalah", "8.5") ?: "8.5",
            il = prefs.getBoolean("il", false),
            hour12 = prefs.getBoolean("hour12", true),
            showMga = prefs.getBoolean("showMga", true),
            showExtraDawn = prefs.getBoolean("showExtraDawn", true),
            theme = prefs.getString("theme", "auto") ?: "auto"
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putFloat("lat", s.location.lat.toFloat())
            .putFloat("lng", s.location.lng.toFloat())
            .putString("tz", s.location.tz)
            .putFloat("elevM", s.location.elevM.toFloat())
            .putString("locName", s.location.name)
            .putInt("candleMin", s.candleMin)
            .putString("havdalah", s.havdalah)
            .putBoolean("il", s.il)
            .putBoolean("hour12", s.hour12)
            .putBoolean("showMga", s.showMga)
            .putBoolean("showExtraDawn", s.showExtraDawn)
            .putString("theme", s.theme)
            .apply()
    }
}
