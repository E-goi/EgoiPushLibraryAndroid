package com.egoiapp.egoipushlibrary.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val egoiPushLibrary: EgoiPushLibrary = EgoiPushLibrary.getInstance(context)
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = GeofenceStatusCodes
                        .getStatusCodeString(geofencingEvent.errorCode)
                    Log.e(TAG, errorMessage)
                    return
                }

                val geofenceTransition = geofencingEvent.geofenceTransition

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                    val triggeringGeofences = geofencingEvent.triggeringGeofences

                    if (triggeringGeofences != null) {
                        for (geofence in triggeringGeofences) {
                            egoiPushLibrary.sendGeoNotification(geofence.requestId)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG: String = "RECEIVER"
    }
}