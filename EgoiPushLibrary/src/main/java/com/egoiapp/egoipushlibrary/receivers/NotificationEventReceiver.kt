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
import com.egoiapp.egoipushlibrary.workers.RegisterEventWorker
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
        if (context != null && intent != null) {
            val extras = intent.extras

            if (extras != null) {
                if (!EgoiPushLibrary.getInstance(context)
                        .isAppOnForeground() && intent.action !== NOTIFICATION_EVENT_CLOSE
                ) {
                    runBlocking {
                        val preferences: EgoiPreferences? =
                            EgoiPushLibrary.getInstance(context).dataStore.getDSPreferences()

                        if (preferences != null) {
                            val activityIntent = Intent(LAUNCH_APP)
                            activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                            context.startActivity(activityIntent)
                        }
                    }
                }

                egoiNotification = EgoiNotification(
                    title = extras.getString("title") ?: "",
                    body = extras.getString("body") ?: "",
                    actionType = extras.getString("actionType") ?: "",
                    actionText = extras.getString("actionText") ?: "",
                    actionUrl = extras.getString("actionUrl") ?: "",
                    actionTextCancel = extras.getString("actionTextCancel") ?: "",
                    apiKey = extras.getString("apiKey") ?: "",
                    appId = extras.getString("appId") ?: "",
                    contactId = extras.getString("contactId") ?: "",
                    messageHash = extras.getString("messageHash") ?: "",
                    deviceId = extras.getInt("deviceId", 0),
                    messageId = extras.getInt("messageId", 0)
                )

                if (intent.action == NOTIFICATION_OPEN) {
                    if (EgoiPushLibrary.getInstance(context).dialogCallback != null) {
                        EgoiPushLibrary.getInstance(context).dialogCallback?.let {
                            it(egoiNotification)
                        }
                    } else {
                        EgoiPushLibrary.getInstance(context).requestWork(
                            workRequest = OneTimeWorkRequestBuilder<FireDialogWorker>()
                                .setInitialDelay(1, TimeUnit.SECONDS)
                                .setInputData(
                                    workDataOf(
                                        /* Dialog Data */
                                        "title" to egoiNotification.title,
                                        "body" to egoiNotification.body,
                                        "actionType" to egoiNotification.actionType,
                                        "actionText" to egoiNotification.actionText,
                                        "actionUrl" to egoiNotification.actionUrl,
                                        "actionTextCancel" to egoiNotification.actionTextCancel,
                                        /* Event Data*/
                                        "apiKey" to egoiNotification.apiKey,
                                        "appId" to egoiNotification.appId,
                                        "contactId" to egoiNotification.contactId,
                                        "messageHash" to egoiNotification.messageHash,
                                        "deviceId" to egoiNotification.deviceId,
                                        "messageId" to egoiNotification.messageId
                                    )
                                )
                                .build()
                        )
                    }
                }

                if (intent.action === NOTIFICATION_EVENT_VIEW) {
                    registerEvent(context, "open")

                    if (egoiNotification.actionType === "deeplink") {
                        EgoiPushLibrary.getInstance(context).deepLinkCallback?.let {
                            it(egoiNotification)
                        }
                    } else {
                        val uriIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(egoiNotification.actionUrl))
                        uriIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        context.startActivity(uriIntent)
                    }
                }

                if (intent.action === NOTIFICATION_EVENT_CLOSE) {
                    registerEvent(context, "canceled")
                }

                if (intent.action === NOTIFICATION_EVENT_VIEW || intent.action === NOTIFICATION_EVENT_CLOSE) {
                    dismissNotification(context)
                }
            }
        }
    }

    private fun registerEvent(context: Context, event: String) {
        EgoiPushLibrary.getInstance(context).requestWork(
            workRequest = OneTimeWorkRequestBuilder<RegisterEventWorker>()
                .setInputData(
                    workDataOf(
                        "apiKey" to egoiNotification.apiKey,
                        "appId" to egoiNotification.appId,
                        "contactId" to egoiNotification.contactId,
                        "messageHash" to egoiNotification.messageHash,
                        "event" to event,
                        "deviceId" to egoiNotification.deviceId
                    )
                )
                .build()
        )
    }

    private fun dismissNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(egoiNotification.messageId)
    }

    companion object {
        const val NOTIFICATION_OPEN: String = "com.egoiapp.action.NOTIFICATION_OPEN"
        const val NOTIFICATION_EVENT_VIEW: String = "com.egoiapp.action.NOTIFICATION_EVENT_VIEW"
        const val NOTIFICATION_EVENT_CLOSE: String = "com.egoiapp.action.NOTIFICATION_EVENT_CLOSE"
        const val LAUNCH_APP: String = "com.egoiapp.action.LAUNCH_APP"
    }

}