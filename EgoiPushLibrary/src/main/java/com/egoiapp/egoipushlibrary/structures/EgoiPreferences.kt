package com.egoiapp.egoipushlibrary.structures

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class EgoiPreferences(
    var appId: String = "",
    var apiKey: String = "",
    var geoEnabled: Boolean = true,
    var processedNotifications: JSONArray = JSONArray()
) {
    /**
     * @throws JSONException
     */
    fun encode(): String {
        val json = JSONObject()

        json.put("app-id", appId)
        json.put("api-key", apiKey)
        json.put("geo-enabled", geoEnabled)
        json.put("processed-notifications", processedNotifications.toString())

        return json.toString()
    }

    /**
     * @throws JSONException
     */
    fun decode(data: String): EgoiPreferences {
        val json = JSONObject(data)

        return EgoiPreferences(
            appId = json.getString("app-id"),
            apiKey = json.getString("api-key"),
            geoEnabled = json.getBoolean("geo-enabled"),
            processedNotifications = JSONArray(json.getString("processed-notifications"))
        )
    }
}