package com.egoiapp.egoipushlibrary.handlers

import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.*
import com.egoiapp.egoipushlibrary.workers.FireDialogWorker
import com.egoiapp.egoipushlibrary.workers.FireNotificationWorker
import com.egoiapp.egoipushlibrary.workers.RegisterTokenWorker
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Class responsible for handling every operation related with Firebase and the tokens
 */
class FirebaseHandler(
    private val instance: EgoiPushLibrary
) {
    private var message: EGoiMessage? = null
    private var geoPush: Boolean = false

    /**
     * Update the token saved in the library
     * @param token The token to be saved
     */
    fun updateToken(token: String) {
        if (tokenRegistered && token != Companion.token) {
            registerToken(token = token)
        }
    }

    /**
     * Register the token in E-goi's contact list. If it already exists, updates the contact
     * @param token The token to be registered in E-goi
     * @param field The column in E-goi's list that will be written in (optional)
     * @param value The value to be written in the field defined above (optional)
     */
    fun registerToken(token: String, field: String = "", value: String = ""): OneTimeWorkRequest {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        Companion.token = token

        if (field != "" && value != "") {
            Companion.field = field
            Companion.value = value
        }

        var twoStepsData: JSONObject? = null

        if (Companion.field != "" && Companion.value != "") {
            twoStepsData = JSONObject()
            twoStepsData.put("field", field)
            twoStepsData.put("value", value)
        }

        val workRequest = OneTimeWorkRequestBuilder<RegisterTokenWorker>()
            .setInputData(
                workDataOf(
                    "apiKey" to preferences.apiKey,
                    "appId" to preferences.appId,
                    "token" to token,
                    "twoStepsData" to twoStepsData.toString()
                )
            )
            .build()

        instance.requestWork(workRequest)

        return workRequest
    }

    /**
     * Handle notifications received. If it is a notification with geolocation, creates a geofence
     * that will trigger the notification. Otherwise, displays a dialog to the user.
     */
    fun messageReceived() {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        message?.let {
            if (!geoPush) {
                fireNotification()
            } else if (preferences.geoEnabled){
                instance.addGeofence(it)
                geoPush = false
            }
            message = null
        }
    }

    /**
     * Build a local message with the data received from the remote notification
     * @param intent The intent of the notification received
     */
    fun processMessage(intent: Intent) {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        val extras = intent.extras

        if (extras != null && extras.getString("key") == "E-GOI_PUSH") {
            if ((preferences.geoEnabled && !instance.location.checkLocationPermissions() &&
                        extras.containsKey("latitude") && extras.getString("latitude") != "")
                || (!preferences.geoEnabled && extras.containsKey("latitude") && extras.getString("latitude") != "")
            ) {
                return
            }

            this.message = EGoiMessage(
                notification = EGoiMessageNotification(
                    title = extras.getString("title") ?: "",
                    body = extras.getString("body") ?: "",
                    image = extras.getString("image") ?: ""
                ),
                data = EGoiMessageData(
                    os = extras.getString("os") ?: "android",
                    messageHash = extras.getString("message-hash") ?: "",
                    listId = if (extras.getString("list-id", "0") != "")
                        extras.getString("list-id", "0").toInt() else 0,
                    contactId = extras.getString("contact-id") ?: "",
                    accountId = if (extras.getString("account-id", "0") != "")
                        extras.getString("account-id", "0").toInt() else 0,
                    applicationId = extras.getString("application-id") ?: "",
                    messageId = if (extras.getString("message-id", "0") != "")
                        extras.getString("message-id", "0").toInt() else 0,
                )
            )

            message?.let {
                // [Assign action to notification]
                val actionsJson = extras.getString("actions")

                if (actionsJson != null) {
                    val actions = JSONObject(actionsJson)

                    if (
                        actions.has("type") &&
                        actions.has("text") &&
                        actions.has("url") &&
                        actions.has("text-cancel")
                    ) {
                        it.data.actions.type = actions.getString("type")
                        it.data.actions.text = actions.getString("text")
                        it.data.actions.url = actions.getString("url")
                        it.data.actions.textCancel = actions.getString("text-cancel")
                    }
                }

                // [Handle geolocation]
                if (
                    preferences.geoEnabled &&
                    instance.location.checkLocationPermissions() &&
                    extras.containsKey("latitude") && extras.getString("latitude") != "" &&
                    extras.containsKey("longitude") && extras.getString("longitude") != "" &&
                    extras.containsKey("radius") && extras.getString("radius") != ""
                ) {
                    this.geoPush = true
                    it.data.geo.latitude =
                        extras.getString("latitude")?.toDouble() ?: Double.NaN
                    it.data.geo.longitude =
                        extras.getString("longitude")?.toDouble() ?: Double.NaN
                    it.data.geo.radius = extras.getString("radius")?.toFloat() ?: 0.0.toFloat()
                    it.data.geo.duration = extras.getString("duration")?.toLong() ?: 0
                    it.data.geo.periodStart = extras.getString("time-start")
                    it.data.geo.periodEnd = extras.getString("time-end")
                }
            }
        }
    }

    /**
     * Validate if the intent received was from a click on a notification. If it was, display a
     * dialog to the user.
     * @param intent The intent of the notification received
     */
    fun showDialog(intent: Intent) {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        if (
            intent.action == "com.google.firebase.messaging.NOTIFICATION_OPEN"
        ) {
            if (instance.dialogCallback != null) {
                runBlocking {

                    message?.let {
                        val egoiNotification = EgoiNotification(
                            title = it.notification.title,
                            body = it.notification.body,
                            actionType = it.data.actions.type,
                            actionText = it.data.actions.text,
                            actionUrl = it.data.actions.url,
                            actionTextCancel = it.data.actions.textCancel,
                            apiKey = preferences.apiKey,
                            appId = preferences.appId,
                            contactId = it.data.contactId,
                            messageHash = it.data.messageHash,
                            messageId = it.data.messageId
                        )

                        instance.dialogCallback?.let {
                            it(egoiNotification)
                        }
                    }
                }
            } else {
                fireDialog()
            }
        }
    }

    /**
     * Display a dialog with the content of the notification to the user when the notification is
     * pressed in the notification bar.
     */
    private fun fireDialog() {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        runBlocking {
            message?.let {
                instance.requestWork(
                    workRequest = OneTimeWorkRequestBuilder<FireDialogWorker>()
                        .setInputData(
                            workDataOf(
                                /* Dialog Data */
                                "title" to it.notification.title,
                                "body" to it.notification.body,
                                "actionType" to it.data.actions.type,
                                "actionText" to it.data.actions.text,
                                "actionUrl" to it.data.actions.url,
                                "actionTextCancel" to it.data.actions.textCancel,
                                /* Event Data*/
                                "apiKey" to preferences.apiKey,
                                "appId" to preferences.appId,
                                "contactId" to it.data.contactId,
                                "messageHash" to it.data.messageHash,
                                "messageId" to it.data.messageId
                            )
                        )
                        .build()
                )
            }
        }
    }

    private fun fireNotification() {
        val preferences: EgoiPreferences = instance.dataStore.getDSPreferences()

        runBlocking {
            message?.let {
                instance.requestWork(
                    workRequest = OneTimeWorkRequestBuilder<FireNotificationWorker>()
                        .setInputData(
                            workDataOf(
                                "title" to it.notification.title,
                                "text" to it.notification.body,
                                "image" to it.notification.image,
                                "actionType" to it.data.actions.type,
                                "actionText" to it.data.actions.text,
                                "actionUrl" to it.data.actions.url,
                                "actionTextCancel" to it.data.actions.textCancel,
                                "apiKey" to preferences.apiKey,
                                "appId" to preferences.appId,
                                "contactId" to it.data.contactId,
                                "messageHash" to it.data.messageHash,
                                "messageId" to it.data.messageId
                            )
                        )
                        .build()
                )
            }
        }
    }

    companion object {
        var tokenRegistered: Boolean = false

        private var token: String = ""
        private var field: String = ""
        private var value: String = ""
    }
}