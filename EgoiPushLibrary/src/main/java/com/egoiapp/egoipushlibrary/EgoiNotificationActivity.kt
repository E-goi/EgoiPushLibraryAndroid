package com.egoiapp.egoipushlibrary

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.egoiapp.egoipushlibrary.receivers.NotificationEventReceiver
import com.egoiapp.egoipushlibrary.structures.EgoiNotification

class EgoiNotificationActivity : AppCompatActivity() {
    private var intentProcessed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egoi_notification)
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()

        EgoiPushLibrary.getInstance(this).location.stopService()

        if (intentProcessed) {
            finishActivity()
        } else {
            processIntent()
        }
    }

    private fun processIntent() {
        intentProcessed = true
        if (intent.action == NotificationEventReceiver.LOCATION_NOTIFICATION_OPEN) {
            finishActivity()
        } else if (intent.action in arrayOf(applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_OPEN, applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_ACTION_VIEW)) {

            val egoiNotification = EgoiNotification(
                title = intent.extras?.getString("title") ?: "",
                body = intent.extras?.getString("body") ?: "",
                actionType = intent.extras?.getString("actionType") ?: "",
                actionText = intent.extras?.getString("actionText") ?: "",
                actionUrl = intent.extras?.getString("actionUrl") ?: "",
                actionTextCancel = intent.extras?.getString("actionTextCancel") ?: "",
                apiKey = intent.extras?.getString("apiKey") ?: "",
                appId = intent.extras?.getString("appId") ?: "",
                contactId = intent.extras?.getString("contactId") ?: "",
                messageHash = intent.extras?.getString("messageHash") ?: "",
                deviceId = intent.extras?.getInt("deviceId", 0) ?: 0,
                messageId = intent.extras?.getInt("messageId", 0) ?: 0
            )

            if (intent.action == applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_OPEN) {
                if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {
                    fireDialog(egoiNotification)
                } else {
                    EgoiPushLibrary.getInstance(applicationContext)
                        .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                    finishActivity()
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    notificationManager.cancel(egoiNotification.messageId)
                }
            } else if (intent.action == applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_ACTION_VIEW) {
                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(applicationContext).deepLinkCallback?.let {
                        it(egoiNotification)
                    }
                } else if(egoiNotification.actionType == "url"){
                    if (EgoiPushLibrary.getInstance(applicationContext).dialogCallback != null) {
                        EgoiPushLibrary.getInstance(applicationContext).dialogCallback?.let {
                            it(egoiNotification)
                        }
                    } else {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(egoiNotification.actionUrl)
                            )
                        )
                    }
                }

                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.cancel(egoiNotification.messageId)
            }
        }
    }

    private fun fireDialog(egoiNotification: EgoiNotification) {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this)

        builder.setTitle(egoiNotification.title)
        builder.setMessage(egoiNotification.body)

        if (
            egoiNotification.actionType != "" &&
            egoiNotification.actionText != "" &&
            egoiNotification.actionUrl != "" &&
            egoiNotification.actionTextCancel != ""
        ) {
            builder.setPositiveButton(egoiNotification.actionText)
            { _, _ ->
                EgoiPushLibrary.getInstance(applicationContext)
                    .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(applicationContext).deepLinkCallback?.let {
                        it(egoiNotification)
                    }
                    finishActivity()
                } else if(egoiNotification.actionType == "url"){
                    if (EgoiPushLibrary.getInstance(applicationContext).dialogCallback != null) {
                        EgoiPushLibrary.getInstance(applicationContext).dialogCallback?.let {
                            it(egoiNotification)
                        }
                    } else {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(egoiNotification.actionUrl)
                            )
                        )
                    }
                }
            }

            builder.setNegativeButton(egoiNotification.actionTextCancel)
            { _, _ ->
                EgoiPushLibrary.getInstance(applicationContext)
                    .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                finishActivity()
            }
        }

        val mainHandler = Handler(Looper.getMainLooper())

        val runnable = Runnable {
            builder.show()
        }

        mainHandler.post(runnable)
    }

    private fun finishActivity() {
        if (isTaskRoot) {
            val intent: Intent = packageManager.getLaunchIntentForPackage(packageName)!!
            startActivity(intent)
        }

        finish()
    }
}