package com.egoiapp.egoipushlibrary.handlers

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.receivers.GeofenceBroadcastReceiver
import com.egoiapp.egoipushlibrary.structures.EGoiMessage
import com.egoiapp.egoipushlibrary.structures.EGoiMessageData
import com.egoiapp.egoipushlibrary.structures.EGoiMessageDataAction
import com.egoiapp.egoipushlibrary.structures.EGoiMessageDataGeo
import com.egoiapp.egoipushlibrary.structures.EGoiMessageNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.egoiapp.egoipushlibrary.workers.FireNotificationWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Class responsible for every operation related to geofencing
 */
class GeofenceHandler(
    private val instance: EgoiPushLibrary
) {
    private val pendingNotifications: HashMap<String, EGoiMessage> = HashMap()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(instance.context, GeofenceBroadcastReceiver::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                instance.context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                instance.context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    fun addTestGeofence() {
        val geofenceList: MutableList<Geofence> = mutableListOf()
        val geofencingClient =
            LocationServices.getGeofencingClient(instance.context)

        // Remove active test geofence
        geofencingClient.removeGeofences(listOf("TEST")).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence removed!")

                // Create test geofence
                geofenceList.add(
                    Geofence.Builder()
                        .setRequestId("TEST")
                        .setCircularRegion(41.178880, -8.682427, 100F)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setLoiteringDelay(TimeUnit.SECONDS.toMillis(10).toInt())
                        .setExpirationDuration(TimeUnit.MINUTES.toMillis(10))
                        .build()
                )

                // Create geofence request
                val geofenceRequest: GeofencingRequest = GeofencingRequest.Builder().apply {
                    addGeofences(geofenceList)
                }.build()

                // Check if user granted necessary permission
                if (ActivityCompat.checkSelfPermission(
                        instance.context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Add geofence
                    geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
                        addOnSuccessListener {
                            Log.d(TAG, "Geofence created!")
                        }
                        addOnFailureListener {
                            Log.d(TAG, "Failed to create geofence!")
                            Log.e(TAG, it.message.toString())
                        }
                    }
                }
            }
            addOnFailureListener {
                Log.d(TAG, "Failed to remove geofence!")
                Log.e(TAG, it.message.toString())
            }
        }
    }

    /**
     * Create a geofence that triggers a notification
     * @param message The data of teh notification to be displayed
     */
    fun addGeofence(message: EGoiMessage) {
        if (ActivityCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val geofencingClient =
                LocationServices.getGeofencingClient(instance.context)

            val geofence = Geofence.Builder()
                .setRequestId(message.data.messageHash)
                .setCircularRegion(
                    message.data.geo.latitude,
                    message.data.geo.longitude,
                    message.data.geo.radius
                )
                .setExpirationDuration(message.data.geo.duration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    pendingNotifications[message.data.messageHash] = message
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
        val geofencingClient =
            LocationServices.getGeofencingClient(instance.context)

        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()
        val message: EGoiMessage?

        if (id == "TEST") {
            message = EGoiMessage(
                notification = EGoiMessageNotification(
                    title = "Geofence triggered!",
                    body = "Geofence $id was triggered.",
                    image = "https://media.licdn.com/dms/image/D4D0BAQG6xPl2tobmnQ/company-logo_200_200/0/1666870374567?e=2147483647&v=beta&t=TmR6lpk4262l4uEhh7uymckCcSsjF2sTZ5nB6ZRmlgs"
                ),
                data = EGoiMessageData(
                    os = "android",
                    messageHash = id,
                    mailingId = 0,
                    listId = 0,
                    contactId = "",
                    accountId = 0,
                    applicationId = "egoipushlibrary",
                    messageId = 0,
                    geo = EGoiMessageDataGeo(
                        periodStart = "9:00",
                        periodEnd = "18:00"
                    ),
                    actions = EGoiMessageDataAction(
                        type = "http",
                        text = "View",
                        url = "https://www.e-goi.com",
                        textCancel = "Close",
                    )
                )
            )
        } else {
            message = pendingNotifications[id]
        }

        if (message != null) {
            // region Validate if the current hours are inside the defined period
            if (message.data.geo.periodStart != null && message.data.geo.periodEnd != null) {
                val periodStart = message.data.geo.periodStart!!.split(":")
                val periodEnd = message.data.geo.periodEnd!!.split(":")

                val periodStartDateTime = Calendar.getInstance()
                periodStartDateTime
                    .set(Calendar.HOUR_OF_DAY, periodStart[0].toInt())
                periodStartDateTime
                    .set(Calendar.MINUTE, periodStart[1].toInt())

                val periodEndDateTime = Calendar.getInstance()
                periodEndDateTime
                    .set(Calendar.HOUR_OF_DAY, periodEnd[0].toInt())
                periodEndDateTime
                    .set(Calendar.MINUTE, periodEnd[1].toInt())

                val currentDateTime = Calendar.getInstance()

                if (currentDateTime.before(periodStartDateTime) || currentDateTime.after(
                        periodEndDateTime
                    )
                ) {
                    return
                }
            }
            // endregion

            runBlocking {
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
                                "mailingId" to message.data.mailingId,
                                "messageId" to message.data.messageId
                            )
                        )
                        .build()
                )

                if (message.data.messageHash != "TEST") {
                    val list: List<String> = mutableListOf(id)

                    geofencingClient.removeGeofences(list)
                    pendingNotifications.remove(id)
                }
            }
        }
    }

    companion object {
        const val TAG = "GEOFENCE"
    }
}