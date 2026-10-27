package com.zemanim.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.time.ZoneId

/**
 * GPS via android.location.LocationManager only — no Play Services, no
 * network. Returns the best last-known lat/lng, or null when unavailable
 * or permission is denied.
 */
object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLastKnownLatLng(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return Pair(loc.latitude, loc.longitude)
            } catch (e: SecurityException) {
                return null
            } catch (e: IllegalArgumentException) {
                // provider not available on this device; try the next one
            }
        }
        return null
    }

    /** Device timezone id (used when the user picks GPS location). */
    fun deviceTimeZoneId(): String = ZoneId.systemDefault().id
}
