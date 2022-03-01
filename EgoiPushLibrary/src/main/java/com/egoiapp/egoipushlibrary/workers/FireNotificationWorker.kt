package com.egoiapp.egoipushlibrary.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.receivers.NotificationEventReceiver
import kotlinx.coroutines.runBlocking
import java.net.URL

class FireNotificationWorker(
    val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    private lateinit var title: String
    private lateinit var text: String
    private lateinit var image: String
    private lateinit var actionType: String
    private lateinit var actionText: String
    private lateinit var actionUrl: String
    private lateinit var actionTextCancel: String
    private lateinit var apiKey: String
    private lateinit var appId: String
    private lateinit var contactId: String
    private lateinit var messageHash: String
    private lateinit var deviceId: Number
    private lateinit var messageId: Number

    override fun doWork(): Result {
        configNotificationManager()

        title = inputData.getString("title") ?: ""
        text = inputData.getString("text") ?: ""
        image = inputData.getString("image") ?: ""
        actionType = inputData.getString("actionType") ?: ""
        actionText = inputData.getString("actionText") ?: ""
        actionUrl = inputData.getString("actionUrl") ?: ""
        actionTextCancel = inputData.getString("actionTextCancel") ?: ""
        apiKey = inputData.getString("apiKey") ?: ""
        appId = inputData.getString("appId") ?: ""
        contactId = inputData.getString("contactId") ?: ""
        messageHash = inputData.getString("messageHash") ?: ""
        deviceId = inputData.getInt("deviceId", 0)
        messageId = inputData.getInt("messageId", 0)

        if (title != "" && text != "") {
            notification = buildNotification()
        } else {
            return Result.failure()
        }

        sendNotification()

        return Result.success()
    }

    private fun configNotificationManager() {
        notificationManager = startNotificationManager()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = startNotificationChannel()
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    /**
     * Creates a custom notification channel. Only applicable if device's Android version is equal
     * or higher to Android O
     * @return The custom notification channel
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startNotificationChannel(): NotificationChannel {
        val name = "egoi_channel_push"
        val descriptionText = "Channel from E-goi push notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH

        return NotificationChannel("egoi_channel", name, importance).apply {
            description = descriptionText
        }
    }

    /**
     * Retrieves an instance of a notification manager
     */
    private fun startNotificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Builds a local notification
     * @return Local notification
     */
    private fun buildNotification(): Notification {
        val intent = Intent(context, NotificationEventReceiver::class.java)
        intent.action = NotificationEventReceiver.NOTIFICATION_OPEN
        // Dialog Data
        intent.putExtra("title", title)
        intent.putExtra("body", text)
        intent.putExtra("actionType", actionType)
        intent.putExtra("actionText", actionText)
        intent.putExtra("actionUrl", actionUrl)
        intent.putExtra("actionTextCancel", actionTextCancel)
        // Event Data
        intent.putExtra("apiKey", apiKey)
        intent.putExtra("appId", appId)
        intent.putExtra("contactId", contactId)
        intent.putExtra("messageHash", messageHash)
        intent.putExtra("deviceId", deviceId)

        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val viewIntent = Intent(context, NotificationEventReceiver::class.java)
        viewIntent.action = NotificationEventReceiver.NOTIFICATION_EVENT_VIEW
        // Dialog Data
        viewIntent.putExtra("title", title)
        viewIntent.putExtra("body", text)
        viewIntent.putExtra("actionType", actionType)
        viewIntent.putExtra("actionText", actionText)
        viewIntent.putExtra("actionUrl", actionUrl)
        viewIntent.putExtra("actionTextCancel", actionTextCancel)
        // Event Data
        viewIntent.putExtra("apiKey", apiKey)
        viewIntent.putExtra("appId", appId)
        viewIntent.putExtra("contactId", contactId)
        viewIntent.putExtra("messageHash", messageHash)
        viewIntent.putExtra("deviceId", deviceId)
        viewIntent.putExtra("messageId", messageId)

        val viewPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = Intent(context, NotificationEventReceiver::class.java)
        closeIntent.action = NotificationEventReceiver.NOTIFICATION_EVENT_CLOSE
        // Dialog Data
        closeIntent.putExtra("title", title)
        closeIntent.putExtra("body", text)
        closeIntent.putExtra("actionType", actionType)
        closeIntent.putExtra("actionText", actionText)
        closeIntent.putExtra("actionUrl", actionUrl)
        closeIntent.putExtra("actionTextCancel", actionTextCancel)
        // Event Data
        closeIntent.putExtra("apiKey", apiKey)
        closeIntent.putExtra("appId", appId)
        closeIntent.putExtra("contactId", contactId)
        closeIntent.putExtra("messageHash", messageHash)
        closeIntent.putExtra("deviceId", deviceId)
        closeIntent.putExtra("messageId", messageId)

        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            closeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder =
            NotificationCompat.Builder(context, "egoi_channel")
                .setSmallIcon(EgoiPushLibrary.getInstance(context).notificationIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(closePendingIntent)
                .setAutoCancel(true)

        if (actionType !== "" && actionText !== "" && actionUrl !== "" && actionTextCancel !== "") {
            builder
                .addAction(
                    0,
                    actionText,
                    viewPendingIntent
                )
                .addAction(
                    0,
                    actionTextCancel,
                    closePendingIntent
                )
        }

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

    /**
     * Converts an url image to a bitmap
     * @return The image in bitmap format
     */
    private fun decodeImage(): Bitmap? = runBlocking {
        val url = URL(image)

        return@runBlocking BitmapFactory.decodeStream(url.openStream())
    }

    /**
     * Sends a local notification to the user's device
     */
    private fun sendNotification() {
        notificationManager.notify(messageId.toInt(), notification)
        registerEvent()
    }

    private fun registerEvent() {
        EgoiPushLibrary.getInstance(context).requestWork(
            workRequest = OneTimeWorkRequestBuilder<RegisterEventWorker>()
                .setInputData(
                    workDataOf(
                        "apiKey" to apiKey,
                        "appId" to appId,
                        "contactId" to contactId,
                        "messageHash" to messageHash,
                        "event" to "received",
                        "deviceId" to deviceId
                    )
                )
                .build()
        )
    }
}