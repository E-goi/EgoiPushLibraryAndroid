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
 * Worker responsible for registering/updating the Firebase token in E-goi's list
 */
class RegisterTokenWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val domain = "https://dev-push-wrapper.egoiapp.com"
    private val registerTokenUrl = "${domain}/token"

    override fun doWork(): Result {
        var urlConnection: HttpsURLConnection? = null
        var success = false

        try {
            val payload = JSONObject()
            payload.put("api_key", inputData.getString("apiKey"))
            payload.put("app_id", inputData.getString("appId"))
            payload.put("token", inputData.getString("token"))
            payload.put("os", "android")

            val twoStepsData = inputData.getString("twoStepsData")

            if (twoStepsData != null) {
                val data = JSONObject(twoStepsData)

                if (data.getString("field") !== "" &&
                    data.getString("value") !== ""
                ) {
                    payload.put("two_steps_data", data)
                }
            }

            val url = URL(registerTokenUrl)

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
            e.message?.let { Log.d("TOKEN_EXCEPTION", it) }
        } finally {
            Log.d("TOKEN_REGISTER", success.toString())
            urlConnection?.disconnect()
        }

        return if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}