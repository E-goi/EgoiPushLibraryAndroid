package com.egoiapp.egoipushlibrary.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.egoiapp.egoipushlibrary.workers.FireDialogWorker
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Receiver responsible for handling the clicks on notifications triggered by a geofence
 */
class NotificationEventReceiver : BroadcastReceiver() {
    private lateinit var egoiNotification: EgoiNotification

    /**
     * Check if the action is a notification click and displays a dialog to the user
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null && intent.action == NOTIFICATION_CLOSE) {
            val extras = intent.extras

            egoiNotification = EgoiNotification(
                title = extras?.getString("title") ?: "",
                body = extras?.getString("body") ?: "",
                actionType = extras?.getString("actionType") ?: "",
                actionText = extras?.getString("actionText") ?: "",
                actionUrl = extras?.getString("actionUrl") ?: "",
                actionTextCancel = extras?.getString("actionTextCancel") ?: "",
                apiKey = extras?.getString("apiKey") ?: "",
                appId = extras?.getString("appId") ?: "",
                contactId = extras?.getString("contactId") ?: "",
                messageHash = extras?.getString("messageHash") ?: "",
                deviceId = extras?.getInt("deviceId", 0) ?: 0,
                messageId = extras?.getInt("messageId", 0) ?: 0
            )

            EgoiPushLibrary.getInstance(context.applicationContext)
                .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.cancel(egoiNotification.messageId)
        }
    }

    companion object {
        const val NOTIFICATION_CLOSE: String = "EGOI_NOTIFICATION_CLOSE"
        var LAUNCH_APP: String = "com.egoiapp.action.LAUNCH_APP"
    }

}