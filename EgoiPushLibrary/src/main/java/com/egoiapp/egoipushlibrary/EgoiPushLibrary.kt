package com.egoiapp.egoipushlibrary

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.egoiapp.egoipushlibrary.handlers.DataStoreHandler
import com.egoiapp.egoipushlibrary.handlers.FirebaseHandler
import com.egoiapp.egoipushlibrary.handlers.GeofenceHandler
import com.egoiapp.egoipushlibrary.handlers.LocationHandler
import com.egoiapp.egoipushlibrary.structures.EGoiMessage
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences


/**
 * This is the main class of the library. Every method that can be used, is called through here.
 */
class EgoiPushLibrary {
    lateinit var context: Context
    lateinit var activityContext: Context

    // [handlers]
    lateinit var dataStore: DataStoreHandler
    lateinit var location: LocationHandler
    lateinit var geofence: GeofenceHandler
    lateinit var firebase: FirebaseHandler
    // [end_handlers]

    private var libraryInitialized: Boolean = false

    // [user_callbacks]
    var deepLinkCallback: ((EgoiNotification) -> Unit)? = null
    // [end_user_callbacks]

    // [resources]
    var notificationIcon: Int = 0
    // [end_resources]

    // [strings]
    var locationUpdatedLabel: Int = 0
    var closeLabel: Int = 0
    var launchActivityLabel: Int = 0
    var stopLocationUpdatesLabel: Int = 0
    var applicationUsingLocationLabel: Int = 0
    // [end_strings]

    fun initLibrary(context: Context) {
        this.context = context

        if (!libraryInitialized) {
            libraryInitialized = true

            dataStore = DataStoreHandler(this)
            location = LocationHandler(this)
            geofence = GeofenceHandler(this)
            firebase = FirebaseHandler(this)

            readMetadata()
        }
    }

    /**
     * Library initializer
     * @param appId The ID of the E-goi's push app
     * @param apiKey The API key of the E-goi's account
     * @param deepLinkCallback Callback to be invoked when the action type of the notification is a
     * deeplink
     */
    fun config(
        activityContext: Context,
        activityPackage: String,
        activityName: String,
        appId: String,
        apiKey: String,
        deepLinkCallback: ((EgoiNotification) -> Unit)? = null
    ) {
        this.activityContext = activityContext

        this.deepLinkCallback = deepLinkCallback

        val egoiPreferences = EgoiPreferences(
            appId = appId,
            apiKey = apiKey,
            activityPackage = activityPackage,
            activityName = "$activityPackage.$activityName"
        )

        egoiPreferences.encode()?.let {
            dataStore.setDSData(category = DataStoreHandler.PREFERENCES, data = it)
        }
    }

    fun isAppOnForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                return true
            }
        }

        return false
    }

    /**
     * Create a geofence from the data received from a remote notification
     * @param message The data of the notification received
     */
    fun addGeofence(message: EGoiMessage) {
        if (location.checkPermissions()) {
            geofence.addGeofence(message)
        }
    }

    /**
     * Send a local notification that was triggered by a geofence
     * @param id The ID of the notification to be sent
     */
    fun sendGeoNotification(id: String) {
        if (location.checkPermissions()) {
            geofence.sendGeoNotification(id)
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
     * Read the properties of the metadata
     */
    fun readMetadata() {
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).apply {
            notificationIcon = metaData.getInt(
                "com.egoiapp.egoipushlibrary.notification_icon",
                R.drawable.ic_launcher_foreground
            )

            locationUpdatedLabel = metaData.getInt(
                "com.egoiapp.egoipushlibrary.location_updated_label",
                R.string.location_updated
            )

            closeLabel = metaData.getInt(
                "com.egoiapp.egoipushlibrary.close_label",
                R.string.close
            )

            launchActivityLabel = metaData.getInt(
                "com.egoiapp.egoipushlibrary.launch_activity_label",
                R.string.launch_activity
            )

            stopLocationUpdatesLabel = metaData.getInt(
                "com.egoiapp.egoipushlibrary.stop_location_updates_label",
                R.string.stop_location_updates
            )

            applicationUsingLocationLabel = metaData.getInt(
                "com.egoiapp.egoipushlibrary.application_using_location_label",
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
        fun getInstance(context: Context): EgoiPushLibrary {
            library.initLibrary(context)

            return library
        }
    }
}