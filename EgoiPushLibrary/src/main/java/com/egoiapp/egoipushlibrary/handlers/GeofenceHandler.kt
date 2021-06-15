package com.egoiapp.egoipushlibrary.handlers

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.services.GeofenceService
import com.egoiapp.egoipushlibrary.structures.EGoiMessage
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.egoiapp.egoipushlibrary.workers.FireNotificationWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Class responsible for every operation related to geofencing
 */
class GeofenceHandler(
    private val instance: EgoiPushLibrary
) {
    private val geofencingClient =
        LocationServices.getGeofencingClient(instance.context)
    private val pendingNotifications: HashMap<String, EGoiMessage> = HashMap()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(instance.context, GeofenceService::class.java)
        intent.action = "com.egoiapp.actions.ACTION_GEOFENCE_EVENT"

        PendingIntent.getService(
            instance.context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Create a geofence that triggers a notification
     * @param message The data of teh notification to be displayed
     */
    fun addGeofence(message: EGoiMessage) {
        val geofence = Geofence.Builder()
            .setRequestId(message.data.messageHash)
            .setCircularRegion(
                message.data.geo.latitude,
                message.data.geo.longitude,
                message.data.geo.radius
            )
            .setExpirationDuration(message.data.geo.duration)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(TimeUnit.MINUTES.toMillis(5).toInt())
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    pendingNotifications[message.data.messageHash] = message
                    Log.d("GEOFENCE", "CREATED")
                }

                addOnFailureListener {
                    Log.e("GEOFENCE", it.message!!)
                }
            }
        }
    }

    /**
     * Displays a notification when a geofence is triggered
     * @param id The id of the notification that will be displayed
     */
    fun sendGeoNotification(id: String) {
        val message: EGoiMessage? = pendingNotifications[id]

        if (message != null) {
            runBlocking {
                val preferences: EgoiPreferences? =
                    instance.dataStore.getDSPreferences()

                if (preferences != null) {
                    instance.requestWork(
                        workRequest = OneTimeWorkRequestBuilder<FireNotificationWorker>()
                            .setInputData(
                                workDataOf(
                                    "title" to message.notification.title,
                                    "text" to message.notification.body,
                                    "image" to message.notification.image,
                                    "actionType" to message.data.actions.type,
                                    "actionText" to message.data.actions.text,
                                    "actionUrl" to message.data.actions.url,
                                    "actionTextCancel" to message.data.actions.textCancel,
                                    "apiKey" to preferences.apiKey,
                                    "appId" to preferences.appId,
                                    "contactId" to message.data.contactId,
                                    "messageHash" to message.data.messageHash,
                                    "deviceId" to message.data.deviceId,
                                    "messageId" to message.data.messageId
                                )
                            )
                            .build()
                    )

                    val list: List<String> = mutableListOf(id)

                    geofencingClient.removeGeofences(list)
                    pendingNotifications.remove(id)
                }
            }
        }
    }
}