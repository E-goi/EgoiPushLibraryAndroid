package com.egoiapp.egoipushlibrary.workers

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
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
            mailingId = inputData.getInt("mailingId", 0),
            deviceId = inputData.getInt("deviceId", 0),
            messageId = inputData.getInt("messageId", 0)
        )

        val builder: AlertDialog.Builder =
            AlertDialog.Builder(context)

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
                if (egoiNotification.messageHash != "TEST") {
                    EgoiPushLibrary.getInstance(context)
                        .registerEvent(EgoiPushLibrary.OPEN_EVENT, egoiNotification)
                }

                if (egoiNotification.actionType == "deeplink") {
                    EgoiPushLibrary.getInstance(context).deepLinkCallback?.let {
                        it(egoiNotification)
                    }
                } else {
                    launchActionUrl(egoiNotification.actionUrl)
                }
            }

            builder.setNegativeButton(egoiNotification.actionTextCancel)
            { _, _ ->
                if (egoiNotification.messageHash != "TEST") {
                    EgoiPushLibrary.getInstance(context)
                        .registerEvent(EgoiPushLibrary.CANCEL_EVENT, egoiNotification)
                }
            }
        }

        val mainHandler = Handler(context.applicationContext.mainLooper)

        val runnable = Runnable {
            builder.show()
        }

        mainHandler.post(runnable)

        return Result.success()
    }

    private fun launchActionUrl(url: String) {
        val allowedSchemes = setOf("https", "http")
        val uri = Uri.parse(url)

        if (uri.scheme?.lowercase() !in allowedSchemes) {
            Log.w("EgoiPushLibrary", "Esquema de URL nao suportado ou bloqueado: ${uri.scheme}")
            return
        }

        try {
            val uriIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val activityContext = EgoiPushLibrary.getInstance(context).activityContext
            activityContext.startActivity(uriIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("EgoiPushLibrary", "Nenhuma aplicacao instalada para responder ao URI: $url", e)
        } catch (e: Exception) {
            Log.e("EgoiPushLibrary", "Erro inesperado ao abrir o actionUrl: $url", e)
        }
    }
}
