package com.egoi.egoipushlibrary

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.egoi.egoipushlibrary.handlers.FirebaseHandler
import com.egoi.egoipushlibrary.handlers.GeofenceHandler
import com.egoi.egoipushlibrary.handlers.LocationHandler
import com.egoi.egoipushlibrary.structures.EGoiMessage


class EgoiPushLibrary {
    lateinit var context: Context
    lateinit var appId: Number
    lateinit var apiKey: String

    private lateinit var geofenceHandler: GeofenceHandler
    private lateinit var locationHandler: LocationHandler

    val firebase = FirebaseHandler()

    // Resources
    var notificationIcon: Int = 0

    // Strings
    var locationUpdatedLabel: Int = 0
    var closeLabel: Int = 0
    var launchActivityLabel: Int = 0
    var stopLocationUpdatesLabel: Int = 0
    var applicationUsingLocationLabel: Int = 0

    var geoEnabled: Boolean = false

    var dataStore: DataStore<Preferences>? = null
    val locationUpdatesKey = booleanPreferencesKey("location_updates")

    fun config(
        context: Context,
        appId: Int,
        apiKey: String,
        geoEnabled: Boolean = true,
    ) {
        this.context = context
        this.appId = appId
        this.apiKey = apiKey
        this.geoEnabled = geoEnabled

        readMetadata()

        if (geoEnabled) {
            dataStore = this.context.createDataStore(
                name = "settings"
            )
            geofenceHandler = GeofenceHandler(this.context)
            locationHandler = LocationHandler(this.context)
        }
    }

    fun requestForegroundLocationAccess() {
        if (geoEnabled) {
            locationHandler.requestForegroundAccess()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationAccess() {
        if (geoEnabled) {
            locationHandler.requestBackgroundAccess()
        }
    }

    fun handleLocationAccessResponse(
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return locationHandler.handleAccessResponse(permissions, grantResults)
    }

    fun removeLocationUpdates() {
        if (geoEnabled) {
            locationHandler.removeLocationUpdates()
        }
    }

    fun addGeofence(message: EGoiMessage) {
        if (geoEnabled) {
            geofenceHandler.addGeofence(message)
        }
    }

    fun sendGeoNotification(id: String) {
        if (geoEnabled) {
            geofenceHandler.sendGeoNotification(id)
        }
    }

    fun requestWork(workRequest: WorkRequest) {
        WorkManager
            .getInstance(context)
            .enqueue(workRequest)
    }

    fun setLocationUpdates(status: Boolean) {
        if (geoEnabled) {
            locationHandler.setLocationUpdates(status)
        }
    }

    fun getLocationUpdates(): Boolean {
        return locationHandler.getLocationUpdates()
    }

    fun unbindService() {
        if (geoEnabled) {
            locationHandler.unbindService()
        }
    }

    fun rebindService() {
        locationHandler.rebindService()
    }

    private fun readMetadata() {
        this.context.packageManager.getApplicationInfo(
            this.context.packageName,
            PackageManager.GET_META_DATA
        ).apply {
            notificationIcon = metaData.getInt(
                "com.egoi.egoipushlibrary.notification_icon",
                R.drawable.ic_launcher_foreground
            )

            locationUpdatedLabel = metaData.getInt(
                "com.egoi.egoipushlibrary.location_updated_label",
                R.string.location_updated
            )

            closeLabel = metaData.getInt(
                "com.egoi.egoipushlibrary.close_label",
                R.string.close
            )

            launchActivityLabel = metaData.getInt(
                "com.egoi.egoipushlibrary.launch_activity_label",
                R.string.launch_activity
            )

            stopLocationUpdatesLabel = metaData.getInt(
                "com.egoi.egoipushlibrary.stop_location_updates_label",
                R.string.stop_location_updates
            )

            applicationUsingLocationLabel = metaData.getInt(
                "com.egoi.egoipushlibrary.application_using_location_label",
                R.string.application_using_location
            )
        }
    }

    companion object {
        private val library = EgoiPushLibrary()

        fun getInstance(): EgoiPushLibrary {
            return library
        }
    }
}