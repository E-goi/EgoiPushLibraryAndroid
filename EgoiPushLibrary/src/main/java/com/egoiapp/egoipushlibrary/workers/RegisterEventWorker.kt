package com.egoiapp.egoipushlibrary.workers

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
    private val domain = "https://api.egoiapp.com/push/apps/"
    private val registerEventUrl = "/event"

    override fun doWork(): Result {
        var urlConnection: HttpsURLConnection? = null
        var success = false

        if (inputData.getString("contactId") == "") return Result.success()

        try {
            val payload = JSONObject()
            payload.put("contact", inputData.getString("contactId"))
            payload.put("os", "android")
            payload.put("message_hash", inputData.getString("messageHash"))
            payload.put("mailing_id", inputData.getInt("mailingId", 0))
            payload.put("event", inputData.getString("event"))
            payload.put("device_id", inputData.getInt("deviceId", 0))

            val url = URL(domain + inputData.getString("appId") + registerEventUrl)

            urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.setRequestProperty("Apikey", inputData.getString("apiKey"))
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.doInput = true
            urlConnection.setChunkedStreamingMode(0)

            val out = BufferedOutputStream(urlConnection.outputStream)

            val writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
            writer.write(payload.toString())
            writer.flush()

            val code: Int = urlConnection.responseCode

            if (code != 202) {
                throw IOException("Invalid response from server $code")
            }

            val result: String =
                urlConnection.inputStream.bufferedReader().use(BufferedReader::readText)

            val resultJson = JSONObject(result)

            if (resultJson.getBoolean("success")) {
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