package com.egoi.egoipushlibrary.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.R
import com.google.android.gms.location.*
import java.text.DateFormat
import java.util.*

/**
 * Service responsible for receiving the location updates. Triggers a notification when the app is
 * minimized or closed.
 */
class LocationService : Service() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var handler: Handler
    private lateinit var location: Location

    private val binder: IBinder = LocalBinder()
    private var changingConfiguration: Boolean = false

    override fun onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                onNewLocation(result.lastLocation)
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()

        handler = Handler(handlerThread.looper)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "E-goi Push"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.setSound(null, null)

            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)

        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changingConfiguration = true
    }

    override fun onBind(intent: Intent?): IBinder {
        stopForeground(true)

        changingConfiguration = false

        return binder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        changingConfiguration = false

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!changingConfiguration && EgoiPushLibrary.getInstance().getLocationUpdates()) {
            Log.d(TAG, "Starting foreground service...")
            startForeground(NOTIFICATION_ID, getNotification())
        }

        return true
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
    }

    private fun onNewLocation(location: Location) {
        this.location = location

        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun getNotification(): Notification {
        val title = getString(EgoiPushLibrary.getInstance().applicationUsingLocationLabel)

        val intent = Intent(this, LocationService::class.java)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, (EgoiPushLibrary.getInstance().context as Activity)::class.java),
            0
        )

        val priority: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            @Suppress("DEPRECATION")
            Notification.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .addAction(
                R.drawable.ic_launch,
                getString(EgoiPushLibrary.getInstance().launchActivityLabel),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_cancel,
                getString(EgoiPushLibrary.getInstance().stopLocationUpdatesLabel),
                pendingIntent
            )
            .setContentText(getLocationTitle(this))
            .setContentTitle(title)
            .setOngoing(true)
            .setPriority(priority)
            .setSmallIcon(EgoiPushLibrary.getInstance().notificationIcon)
            .setTicker(getLocationTitle(this))
            .setWhen(System.currentTimeMillis())
            .setSound(Uri.EMPTY)

        return builder.build()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun getLastLocation() {
        try {
            fusedLocationProviderClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        location = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            EgoiPushLibrary.getInstance().setLocationUpdates(true)
            startService(Intent(applicationContext, LocationService::class.java))

            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            } catch (revokedPermission: SecurityException) {
                EgoiPushLibrary.getInstance().setLocationUpdates(false)
                Log.e(TAG, "Lost location permission. Could not remove updates. $revokedPermission")
            }
        }
    }

    fun removeLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            EgoiPushLibrary.getInstance().setLocationUpdates(false)
        } catch (revokedPermission: SecurityException) {
            EgoiPushLibrary.getInstance().setLocationUpdates(true)
            Log.e(TAG, "Lost location permission. Could not remove updates. $revokedPermission")
        }
    }

    companion object {
        private const val TAG: String = "LocationService"
        private const val CHANNEL_ID: String = "egoi_channel_location"
        private const val PACKAGE_NAME: String = "com.egoi.push.services"
        private const val EXTRA_STARTED_FROM_NOTIFICATION: String =
            "$PACKAGE_NAME.started_from_notification"
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private const val NOTIFICATION_ID = 12345678

        const val ACTION_BROADCAST: String = "$PACKAGE_NAME.broadcast"
        const val EXTRA_LOCATION: String = "$PACKAGE_NAME.location"

        private fun getLocationTitle(context: Context): String {
            return context.getString(
                EgoiPushLibrary.getInstance().locationUpdatedLabel,
                DateFormat.getDateTimeInstance().format(Date())
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService {
            return this@LocationService
        }
    }
}