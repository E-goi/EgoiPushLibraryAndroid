package com.egoiapp.egoipushlibrary.services

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.R
import com.egoiapp.egoipushlibrary.handlers.LocationHandler
import com.egoiapp.egoipushlibrary.receivers.LocationBroadcastReceiver
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.text.DateFormat
import java.util.*

class LocationUpdatesIntentService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
        createNotificationManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, getNotification())

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = LocationHandler.UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = LocationHandler.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createNotificationManager() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "E-goi Push"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.setSound(null, null)

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        val title =
            getString(EgoiPushLibrary.getInstance(applicationContext).applicationUsingLocationLabel)

        val activityPendingIntent = createActivityPendingIntent()

        val stopLocationPendingIntent = createStopLocationPendingIntent()

        val priority: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            @Suppress("DEPRECATION")
            Notification.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(activityPendingIntent)
            .setContentText(getLocationTitle(this))
            .setContentTitle(title)
            .setOngoing(true)
            .setPriority(priority)
            .setSmallIcon(EgoiPushLibrary.getInstance(applicationContext).notificationIcon)
            .setTicker(getLocationTitle(this))
            .setWhen(System.currentTimeMillis())
            .setSound(Uri.EMPTY)
            .addAction(
                R.drawable.ic_cancel,
                getString(EgoiPushLibrary.getInstance(applicationContext).stopLocationUpdatesLabel),
                stopLocationPendingIntent
            )

        return builder.build()
    }

    private fun createActivityPendingIntent(): PendingIntent {
        val preferences: EgoiPreferences? =
            EgoiPushLibrary.getInstance(this).dataStore.getDSPreferences()

        val activityIntent = Intent()

        activityIntent.component = ComponentName(
            preferences!!.activityPackage,
            preferences.activityName
        )

        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        return PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            0
        )
    }

    private fun createStopLocationPendingIntent(): PendingIntent {
        val stopLocationIntent = Intent(this, LocationBroadcastReceiver::class.java)
        stopLocationIntent.action = LocationBroadcastReceiver.ACTION_STOP_LOCATION_UPDATES

        return PendingIntent.getBroadcast(
            this,
            0,
            stopLocationIntent,
            0
        )
    }

    private fun getLocationTitle(context: Context): String {
        return context.getString(
            EgoiPushLibrary.getInstance(context).locationUpdatedLabel,
            DateFormat.getDateTimeInstance().format(Date())
        )
    }

    companion object {
        private const val CHANNEL_ID: String = "egoi_channel_location"
        private const val NOTIFICATION_ID = 12345678
    }
}