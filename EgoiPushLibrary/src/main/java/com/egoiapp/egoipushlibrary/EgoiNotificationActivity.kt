package com.egoiapp.egoipushlibrary

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.egoiapp.egoipushlibrary.receivers.NotificationEventReceiver

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
            var intentReceiver = Intent(applicationContext, NotificationEventReceiver::class.java)
            intentReceiver.action = intent.action
            intent.extras?.let { intentReceiver.putExtras(it) }
            sendBroadcast(intentReceiver)

            if (!EgoiPushLibrary.IS_INITIALIZED) {
                var intentPackage = packageManager.getLaunchIntentForPackage(packageName)!!
                startActivity(intentPackage)
            }
            finish()
        }
    }

    private fun finishActivity() {
        if (isTaskRoot) {
            val intent: Intent = packageManager.getLaunchIntentForPackage(packageName)!!
            startActivity(intent)
        }
        finish()
    }
}