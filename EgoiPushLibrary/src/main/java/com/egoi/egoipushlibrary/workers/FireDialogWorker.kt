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
import com.egoi.egoipushlibrary.structures.EgoiNotification

/**
 * Worker responsible for creating and displaying a dialog to the user
 */
class FireDialogWorker(
    val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private lateinit var egoiNotification: EgoiNotification

    override fun doWork(): Result {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(EgoiPushLibrary.getInstance(context).activityContext)

        // Dialog data
        egoiNotification.title = inputData.getString("title") ?: ""
        egoiNotification.body = inputData.getString("body") ?: ""
        egoiNotification.actionType = inputData.getString("actionType") ?: ""
        egoiNotification.actionText = inputData.getString("actionText") ?: ""
        egoiNotification.actionUrl = inputData.getString("actionUrl") ?: ""

        // Event data
        egoiNotification.apiKey = inputData.getString("apiKey") ?: ""
        egoiNotification.appId = inputData.getString("appId") ?: ""
        egoiNotification.contactId = inputData.getString("contactId") ?: ""
        egoiNotification.messageHash = inputData.getString("messageHash") ?: ""
        egoiNotification.deviceId = inputData.getInt("deviceId", 0)

        builder.setTitle(egoiNotification.title)
        builder.setMessage(egoiNotification.body)

        builder.setNegativeButton(EgoiPushLibrary.getInstance(context).closeLabel)
        { _, _ ->
            registerEvent(event = "canceled")
        }

        if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "") {
            builder.setPositiveButton(egoiNotification.actionText)
            { _, _ ->
                registerEvent(event = "open")

                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(context).deepLinkCallback?.let {
                        it(egoiNotification)
                    }
                } else {
                    EgoiPushLibrary.getInstance(context).activityContext.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(egoiNotification.actionUrl)
                        )
                    )
                }
            }
        }

        val mainHandler = Handler(EgoiPushLibrary.getInstance(context).activityContext.mainLooper)

        val runnable = Runnable {
            builder.show()
        }

        mainHandler.post(runnable)

        return Result.success()
    }

    private fun registerEvent(event: String) {
        EgoiPushLibrary.getInstance(context).requestWork(
            workRequest = OneTimeWorkRequestBuilder<RegisterEventWorker>()
                .setInputData(
                    workDataOf(
                        "apiKey" to egoiNotification.apiKey,
                        "appId" to egoiNotification.appId,
                        "contactId" to egoiNotification.contactId,
                        "messageHash" to egoiNotification.messageHash,
                        "event" to event,
                        "deviceId" to egoiNotification.deviceId
                    )
                )
                .build()
        )
    }
}