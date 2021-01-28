package com.egoi.egoipushlibraryandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.egoi.egoipushlibrary.EgoiPushActivity
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : EgoiPushActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EgoiPushLibrary.getInstance().config(
            context = this,
            appId = 123,
            apiKey = "abc"
        )
    }

    fun requestForegroundAccess(view: View) {
        EgoiPushLibrary.getInstance().requestForegroundLocationAccess()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundAccess(view: View) {
        EgoiPushLibrary.getInstance().requestBackgroundLocationAccess()
    }

    fun registerToken(view: View) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener OnCompleteListener@{ task ->
            if (!task.isSuccessful) {
                Log.e("ERROR", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result.toString()

            EgoiPushLibrary.getInstance().firebase.registerToken(
                token = token,
                field = "email",
                value = "email@email.com"
            )
        }
    }
}