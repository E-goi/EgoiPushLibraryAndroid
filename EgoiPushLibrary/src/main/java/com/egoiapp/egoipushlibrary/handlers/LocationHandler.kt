package com.egoiapp.egoipushlibrary.handlers

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.receivers.LocationBroadcastReceiver
import com.egoiapp.egoipushlibrary.services.LocationUpdatesIntentService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Class responsible for handling operations related to the location
 */
class LocationHandler(
    private val instance: EgoiPushLibrary
) {
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(instance.context)

    private val locationRequest: LocationRequest? = LocationRequest.create()?.apply {
        interval = UPDATE_INTERVAL_IN_MILLISECONDS
        fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    init {
        if (checkPermissions() && EgoiPushLibrary.getInstance(instance.context).dataStore.getDSConfigs()?.locationUpdates == true) {
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        try {
            Log.d(TAG, "Starting location updates...")

            fusedLocationClient.requestLocationUpdates(locationRequest, getPendingIntent())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun removeLocationUpdates() {
        Log.d(TAG, "Removing location updates...")

        stopService()

        fusedLocationClient.removeLocationUpdates(getPendingIntent())

        EgoiPushLibrary.getInstance(instance.context).dataStore.setDSLocationUpdates(false)
    }

    fun startService() {
        if (checkPermissions() && EgoiPushLibrary.getInstance(instance.context).dataStore.getDSConfigs()?.locationUpdates == true) {
            instance.context.startService(
                Intent(
                    instance.context,
                    LocationUpdatesIntentService::class.java
                )
            )
        }
    }

    fun stopService() {
        if (checkPermissions() && EgoiPushLibrary.getInstance(instance.context).dataStore.getDSConfigs()?.locationUpdates == true) {
            instance.context.stopService(
                Intent(
                    instance.context,
                    LocationUpdatesIntentService::class.java
                )
            )
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(instance.context, LocationBroadcastReceiver::class.java)
        intent.action = LocationBroadcastReceiver.ACTION_PROCESS_UPDATES

        return PendingIntent.getBroadcast(
            instance.context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Request access to the location when the app is in foreground.
     */
    fun requestForegroundAccess() {
        if (!checkPermissions()) {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

                requestAccess(permissions, 1)
            } else {
                instance.dataStore.setDSLocationUpdates(status = true)
                requestLocationUpdates()
            }
        } else {
            instance.dataStore.setDSLocationUpdates(status = true)
            requestLocationUpdates()
        }
    }

    /**
     * Request access to the location when the app is in background. Only applicable for devices
     * with Android Q or higher.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundAccess() {
        if (!checkPermissions()) {
            if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                val permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

                requestAccess(permissions, 2)
            } else {
                instance.dataStore.setDSLocationUpdates(status = true)
                requestLocationUpdates()
            }
        } else {
            instance.dataStore.setDSLocationUpdates(status = true)
            requestLocationUpdates()
        }
    }

    /**
     * Validate if the user granted access to the location
     * @param permissions The permissions requested.
     * @param grantResults The response of the user.
     * @return TRUE if permission granted, FALSE if not.
     */
    fun handleAccessResponse(
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            return false
        }

        if (
            permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
            permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            instance.dataStore.setDSLocationUpdates(status = true)

            return true
        }

        return false
    }

    /**
     * Request access to a target permission.
     * @param permissions The target permission.
     * @param requestCode The code identify the request.
     */
    private fun requestAccess(permissions: Array<out String>, requestCode: Int) {
        if (hasPermission(permissions[0]))
            return

        ActivityCompat.requestPermissions(
            instance.activityContext as Activity,
            permissions,
            requestCode
        )
    }

    /**
     * Check if the user granted access to the location
     * @return True or false
     */
    fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            return false
        }

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            instance.context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    /**
     * Check if the user granted access to a specific permission
     * @param permission The permission to be checked
     */
    private fun hasPermission(permission: String): Boolean {
        if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            instance.context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG: String = "LocationHandler"
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }
}