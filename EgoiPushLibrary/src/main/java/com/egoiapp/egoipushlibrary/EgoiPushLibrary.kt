package com.egoiapp.egoipushlibrary

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.handlers.DataStoreHandler
import com.egoiapp.egoipushlibrary.handlers.FirebaseHandler
import com.egoiapp.egoipushlibrary.handlers.GeofenceHandler
import com.egoiapp.egoipushlibrary.handlers.LocationHandler
import com.egoiapp.egoipushlibrary.structures.EGoiMessage
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.egoiapp.egoipushlibrary.workers.RegisterEventWorker
import kotlinx.coroutines.runBlocking


/**
 * This is the main class of the library. Every method that can be used, is called through here.
 */
class EgoiPushLibrary {
    lateinit var context: Context
    lateinit var activityContext: Context

    // [handlers]
    lateinit var dataStore: DataStoreHandler
    lateinit var location: LocationHandler
    private lateinit var geofence: GeofenceHandler
    lateinit var firebase: FirebaseHandler
    // [end_handlers]

    private var libraryInitialized: Boolean = false

    // [user_callbacks]
    var dialogCallback: ((EgoiNotification) -> Unit)? = null
    var deepLinkCallback: ((EgoiNotification) -> Unit)? = null
    // [end_user_callbacks]

    // [resources]
    var notificationIcon: Int = 0
    // [end_resources]

    // [strings]
    var locationUpdatedLabel: Int = 0
    private var launchActivityLabel: Int = 0
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
     * @param launchAppAction The action to listen for when the user clicks the notification
     * @param geoEnabled Flag that enables or disabled location related functionalities
     * @param dialogCallback Callback to be invoked in the place of the pop-up
     * @param deepLinkCallback Callback to be invoked when the action type of the notification is a
     * deeplink
     */
    fun config(
        activityContext: Context,
        appId: String,
        apiKey: String,
        launchAppAction: String,
        geoEnabled: Boolean = true,
        dialogCallback: ((EgoiNotification) -> Unit)? = null,
        deepLinkCallback: ((EgoiNotification) -> Unit)? = null
    ) {
        this.activityContext = activityContext

        this.dialogCallback = dialogCallback
        this.deepLinkCallback = deepLinkCallback

        setDSData(appId, apiKey, launchAppAction, geoEnabled)

        IS_INITIALIZED = true
    }

    private fun setDSData(appId: String, apiKey: String, openAppAction: String, geoEnabled: Boolean) = runBlocking {
        val egoiPreferences = EgoiPreferences(
            appId = appId,
            apiKey = apiKey,
            openAppAction = openAppAction,
            geoEnabled = geoEnabled
        )

        dataStore.setDSData(DataStoreHandler.PREFERENCES, egoiPreferences.encode())
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
     * Send event to E-goi
     * @param event The event to send to E-goi
     * @param notification The notification associated to the event
     */
    fun registerEvent(event: String, notification: EgoiNotification): String? {
        val allowedEvents: Array<String> = arrayOf(OPEN_EVENT, CANCEL_EVENT, RECEIVED_EVENT)

        if (event !in allowedEvents) {
            return "Invalid event"
        }

        requestWork(
            workRequest = OneTimeWorkRequestBuilder<RegisterEventWorker>()
                .setInputData(
                    workDataOf(
                        "apiKey" to notification.apiKey,
                        "appId" to notification.appId,
                        "contactId" to notification.contactId,
                        "messageHash" to notification.messageHash,
                        "event" to event,
                        "deviceId" to notification.deviceId
                    )
                )
                .build()
        )

        return null
    }

    /**
     * Read the properties of the metadata
     */
    private fun readMetadata() {
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
        var IS_INITIALIZED: Boolean = false
        const val OPEN_EVENT = "open"
        const val CANCEL_EVENT = "canceled"
        const val RECEIVED_EVENT = "received"

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