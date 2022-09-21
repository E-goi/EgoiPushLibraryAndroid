package com.egoiapp.egoipushlibrary.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egoiapp.egoipushlibrary.EgoiNotificationActivity
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import kotlin.concurrent.thread

/**
 * Receiver responsible for handling the clicks on notifications triggered by a geofence
 */
class NotificationEventReceiver : BroadcastReceiver() {
    private lateinit var egoiNotification: EgoiNotification

    /**
     * Check if the action is a notification click and displays a dialog to the user
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (
            context != null
            && intent != null
            && intent.action in listOf(
                context.applicationContext.packageName + NOTIFICATION_OPEN,
                context.applicationContext.packageName + NOTIFICATION_CLOSE,
                context.applicationContext.packageName + NOTIFICATION_ACTION_VIEW
            )
        ) {
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

            if (
                !EgoiPushLibrary.IS_INITIALIZED
                && intent.action != context.applicationContext.packageName + NOTIFICATION_CLOSE
            ) {
                val intentActivity =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                context.startActivity(intentActivity)
            }

            if (intent.action == context.applicationContext.packageName + NOTIFICATION_OPEN) {
                thread {
                    while (!EgoiPushLibrary.IS_INITIALIZED) {
                        Thread.sleep(500)
                    }

                    if (EgoiPushLibrary.getInstance(context.applicationContext).dialogCallback != null) {
                        EgoiPushLibrary.getInstance(context.applicationContext).dialogCallback?.let {
                            it(egoiNotification)
                        }
                    } else {
                        val intentActivity = Intent(context, EgoiNotificationActivity::class.java)
                        intentActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intentActivity.action = EgoiNotificationActivity.NOTIFICATION_OPEN
                        extras?.let { intentActivity.putExtras(it) }
                        context.startActivity(intentActivity)
                    }
                }
            }

            if (intent.action == context.applicationContext.packageName + NOTIFICATION_CLOSE) {
                EgoiPushLibrary.getInstance(context.applicationContext)
                    .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.cancel(egoiNotification.messageId)
            }

            if (intent.action == context.applicationContext.packageName + NOTIFICATION_ACTION_VIEW) {
                val intentActivity = Intent(context, EgoiNotificationActivity::class.java)
                intentActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intentActivity.action = EgoiNotificationActivity.NOTIFICATION_ACTION_VIEW
                extras?.let { intentActivity.putExtras(it) }
                context.startActivity(intentActivity)
            }
        }
    }

    companion object {
        const val NOTIFICATION_OPEN: String = ".EGOI_NOTIFICATION_OPEN"
        const val NOTIFICATION_CLOSE: String = ".EGOI_NOTIFICATION_CLOSE"
        const val NOTIFICATION_ACTION_VIEW: String = ".EGOI_NOTIFICATION_ACTION_VIEW"
    }

}