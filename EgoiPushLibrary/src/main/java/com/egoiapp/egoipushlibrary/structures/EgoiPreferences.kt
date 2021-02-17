package com.egoiapp.egoipushlibrary.structures

import org.json.JSONException
import org.json.JSONObject

data class EgoiPreferences(
    var appId: String = "",
    var apiKey: String = "",
    var activityPackage: String = "",
    var activityName: String = "",
    var locationUpdates: Boolean = false
) {
    fun encode(): String? {
        return try {
            val json = JSONObject()
            json.put("app-id", appId)
            json.put("api-key", apiKey)
            json.put("activity-package", activityPackage)
            json.put("activity-name", activityName)
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
                activityPackage = json.getString("activity-package"),
                activityName = json.getString("activity-name"),
                locationUpdates = json.getBoolean("location-updates")
            )
        } catch (exception: JSONException) {
            null
        }
    }
}