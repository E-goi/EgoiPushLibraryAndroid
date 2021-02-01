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


/**
 * This is the main class of the library. Every method that can be used, is called through here.
 */
class EgoiPushLibrary {
    private lateinit var geofenceHandler: GeofenceHandler
    private lateinit var locationHandler: LocationHandler

    val firebase = FirebaseHandler()

    // [configs]
    lateinit var context: Context
    lateinit var appId: String
    lateinit var apiKey: String

    var geoEnabled: Boolean = false
    var deepLinkCallback: ((String) -> Unit)? = null
    // [end_configs]

    // Resources
    var notificationIcon: Int = 0

    // Strings
    var locationUpdatedLabel: Int = 0
    var closeLabel: Int = 0
    var launchActivityLabel: Int = 0
    var stopLocationUpdatesLabel: Int = 0
    var applicationUsingLocationLabel: Int = 0

    var dataStore: DataStore<Preferences>? = null
    val locationUpdatesKey = booleanPreferencesKey("location_updates")

    /**
     * Library initializer
     * @param context The context to use in the library
     * @param appId The ID of the E-goi's push app
     * @param apiKey The API key of the E-goi's account
     * @param geoEnabled Flag that enables/disables location functionalities
     * @param deepLinkCallback Callback to be invoked when the action type of the notification is a
     * deeplink
     */
    fun config(
        context: Context,
        appId: String,
        apiKey: String,
        geoEnabled: Boolean = true,
        deepLinkCallback: ((String) -> Unit)? = null
    ) {
        this.context = context
        this.appId = appId
        this.apiKey = apiKey
        this.geoEnabled = geoEnabled
        this.deepLinkCallback = deepLinkCallback

        readMetadata()

        if (geoEnabled) {
            dataStore = this.context.createDataStore(
                name = "settings"
            )
            geofenceHandler = GeofenceHandler(this.context)
            locationHandler = LocationHandler(this.context)
        }
    }

    /**
     * Request the user for permission to access the location when the app is in the foreground
     */
    fun requestForegroundLocationAccess() {
        if (geoEnabled) {
            locationHandler.requestForegroundAccess()
        }
    }

    /**
     * Request the user for permission to access the location when the app is in the background/closed.
     * Only applicable for devices with Android Q or higher.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationAccess() {
        if (geoEnabled) {
            locationHandler.requestBackgroundAccess()
        }
    }

    /**
     * Handle the response of the user to the request for location access
     * @param permissions The permissions requested
     * @param grantResults The response of the user for each permission
     */
    fun handleLocationAccessResponse(
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return locationHandler.handleAccessResponse(permissions, grantResults)
    }

    /**
     * Create a geofence from the data received from a remote notification
     * @param message The data of the notification received
     */
    fun addGeofence(message: EGoiMessage) {
        if (geoEnabled) {
            geofenceHandler.addGeofence(message)
        }
    }

    /**
     * Send a local notification that was triggered by a geofence
     * @param id The ID of the notification to be sent
     */
    fun sendGeoNotification(id: String) {
        if (geoEnabled) {
            geofenceHandler.sendGeoNotification(id)
        }
    }

    /**
     * Send a work request to the work manager
     * @param workRequest The work request
     */
    fun requestWork(workRequest: WorkRequest) {
        WorkManager
            .getInstance(context)
            .enqueue(workRequest)
    }

    /**
     * Enable or disable the location updates
     */
    fun setLocationUpdates(status: Boolean) {
        if (geoEnabled) {
            locationHandler.setLocationUpdates(status)
        }
    }

    /**
     * Get the status of the location updates
     */
    fun getLocationUpdates(): Boolean {
        return locationHandler.getLocationUpdates()
    }

    /**
     * Unbind the foreground location service
     */
    fun unbindLocationService() {
        if (geoEnabled) {
            locationHandler.unbindService()
        }
    }

    /**
     * Rebind the foreground location service
     */
    fun rebindLocationService() {
        locationHandler.rebindService()
    }

    /**
     * Read the properties of the metadata
     */
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

        /**
         * Retrieve the static instance of the library
         * @return The library instance
         */
        fun getInstance(): EgoiPushLibrary {
            return library
        }
    }
}