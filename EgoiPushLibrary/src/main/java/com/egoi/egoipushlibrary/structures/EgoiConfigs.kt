package com.egoi.egoipushlibrary.structures

import org.json.JSONException
import org.json.JSONObject

data class EgoiConfigs(
    var serviceBound: Boolean = false,
    var locationUpdates: Boolean = false
) {
    fun encode(): String? {
        return try {
            val json = JSONObject()
            json.put("location-updates", locationUpdates)

            json.toString()
        } catch (exception: JSONException) {
            null
        }
    }

    fun decode(data: String): EgoiConfigs? {
        return try {
            val json = JSONObject(data)

            EgoiConfigs(
                locationUpdates = json.getBoolean("location-updates")
            )
        } catch (exception: JSONException) {
            null
        }
    }
}