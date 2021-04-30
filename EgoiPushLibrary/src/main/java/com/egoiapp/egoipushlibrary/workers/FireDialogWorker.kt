package com.egoiapp.egoipushlibrary.workers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiNotification

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

        egoiNotification = EgoiNotification(
            title = inputData.getString("title") ?: "",
            body = inputData.getString("body") ?: "",
            actionType = inputData.getString("actionType") ?: "",
            actionText = inputData.getString("actionText") ?: "",
            actionUrl = inputData.getString("actionUrl") ?: "",
            actionTextCancel = inputData.getString("actionTextCancel") ?: "",
            apiKey = inputData.getString("apiKey") ?: "",
            appId = inputData.getString("appId") ?: "",
            contactId = inputData.getString("contactId") ?: "",
            messageHash = inputData.getString("messageHash") ?: "",
            deviceId = inputData.getInt("deviceId", 0),
            messageId = inputData.getInt("messageId", 0)
        )

        builder.setTitle(egoiNotification.title)
        builder.setMessage(egoiNotification.body)

        if (egoiNotification.actionType != "" && egoiNotification.actionText != "" && egoiNotification.actionUrl != "" && egoiNotification.actionTextCancel != "") {
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

            builder.setNegativeButton(egoiNotification.actionTextCancel)
            { _, _ ->
                registerEvent(event = "canceled")
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