package com.egoiapp.egoipushlibrary.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * This service is responsible creating geofences
 */
class GeofenceService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GEOFENCE_EVENT) {
            Log.d(TAG, "Geofence service started...")
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    Log.e("GEO_EVENT", geofencingEvent.errorCode.toString())
                    return START_NOT_STICKY
                }

                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    if (geofencingEvent.triggeringGeofences != null && geofencingEvent.triggeringGeofences!!.isNotEmpty()) {
                        EgoiPushLibrary.getInstance(applicationContext)
                            .sendGeoNotification(geofencingEvent.triggeringGeofences!![0].requestId)
                    } else {
                        Log.e("GEO_EVENT", "No Geofence Trigger Found! Abort mission!")
                        return START_NOT_STICKY
                    }
                }
            } else {
                Log.e("GEO_EVENT", "Failed to get a GeofencingEvent objet from the current intent.")
                return START_NOT_STICKY
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT: String = "com.egoiapp.actions.ACTION_GEOFENCE_EVENT"
        private const val TAG: String = "GeofenceService"
    }
}