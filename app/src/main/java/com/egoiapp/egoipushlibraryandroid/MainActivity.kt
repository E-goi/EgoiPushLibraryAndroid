package com.egoiapp.egoipushlibraryandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.location)
            .setOnClickListener {
                requestLocationAccess()
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val button: Button = findViewById(R.id.location_background)
            button.setOnClickListener {
                requestLocationAccessInBackground()
            }
            button.visibility = Button.VISIBLE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val button: Button = findViewById(R.id.notifications)
            button.setOnClickListener {
                requestNotificationsAccess()
            }
            button.visibility = Button.VISIBLE
        }

        findViewById<Button>(R.id.token)
            .setOnClickListener {
                registerToken()
            }

        EgoiPushLibrary.getInstance(applicationContext).config(
            activityContext = this,
            appId = "abc",
            apiKey = "abc",
            deepLinkCallback = fun(link: EgoiNotification) {
                Log.d("DEEP_LINK", link.toString())
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EgoiPushLibrary.getInstance(applicationContext).handleLocationAccessResponse(
            requestCode,
            grantResults
        )
    }

    private fun requestLocationAccess() {
        EgoiPushLibrary.getInstance(applicationContext).requestLocationAccess()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestLocationAccessInBackground() {
        EgoiPushLibrary.getInstance(applicationContext).requestLocationAccessInBackground()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationsAccess() {
        EgoiPushLibrary.getInstance(applicationContext).requestNotificationsAccess()
    }

    private fun registerToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener OnCompleteListener@{ task ->
            if (!task.isSuccessful) {
                Log.e("ERROR", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result.toString()

            val request = EgoiPushLibrary.getInstance(applicationContext).firebase.registerToken(
                token = token,
                field = "email",
                value = "email@email.com"
            )

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
                .observe(this) { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        // validate if state is equal to SUCCEEDED when token registered
                        if (workInfo.state == WorkInfo.State.FAILED) {
                            Log.d("TOKEN", "failed")
                        }
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            Log.d("TOKEN", "success")
                        }
                        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
                            .removeObservers(this)
                    }
                }
        }
    }
}