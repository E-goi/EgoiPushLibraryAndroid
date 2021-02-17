package com.egoiapp.egoipushlibraryandroid

import android.content.Intent
import android.util.Log
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        EgoiPushLibrary.getInstance(applicationContext).firebase.updateToken(token)
        Log.d(TAG, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        EgoiPushLibrary.getInstance(applicationContext).firebase.messageReceived()
    }

    override fun handleIntent(intent: Intent?) {
        if (intent != null) {
            EgoiPushLibrary.getInstance(applicationContext).firebase.processMessage(intent)
        }

        super.handleIntent(intent)
    }

    override fun handleIntentOnMainThread(intent: Intent?): Boolean {
        if (intent != null) {
            EgoiPushLibrary.getInstance(applicationContext).firebase.showDialog(intent)
        }

        return super.handleIntentOnMainThread(intent)
    }

    companion object {
        private const val TAG: String = "FirebaseService"
    }
}