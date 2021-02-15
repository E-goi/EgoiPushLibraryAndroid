package com.egoi.egoipushlibraryandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.egoi.egoipushlibrary.EgoiPushActivity
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.structures.EgoiNotification
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : EgoiPushActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EgoiPushLibrary.getInstance(applicationContext).config(
            activityContext = this,
            activityPackage = "com.egoi.egoipushlibraryandroid",
            activityName = "MainActivity",
            appId = "abc",
            apiKey = "abc",
            deepLinkCallback = fun (link: EgoiNotification) {
                Log.d("DEEP_LINK", link.toString())
            }
        )
    }

    fun requestForegroundAccess(view: View) {
        EgoiPushLibrary.getInstance(applicationContext).location.requestForegroundAccess()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundAccess(view: View) {
        EgoiPushLibrary.getInstance(applicationContext).location.requestBackgroundAccess()
    }

    fun registerToken(view: View) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener OnCompleteListener@{ task ->
            if (!task.isSuccessful) {
                Log.e("ERROR", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result.toString()

            EgoiPushLibrary.getInstance(applicationContext).firebase.registerToken(
                token = token,
                field = "email",
                value = "email@email.com"
            )
        }
    }
}