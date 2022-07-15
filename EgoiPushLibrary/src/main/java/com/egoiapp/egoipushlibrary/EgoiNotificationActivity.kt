package com.egoiapp.egoipushlibrary

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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

        if (intent.action == LOCATION_NOTIFICATION_OPEN) {
            finishActivity()
        } else if (intent.action in arrayOf(NOTIFICATION_OPEN, NOTIFICATION_ACTION_VIEW)) {

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

            if (intent.action == NOTIFICATION_OPEN) {
                if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {
                    if (EgoiPushLibrary.getInstance(applicationContext).dialogCallback != null) {
                        EgoiPushLibrary.getInstance(applicationContext).dialogCallback?.let {
                            it(egoiNotification)
                        }
                        finishActivity()
                    } else {
                        fireDialog(egoiNotification)
                    }
                } else {
                    EgoiPushLibrary.getInstance(applicationContext)
                        .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                    finishActivity()
                }
            } else if (intent.action == NOTIFICATION_ACTION_VIEW) {
                EgoiPushLibrary.getInstance(applicationContext)
                    .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)

                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(applicationContext).deepLinkCallback?.let {
                        it(egoiNotification)
                    }
                    finishActivity()
                } else {
                    val uriIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(egoiNotification.actionUrl))
                    startActivity(uriIntent)
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
                } else {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(egoiNotification.actionUrl)
                        )
                    )
                }
            }

            builder.setNegativeButton(egoiNotification.actionTextCancel)
            { _, _ ->
                EgoiPushLibrary.getInstance(applicationContext)
                    .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                finishActivity()
            }
        }

        val mainHandler = Handler(applicationContext.mainLooper)

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

    companion object {
        const val NOTIFICATION_OPEN: String = "EGOI_NOTIFICATION_OPEN"
        const val NOTIFICATION_ACTION_VIEW: String = "EGOI_NOTIFICATION_ACTION_VIEW"
        const val LOCATION_NOTIFICATION_OPEN: String = "EGOI_LOCATION_NOTIFICATION_OPEN"
    }
}