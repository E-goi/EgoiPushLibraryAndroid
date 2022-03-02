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
        if (context != null && intent != null) {
            val extras = intent.extras

            if (extras != null) {
                if (!EgoiPushLibrary.getInstance(context)
                        .isAppOnForeground() && intent.action !== NOTIFICATION_EVENT_CLOSE
                ) {
                    runBlocking {
                        val preferences: EgoiPreferences =
                            EgoiPushLibrary.getInstance(context).dataStore.getDSPreferences()

                        var action = LAUNCH_APP

                        if (preferences.openAppAction != "") {
                            action = preferences.openAppAction
                        }

                        val activityIntent = Intent(action)
                        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        context.startActivity(activityIntent)
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

                if (intent.action === NOTIFICATION_EVENT_CLOSE) {
                    EgoiPushLibrary.getInstance(context.applicationContext)
                        .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                    dismissNotification(context.applicationContext)
                    return
                }

                val thread = Thread {
                    while (!EgoiPushLibrary.IS_INITIALIZED) {
                        Thread.sleep(300)
                    }

                    if (intent.action == NOTIFICATION_OPEN) {
                        if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {
                            if (EgoiPushLibrary.getInstance(context.applicationContext).dialogCallback != null) {
                                EgoiPushLibrary.getInstance(context.applicationContext).dialogCallback?.let {
                                    it(egoiNotification)
                                }
                            } else {
                                EgoiPushLibrary.getInstance(context.applicationContext).requestWork(
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
                        } else {
                            EgoiPushLibrary.getInstance(context.applicationContext)
                                .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                        }
                    }

                    if (intent.action === NOTIFICATION_EVENT_VIEW) {
                        EgoiPushLibrary.getInstance(context.applicationContext)
                            .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)

                        if (egoiNotification.actionType == "deeplink") {
                            EgoiPushLibrary.getInstance(context.applicationContext).deepLinkCallback?.let {
                                it(egoiNotification)
                            }
                        } else {
                            val uriIntent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(egoiNotification.actionUrl))
                            uriIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                            context.applicationContext.startActivity(uriIntent)
                        }

                        dismissNotification(context.applicationContext)
                    }
                }

                thread.start()
            }
        }
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
        var LAUNCH_APP: String = "com.egoiapp.action.LAUNCH_APP"
    }

}