package com.egoi.egoipushlibrary.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

/**
 * Worker responsible for sending the interactions of the user with the notification to E-goi
 */
class RegisterEventWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val queue: RequestQueue = Volley.newRequestQueue(context)
    private val domain = "https://btg-push-wrapper.egoiapp.com"
    private val registerEventUrl = "${domain}/event"

    override fun doWork(): Result {
        val payload = JSONObject()

        payload.put("api_key", inputData.getString("apiKey"))
        payload.put("app_id", inputData.getInt("appId", 0))
        payload.put("contact", inputData.getString("contactId"))
        payload.put("os", "android")
        payload.put("message_hash", inputData.getString("messageHash"))
        payload.put("event", inputData.getString("event"))
        payload.put("device_id", inputData.getInt("deviceId", 0))

        val request = JsonObjectRequest(Request.Method.POST, registerEventUrl, payload,
            { response: JSONObject ->
                if (response["data"] == "OK") {
                    Log.i("EVENT", "SUCCESS")
                } else {
                    Log.e("EVENT", "FAILED")
                }
            },
            { error ->
                Log.e("EVENT", error.toString())
            }
        )

        request.retryPolicy = DefaultRetryPolicy(
            2000,
            1,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(request)

        return Result.success()
    }
}