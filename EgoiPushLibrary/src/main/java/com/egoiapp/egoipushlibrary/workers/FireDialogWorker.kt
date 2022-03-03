package com.egoiapp.egoipushlibrary.workers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.work.Worker
import androidx.work.WorkerParameters
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

        if (
            egoiNotification.actionType != "" &&
            egoiNotification.actionText != "" &&
            egoiNotification.actionUrl != "" &&
            egoiNotification.actionTextCancel != ""
        ) {
            builder.setPositiveButton(egoiNotification.actionText)
            { _, _ ->
                EgoiPushLibrary.getInstance(context.applicationContext)
                    .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)

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
                EgoiPushLibrary.getInstance(context.applicationContext)
                    .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
            }
        }

        val mainHandler = Handler(EgoiPushLibrary.getInstance(context).activityContext.mainLooper)

        val runnable = Runnable {
            builder.show()
        }

        mainHandler.post(runnable)

        return Result.success()
    }
}