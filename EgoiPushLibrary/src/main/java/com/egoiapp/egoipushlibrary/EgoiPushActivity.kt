package com.egoiapp.egoipushlibrary

import androidx.appcompat.app.AppCompatActivity

/**
 * This activity handles some operations related to the location functionalities
 */
open class EgoiPushActivity : AppCompatActivity() {

    /**
     * Rebinds the service to the session and removes the location notification
     */
    override fun onStart() {
        super.onStart()
        EgoiPushLibrary.getInstance(this).location.stopService()
    }

    /**
     * Unbinds the service from the session and displays a notification telling the user his
     * location is being accessed by the application.
     */
    override fun onStop() {
        EgoiPushLibrary.getInstance(this).location.startService()
        super.onStop()
    }

    /**
     * Receives and handle the responses of the user to permission requests. In this library,
     * only access to the location in foreground and background are requested to the user.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EgoiPushLibrary.getInstance(this).location.handleAccessResponse(permissions, grantResults)
    }
}