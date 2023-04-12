package com.egoiapp.egoipushlibrary.handlers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
}