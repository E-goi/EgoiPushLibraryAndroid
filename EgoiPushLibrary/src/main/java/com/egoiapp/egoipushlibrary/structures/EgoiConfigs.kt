package com.egoiapp.egoipushlibrary.structures

import org.json.JSONException
import org.json.JSONObject

data class EgoiConfigs(
    var serviceBound: Boolean = false,
    var locationUpdates: Boolean = false
) {
    /**
     * @throws JSONException
     */
    fun encode(): String {
        val json = JSONObject()
        json.put("location-updates", locationUpdates)

        return json.toString()
    }

    /**
     * @throws JSONException
     */
    fun decode(data: String): EgoiConfigs {
        val json = JSONObject(data)

        return EgoiConfigs(
            locationUpdates = json.getBoolean("location-updates")
        )
    }
}