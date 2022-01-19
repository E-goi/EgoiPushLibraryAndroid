package com.egoiapp.egoipushlibrary.structures

import org.json.JSONException
import org.json.JSONObject

data class EgoiPreferences(
    var appId: String = "",
    var apiKey: String = "",
    var openAppAction: String = "",
    var geoEnabled: Boolean = true,
    var locationUpdates: Boolean = false
) {
    fun encode(): String? {
        return try {
            val json = JSONObject()
            json.put("app-id", appId)
            json.put("api-key", apiKey)
            json.put("geo-enabled", geoEnabled)
            json.put("open-app-action", openAppAction)
            json.put("location-updates", locationUpdates)

            json.toString()
        } catch (exception: JSONException) {
            null
        }
    }

    fun decode(data: String): EgoiPreferences? {
        return try {
            val json = JSONObject(data)

            EgoiPreferences(
                appId = json.getString("app-id"),
                apiKey = json.getString("api-key"),
                geoEnabled = json.getBoolean("geo-enabled"),
                openAppAction = json.getString("open-app-action"),
                locationUpdates = json.getBoolean("location-updates")
            )
        } catch (exception: JSONException) {
            null
        }
    }
}