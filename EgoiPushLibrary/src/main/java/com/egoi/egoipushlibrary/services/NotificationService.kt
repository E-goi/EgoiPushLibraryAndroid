package com.egoi.egoipushlibrary.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.receivers.NotificationEventReceiver
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.URL

class NotificationService : Service() {
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    private lateinit var title: String
    private lateinit var text: String
    private lateinit var image: String
    private lateinit var actionType: String
    private lateinit var actionText: String
    private lateinit var actionUrl: String
    private lateinit var apiKey: String
    private lateinit var appId: Number
    private lateinit var contactId: String
    private lateinit var messageHash: String
    private lateinit var deviceId: Number

    override fun onCreate() {
        super.onCreate()

        configWorker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == "com.egoi.action.SEND_NOTIFICATION") {
            val extras = intent.extras

            if (extras != null) {
                title = extras.getString("title") ?: ""
                text = extras.getString("text") ?: ""
                image = extras.getString("image") ?: ""
                actionType = extras.getString("actionType") ?: ""
                actionText = extras.getString("actionText") ?: ""
                actionUrl = extras.getString("actionUrl") ?: ""
                apiKey = extras.getString("apiKey") ?: ""
                appId = extras.getInt("appId", 0)
                contactId = extras.getString("contactId") ?: ""
                messageHash = extras.getString("messageHash") ?: ""
                deviceId = extras.getInt("deviceId", 0)

                if (title != "" && text != "") {
                    notification = buildNotification()
                } else {
                    return START_NOT_STICKY
                }

                sendNotification()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun configWorker() {
        notificationManager = startNotificationManager()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = startNotificationChannel()
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startNotificationChannel(): NotificationChannel {
        val name = "egoi_channel_push"
        val descriptionText = "Channel from E-goi push notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH

        return NotificationChannel("egoi_channel", name, importance).apply {
            description = descriptionText
        }
    }

    private fun startNotificationManager(): NotificationManager {
        return EgoiPushLibrary.getInstance().context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun buildNotification(): Notification {
        val intent =
            Intent(EgoiPushLibrary.getInstance().context, NotificationEventReceiver::class.java)
        intent.action = "com.egoi.action.NOTIFICATION_OPEN"
        // Dialog Data
        intent.putExtra("title", title)
        intent.putExtra("body", text)
        intent.putExtra("actionType", actionType)
        intent.putExtra("actionText", actionText)
        intent.putExtra("actionUrl", actionUrl)
        // Event Data
        intent.putExtra("apiKey", apiKey)
        intent.putExtra("appId", appId)
        intent.putExtra("contactId", contactId)
        intent.putExtra("messageHash", messageHash)
        intent.putExtra("deviceId", deviceId)

        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
            EgoiPushLibrary.getInstance().context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder =
            NotificationCompat.Builder(EgoiPushLibrary.getInstance().context, "egoi_channel")
                .setSmallIcon(EgoiPushLibrary.getInstance().notificationIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        if (image != "") {
            val bitmap: Bitmap? = decodeImage()

            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                )
            }
        }

        return builder.build()
    }

    private fun decodeImage(): Bitmap? {
        val url = URL(image)

        val deferred: Deferred<Bitmap?> = GlobalScope.async {
            @Suppress("BlockingMethodInNonBlockingContext")
            BitmapFactory.decodeStream(url.openStream())
        }

        var bitmap: Bitmap?

        runBlocking {
            bitmap = deferred.await()
        }

        return bitmap
    }

    private fun sendNotification() {
        notificationManager.notify(0, notification)
    }
}