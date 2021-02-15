package com.egoi.egoipushlibrary.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.google.android.gms.location.LocationResult

class LocationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return

        val action: String = intent.action ?: ""

        if (action === ACTION_PROCESS_UPDATES) {
            val result: LocationResult? = LocationResult.extractResult(intent)

            result ?: return
        }

        if (action === ACTION_STOP_LOCATION_UPDATES) {
            EgoiPushLibrary.getInstance(context!!).location.removeLocationUpdates()
        }
    }

    companion object {
        const val ACTION_PROCESS_UPDATES: String = "com.egoi.action.PROCESS_UPDATES"
        const val ACTION_STOP_LOCATION_UPDATES: String = "com.egoi.STOP_LOCATION_UPDATES"
    }
}