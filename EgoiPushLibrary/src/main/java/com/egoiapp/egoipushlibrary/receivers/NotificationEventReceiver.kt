package com.egoiapp.egoipushlibrary.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egoiapp.egoipushlibrary.EgoiNotificationActivity
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification

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
            context != null && intent != null &&
            intent.action in arrayOf(context.applicationContext.packageName + NOTIFICATION_OPEN, context.applicationContext.packageName + NOTIFICATION_ACTION_VIEW, context.applicationContext.packageName + NOTIFICATION_CLOSE)
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

            if (intent.action == context.applicationContext.packageName + NOTIFICATION_ACTION_VIEW ||
                intent.action == context.applicationContext.packageName + NOTIFICATION_OPEN) {
                if (!EgoiPushLibrary.IS_INITIALIZED) {
                    var intentActivity = context.packageManager.getLaunchIntentForPackage(context.applicationContext.packageName)!!
                    context.startActivity(intentActivity)
                }

                val intentNotification = Intent(context, EgoiNotificationActivity::class.java)
                intentNotification.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intentNotification.action = intent.action
                intent.extras?.let { intentNotification.putExtras(it) }
                context.startActivity(intentNotification);
            } else if (intent.action == context.applicationContext.packageName + NOTIFICATION_CLOSE) {
                EgoiPushLibrary.getInstance(context.applicationContext)
                    .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.cancel(egoiNotification.messageId)
            }
        }
    }

    companion object {
        const val NOTIFICATION_CLOSE: String = ".EGOI_NOTIFICATION_CLOSE"
        const val NOTIFICATION_OPEN: String = ".EGOI_NOTIFICATION_OPEN"
        const val NOTIFICATION_ACTION_VIEW: String = ".EGOI_NOTIFICATION_ACTION_VIEW"
        const val LOCATION_NOTIFICATION_OPEN: String = "EGOI_LOCATION_NOTIFICATION_OPEN"
    }
}