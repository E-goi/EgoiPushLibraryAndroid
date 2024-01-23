package com.egoiapp.egoipushlibrary

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.egoiapp.egoipushlibrary.handlers.DataStoreHandler
import com.egoiapp.egoipushlibrary.receivers.NotificationEventReceiver
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import kotlinx.coroutines.runBlocking

class EgoiNotificationActivity : AppCompatActivity() {
    private var preferences: EgoiPreferences = EgoiPreferences()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egoi_notification)
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()

        preferences = EgoiPushLibrary.getInstance(applicationContext)
            .dataStore.getDSPreferences()

        val messageId: Int = intent.extras?.getInt("messageId", 0) ?: 0
        val index: Int? = getProcessedNotificationIndex(messageId)

        if (index != null) {
            removeProcessedNotification(index)
            finish()
            return
        }

        processIntent()
    }

    private fun processIntent() {
        if (intent.action == LOCATION_NOTIFICATION_OPEN) {
            finishActivity()
        } else if (intent.action in listOf(NOTIFICATION_OPEN, NOTIFICATION_ACTION_VIEW)) {
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
                mailingId = intent.extras?.getInt("mailingId") ?: 0,
                deviceId = intent.extras?.getInt("deviceId", 0) ?: 0,
                messageId = intent.extras?.getInt("messageId", 0) ?: 0
            )

            if (!EgoiPushLibrary.IS_INITIALIZED) {
                val intentBroadcast =
                    Intent(applicationContext, NotificationEventReceiver::class.java)
                intentBroadcast.action = when (intent.action) {
                    NOTIFICATION_OPEN -> applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_OPEN
                    NOTIFICATION_ACTION_VIEW -> applicationContext.packageName + NotificationEventReceiver.NOTIFICATION_ACTION_VIEW
                    else -> null
                }
                intent.extras?.let { intentBroadcast.putExtras(it) }
                sendBroadcast(intentBroadcast)
                finish()
            } else {
                preferences.processedNotifications.put(egoiNotification.messageId)

                runBlocking {
                    EgoiPushLibrary.getInstance(applicationContext).dataStore
                        .setDSData(DataStoreHandler.PREFERENCES, preferences.encode())
                }

                if (intent.action == NOTIFICATION_OPEN) {
                    if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {
                        if (EgoiPushLibrary.getInstance(applicationContext).dialogCallback != null) {
                            EgoiPushLibrary.getInstance(applicationContext).dialogCallback?.let {
                                it(egoiNotification)
                            }
                            finish()
                        } else {
                            fireDialog(egoiNotification)
                        }
                    } else {
                        if (egoiNotification.messageHash != "TEST") {
                            EgoiPushLibrary.getInstance(applicationContext)
                                .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                        }

                        finish()
                    }
                }

                if (intent.action == NOTIFICATION_ACTION_VIEW) {
                    if (egoiNotification.messageHash != "TEST") {
                        EgoiPushLibrary.getInstance(applicationContext)
                            .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                    }

                    if (egoiNotification.actionType == "deeplink") {
                        EgoiPushLibrary.getInstance(applicationContext).deepLinkCallback?.let {
                            it(egoiNotification)
                        }

                        val index: Int? = getProcessedNotificationIndex(egoiNotification.messageId)

                        if (index != null) {
                            removeProcessedNotification(index)
                        }

                        finish()
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
                if (egoiNotification.messageHash != "TEST") {
                    EgoiPushLibrary.getInstance(applicationContext)
                        .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                }

                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(applicationContext).deepLinkCallback?.let {
                        it(egoiNotification)
                    }

                    val index: Int? = getProcessedNotificationIndex(egoiNotification.messageId)

                    if (index != null) {
                        removeProcessedNotification(index)
                    }

                    finish()
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
                if (egoiNotification.messageHash != "TEST") {
                    EgoiPushLibrary.getInstance(applicationContext)
                        .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                }

                finish()
            }

            builder.setOnCancelListener {
                finish()
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

    private fun getProcessedNotificationIndex(messageId: Int): Int? {
        if (messageId > 0) {
            for (i in 0 until preferences.processedNotifications.length()) {
                if (messageId == preferences.processedNotifications.getInt(i)) {
                    return i
                }
            }
        }

        return null
    }

    private fun removeProcessedNotification(index: Int) {
        preferences.processedNotifications.remove(index)

        runBlocking {
            EgoiPushLibrary.getInstance(applicationContext).dataStore
                .setDSData(DataStoreHandler.PREFERENCES, preferences.encode())
        }
    }

    companion object {
        const val NOTIFICATION_OPEN: String = "EGOI_NOTIFICATION_OPEN"
        const val NOTIFICATION_ACTION_VIEW: String = "EGOI_NOTIFICATION_ACTION_VIEW"
        const val LOCATION_NOTIFICATION_OPEN: String = "EGOI_LOCATION_NOTIFICATION_OPEN"
    }
}