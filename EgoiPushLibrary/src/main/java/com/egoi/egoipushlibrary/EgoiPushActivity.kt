package com.egoi.egoipushlibrary

import androidx.appcompat.app.AppCompatActivity

open class EgoiPushActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        EgoiPushLibrary.getInstance().rebindService()
    }

    override fun onStop() {
        EgoiPushLibrary.getInstance().unbindService()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EgoiPushLibrary.getInstance().handleLocationAccessResponse(permissions, grantResults)
    }
}