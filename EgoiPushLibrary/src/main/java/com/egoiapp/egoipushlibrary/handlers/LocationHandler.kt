package com.egoiapp.egoipushlibrary.handlers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.egoiapp.egoipushlibrary.EgoiPushLibrary

class LocationHandler(
    private val instance: EgoiPushLibrary
) {
    fun checkLocationPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        if (ContextCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                instance.context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return true
    }

    fun requestLocationAccess() {
        val permissions: MutableList<String> = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(
            instance.activityContext as Activity,
            permissions.toTypedArray(),
            REQUEST_CODE
        )
    }

    fun handleLocationAccessResponse(requestCode: Int, grantResults: IntArray): Boolean {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    return false
                }

                return true
            }
        }

        return false
    }

    companion object {
        const val REQUEST_CODE = 654
    }
}