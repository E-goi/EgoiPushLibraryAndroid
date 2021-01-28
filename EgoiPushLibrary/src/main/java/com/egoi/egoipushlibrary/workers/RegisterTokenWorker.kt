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
import com.egoi.egoipushlibrary.handlers.FirebaseHandler
import org.json.JSONObject
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Worker responsible for registering/updating the Firebase token in E-goi's list
 */
class RegisterTokenWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val queue: RequestQueue = Volley.newRequestQueue(context)
    private val domain = "https://push-wrapper.egoiapp.com"
    private val registerTokenUrl = "${domain}/token"

    override fun doWork(): Result {
        val payload = JSONObject()

        payload.put("api_key", inputData.getString("apiKey"))
        payload.put("app_id", inputData.getInt("appId", 0))
        payload.put("token", inputData.getString("token"))
        payload.put("os", "android")

        val twoStepsData = inputData.getString("twoStepsData")

        if (twoStepsData != null) {
            val data = JSONObject(twoStepsData)

            if (data.getString("field") != "" &&
                data.getString("value") !== ""
            ) {
                payload.put("two_steps_data", data)
            }
        }

        val request = JsonObjectRequest(Request.Method.POST, registerTokenUrl, payload,
            { response: JSONObject ->
                FirebaseHandler.tokenRegistered = response["data"] == "OK"
            },
            { error ->
                Log.e("ERROR", error.message ?: "")
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