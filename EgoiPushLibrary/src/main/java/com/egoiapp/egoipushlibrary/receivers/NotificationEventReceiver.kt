package com.egoiapp.egoipushlibrary.receivers

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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

            if (!EgoiPushLibrary.IS_INITIALIZED) {
                var intentPackage = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                context.startActivity(intentPackage)
            }

            thread {
                while (!EgoiPushLibrary.IS_INITIALIZED) {
                    Thread.sleep(500)
                }

                if (intent.action == context.packageName + NOTIFICATION_OPEN) {
                    if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {

                        if (EgoiPushLibrary.getInstance(context).dialogCallback != null) {
                            EgoiPushLibrary.getInstance(context).dialogCallback?.let {
                                it(egoiNotification)
                            }
                        } else {
                            fireDialog(EgoiPushLibrary.getInstance(context).activityContext, egoiNotification)
                        }
                    } else {
                        EgoiPushLibrary.getInstance(context)
                            .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        notificationManager.cancel(egoiNotification.messageId)
                    }
                } else if (intent.action == context.packageName + NOTIFICATION_ACTION_VIEW) {
                    if (egoiNotification.actionType == "deeplink") {
                        EgoiPushLibrary.getInstance(context).deepLinkCallback?.let {
                            it(egoiNotification)
                        }
                    } else if (egoiNotification.actionType == "url") {
                        val intentUrl = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(egoiNotification.actionUrl)
                        )
                        intentUrl.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intentUrl)
                    }

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    notificationManager.cancel(egoiNotification.messageId)
                }
                else if (intent.action == context.applicationContext.packageName+NOTIFICATION_CLOSE) {
                    EgoiPushLibrary.getInstance(context.applicationContext)
                        .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    notificationManager.cancel(egoiNotification.messageId)
                }
            }
        }
    }

    private fun fireDialog(mContext: Context, egoiNotification: EgoiNotification) {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(mContext)

        with(builder) {
            setTitle(egoiNotification.title)
            setMessage(egoiNotification.body)

            if (
                egoiNotification.actionType != "" &&
                egoiNotification.actionText != "" &&
                egoiNotification.actionUrl != "" &&
                egoiNotification.actionTextCancel != ""
            ) {
                setPositiveButton(egoiNotification.actionText)
                { _, _ ->
                    EgoiPushLibrary.getInstance(context)
                        .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                    if (egoiNotification.actionType == "deeplink") {
                        EgoiPushLibrary.getInstance(context).deepLinkCallback?.let {
                            it(egoiNotification)
                        }
                        /*finishActivity()*/
                    } else if(egoiNotification.actionType == "url"){
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(egoiNotification.actionUrl)
                            )
                        )
                    }
                }

                setNegativeButton(egoiNotification.actionTextCancel)
                { _, _ ->
                    EgoiPushLibrary.getInstance(context)
                        .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                }
            }

            val mainHandler = Handler(Looper.getMainLooper())

            val runnable = Runnable {
                val dialog = create()
                dialog.show()
            }

            mainHandler.post(runnable)
        }
    }


    companion object {
        const val NOTIFICATION_CLOSE: String = ".EGOI_NOTIFICATION_CLOSE"
        const val NOTIFICATION_OPEN: String = ".EGOI_NOTIFICATION_OPEN"
        const val NOTIFICATION_ACTION_VIEW: String = ".EGOI_NOTIFICATION_ACTION_VIEW"
        const val LOCATION_NOTIFICATION_OPEN: String = "EGOI_LOCATION_NOTIFICATION_OPEN"
    }
}