package com.egoi.egoipushlibrary.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.workers.FireDialogWorker

/**
 * Receiver responsible for handling the clicks on notifications triggered by a geofence
 */
class NotificationEventReceiver : BroadcastReceiver() {

    /**
     * Check if the action is a notification click and displays a dialog to the user
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.egoi.action.NOTIFICATION_OPEN") {

            val extras = intent.extras

            if (extras != null) {
                val i = Intent(
                    EgoiPushLibrary.getInstance().context,
                    (EgoiPushLibrary.getInstance().context as Activity)::class.java
                )
                i.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                EgoiPushLibrary.getInstance().context.startActivity(i)

                EgoiPushLibrary.getInstance().requestWork(
                    OneTimeWorkRequestBuilder<FireDialogWorker>()
                        .setInputData(
                            workDataOf(
                                /* Dialog Data */
                                "title" to extras.getString("title"),
                                "body" to extras.getString("body"),
                                "actionType" to extras.getString("actionType"),
                                "actionText" to extras.getString("actionText"),
                                "actionUrl" to extras.getString("actionUrl"),
                                /* Event Data*/
                                "apiKey" to EgoiPushLibrary.getInstance().apiKey,
                                "appId" to EgoiPushLibrary.getInstance().appId,
                                "contactId" to extras.getString("contactId"),
                                "messageHash" to extras.getString("messageHash"),
                                "deviceId" to extras.getInt("deviceId", 0)
                            )
                        )
                        .build()
                )
            }
        }
    }
}