package com.egoi.egoipushlibrary.handlers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.services.GeofenceService
import com.egoi.egoipushlibrary.services.NotificationService
import com.egoi.egoipushlibrary.structures.EGoiMessage
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

/**
 * Class responsible for every operation related to geofencing
 */
class GeofenceHandler(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val pendingNotifications: HashMap<String, EGoiMessage> = HashMap()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceService::class.java)
        intent.action = "com.egoi.actions.ACTION_GEOFENCE_EVENT"

        PendingIntent.getService(
            context,
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
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(TimeUnit.MINUTES.toMillis(5).toInt())
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
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
            val intent = Intent(context, NotificationService::class.java)
            intent.action = "com.egoi.action.SEND_NOTIFICATION"
            // Dialog Data
            intent.putExtra("title", message.notification.title)
            intent.putExtra("text", message.notification.body)
            intent.putExtra("image", message.notification.image)
            intent.putExtra("actionType", message.data.actions.type)
            intent.putExtra("actionText", message.data.actions.text)
            intent.putExtra("actionUrl", message.data.actions.url)
            // Event Data
            intent.putExtra("apiKey", EgoiPushLibrary.getInstance().apiKey)
            intent.putExtra("appId", EgoiPushLibrary.getInstance().appId)
            intent.putExtra("contactId", message.data.contactId)
            intent.putExtra("messageHash", message.data.messageHash)
            intent.putExtra("deviceId", message.data.deviceId)

            context.startService(intent)

            val list: List<String> = mutableListOf(id)

            geofencingClient.removeGeofences(list)
            pendingNotifications.remove(id)
        }
    }
}