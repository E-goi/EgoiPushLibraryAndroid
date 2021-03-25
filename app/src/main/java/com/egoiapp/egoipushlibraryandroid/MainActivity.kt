package com.egoiapp.egoipushlibraryandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.egoiapp.egoipushlibrary.EgoiPushActivity
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : EgoiPushActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EgoiPushLibrary.getInstance(applicationContext).config(
            activityContext = this,
            activityPackage = "com.egoiapp.egoipushlibraryandroid",
            activityName = "MainActivity",
            appId = "abc",
            apiKey = "abc",
//            dialogCallback = fun (link: EgoiNotification) {
//                Log.d("DIALOG", link.toString())
//            },
            deepLinkCallback = fun (link: EgoiNotification) {
                Log.d("DEEP_LINK", link.toString())
            }
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun requestForegroundAccess(view: View) {
        EgoiPushLibrary.getInstance(applicationContext).location.requestForegroundAccess()
    }

    @Suppress("UNUSED_PARAMETER")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundAccess(view: View) {
        EgoiPushLibrary.getInstance(applicationContext).location.requestBackgroundAccess()
    }

    @Suppress("UNUSED_PARAMETER")
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