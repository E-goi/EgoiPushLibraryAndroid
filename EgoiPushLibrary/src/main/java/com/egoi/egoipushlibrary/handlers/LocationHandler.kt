package com.egoi.egoipushlibrary.handlers

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.services.LocationService
import com.egoi.egoipushlibrary.services.LocationService.LocalBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Class responsible for handling operations related to the location
 */
class LocationHandler(private val context: Context) {
    private var locationService: LocationService? = null
    private var bound: Boolean = false


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LocalBinder = service as LocalBinder
            locationService = binder.getService()
            bound = true

            if (checkPermissions() && getLocationUpdates()) {
                locationService?.requestLocationUpdates()
            } else {
                locationService?.removeLocationUpdates()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationService = null
            bound = false
        }
    }

    init {
        if (checkPermissions()) {
            context.bindService(
                Intent(EgoiPushLibrary.getInstance().context, LocationService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    /**
     * Request access to the location when the app is in foreground.
     */
    fun requestForegroundAccess() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

            requestAccess(permissions, 1)
        } else {
            setLocationUpdates(true)
            locationService?.requestLocationUpdates()
        }
    }

    /**
     * Request access to the location when the app is in background. Only applicable for devices
     * with Android Q or higher.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundAccess() {
        if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            val permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            requestAccess(permissions, 2)
        } else {
            setLocationUpdates(true)
            locationService?.requestLocationUpdates()
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
            setLocationUpdates(true)

            context.bindService(
                Intent(EgoiPushLibrary.getInstance().context, LocationService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            return true
        }

        return false
    }

    /**
     * Enable/disable the location updates
     * @param status The status to be defined
     */
    fun setLocationUpdates(status: Boolean) {
        GlobalScope.launch {
            EgoiPushLibrary.getInstance().dataStore!!.edit { settings ->
                settings[EgoiPushLibrary.getInstance().locationUpdatesKey] = status
            }
        }
    }

    /**
     * Check if the location updates are enabled
     * @return True of False
     */
    fun getLocationUpdates(): Boolean {
        val deferred = GlobalScope.async {
            EgoiPushLibrary.getInstance().dataStore!!.data.map { settings ->
                settings[EgoiPushLibrary.getInstance().locationUpdatesKey] ?: true
            }.first()
        }

        val status: Boolean

        runBlocking {
            status = deferred.await()
        }

        return status
    }

    /**
     * Unbinds the foreground location service. Must be called when the application enters in
     * foreground.
     */
    fun unbindService() {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }

    /**
     * Rebinds the foreground location service. Must be called when the application is minimized or
     * closed.
     */
    fun rebindService() {
        context.bindService(
            Intent(EgoiPushLibrary.getInstance().context, LocationService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
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
            context as Activity,
            permissions,
            requestCode
        )
    }

    /**
     * Check if the user granted access to the location
     * @return True or false
     */
    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                EgoiPushLibrary.getInstance().context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            return false
        }

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            EgoiPushLibrary.getInstance().context,
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

        return ActivityCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}