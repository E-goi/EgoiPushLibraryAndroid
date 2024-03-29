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
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.egoiapp.egoipushlibrary.EgoiNotificationActivity
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.receivers.NotificationEventReceiver
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
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
    private lateinit var mailingId: Number
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
        mailingId = inputData.getInt("mailingId", 0)
        messageId = inputData.getInt("messageId", 0)
        deviceId = inputData.getInt("deviceId", 0)

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
        val intent = Intent(context, EgoiNotificationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.action = EgoiNotificationActivity.NOTIFICATION_OPEN

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
        intent.putExtra("mailingId", mailingId)
        intent.putExtra("deviceId", deviceId)
        intent.putExtra("messageId", messageId)

        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                context,
                ACTIVITY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                ACTIVITY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val viewIntent = Intent(context, EgoiNotificationActivity::class.java)
        viewIntent.action = EgoiNotificationActivity.NOTIFICATION_ACTION_VIEW
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
        viewIntent.putExtra("mailingId", mailingId)
        viewIntent.putExtra("deviceId", deviceId)
        viewIntent.putExtra("messageId", messageId)

        val viewPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                context,
                ACTIVITY_REQUEST_CODE,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                ACTIVITY_REQUEST_CODE,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val closeIntent = Intent(context, NotificationEventReceiver::class.java)
        closeIntent.action = context.applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_CLOSE
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
        closeIntent.putExtra("mailingId", mailingId)
        closeIntent.putExtra("deviceId", deviceId)
        closeIntent.putExtra("messageId", messageId)

        val closePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                0,
                closeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                closeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder =
            NotificationCompat.Builder(context, "egoi_channel")
                .setSmallIcon(EgoiPushLibrary.getInstance(context).notificationIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(closePendingIntent)
                .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder
                .setStyle(NotificationCompat.BigTextStyle())
        } else {
            builder
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }

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

        val egoiNotification = EgoiNotification(
            apiKey = apiKey,
            appId = appId,
            contactId = contactId,
            messageHash = messageHash,
            mailingId = mailingId.toInt(),
            deviceId = deviceId.toInt()
        )

        if (egoiNotification.messageHash != "TEST") {
            EgoiPushLibrary.getInstance(context.applicationContext)
                .registerEvent(EgoiPushLibrary.RECEIVED_EVENT, egoiNotification)
        }
    }

    companion object {
        const val ACTIVITY_REQUEST_CODE: Int = 873264872
    }
}