package com.egoi.egoipushlibrary.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Worker responsible for sending the interactions of the user with the notification to E-goi
 */
class RegisterEventWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val domain = "https://dev-push-wrapper.egoiapp.com"
    private val registerEventUrl = "${domain}/event"

    override fun doWork(): Result {
        var urlConnection: HttpsURLConnection? = null
        var success = false

        try {
            val payload = JSONObject()
            payload.put("api_key", inputData.getString("apiKey"))
            payload.put("app_id", inputData.getString("appId"))
            payload.put("contact", inputData.getString("contactId"))
            payload.put("os", "android")
            payload.put("message_hash", inputData.getString("messageHash"))
            payload.put("event", inputData.getString("event"))
            payload.put("device_id", inputData.getInt("deviceId", 0))

            val url = URL(registerEventUrl)

            urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.doInput = true
            urlConnection.setChunkedStreamingMode(0)

            val out = BufferedOutputStream(urlConnection.outputStream)

            val writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
            writer.write(payload.toString())
            writer.flush()

            val code: Int = urlConnection.responseCode

            if (code != 200) {
                throw IOException("Invalid response from server $code")
            }

            val result: String =
                urlConnection.inputStream.bufferedReader().use(BufferedReader::readText)

            val resultJson = JSONObject(result)

            if (resultJson.get("data") == "OK") {
                success = true
            }
        } catch (e: Exception) {
            e.message?.let { Log.d("EVENT_EXCEPTION", it) }
        } finally {
            Log.d("EVENT_REGISTER", success.toString())
            urlConnection?.disconnect()
        }

        return if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}