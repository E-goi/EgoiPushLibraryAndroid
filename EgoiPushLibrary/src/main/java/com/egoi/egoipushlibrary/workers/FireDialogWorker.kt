package com.egoi.egoipushlibrary.workers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.egoi.egoipushlibrary.EgoiPushLibrary

/**
 * Worker responsible for creating and displaying a dialog to the user
 */
class FireDialogWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private lateinit var apiKey: String
    private lateinit var appId: Number
    private lateinit var contactId: String
    private lateinit var messageHash: String
    private lateinit var deviceId: Number

    override fun doWork(): Result {
        val builder: AlertDialog.Builder = AlertDialog.Builder(EgoiPushLibrary.getInstance().context)

        // Dialog data
        val title = inputData.getString("title")
        val body = inputData.getString("body")
        val actionType = inputData.getString("actionType")
        val actionText = inputData.getString("actionText")
        val actionUrl = inputData.getString("actionUrl")

        // Event data
        apiKey = inputData.getString("apiKey") ?: ""
        appId = inputData.getInt("appId", 0)
        contactId = inputData.getString("contactId") ?: ""
        messageHash = inputData.getString("messageHash") ?: ""
        deviceId = inputData.getInt("deviceId", 0)

        builder.setTitle(title)
        builder.setMessage(body)

        builder.setNegativeButton(EgoiPushLibrary.getInstance().closeLabel)
        { _, _ ->
            registerEvent(event = "canceled")
        }

        if (actionType != null && actionText != null && actionUrl != null) {
            builder.setPositiveButton(actionText)
            { _, _ ->
                registerEvent(event = "open")

                if (actionType == "deeplink") {
                    EgoiPushLibrary.getInstance().deepLinkCallback?.let { it(actionUrl) }
                } else {
                    EgoiPushLibrary.getInstance().context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(actionUrl)
                        )
                    )
                }
            }
        }

        val mainHandler = Handler(EgoiPushLibrary.getInstance().context.mainLooper)

        val runnable = Runnable {
            builder.show()
        }

        mainHandler.post(runnable)

        return Result.success()
    }

    private fun registerEvent(event: String) {
        EgoiPushLibrary.getInstance().requestWork(
            OneTimeWorkRequestBuilder<RegisterEventWorker>()
                .setInputData(
                    workDataOf(
                        "apiKey" to apiKey,
                        "appId" to appId,
                        "contactId" to contactId,
                        "messageHash" to messageHash,
                        "event" to event,
                        "deviceId" to deviceId
                    )
                )
                .build()
        )
    }
}