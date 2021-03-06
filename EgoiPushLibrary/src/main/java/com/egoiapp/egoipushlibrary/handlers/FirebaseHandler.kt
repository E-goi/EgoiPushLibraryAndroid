package com.egoiapp.egoipushlibrary.handlers

import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EGoiMessage
import com.egoiapp.egoipushlibrary.structures.EGoiMessageData
import com.egoiapp.egoipushlibrary.structures.EGoiMessageNotification
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import com.egoiapp.egoipushlibrary.workers.FireDialogWorker
import com.egoiapp.egoipushlibrary.workers.FireNotificationWorker
import com.egoiapp.egoipushlibrary.workers.RegisterTokenWorker
import org.json.JSONObject

/**
 * Class responsible for handling every operation related with Firebase and the tokens
 */
class FirebaseHandler(
    private val instance: EgoiPushLibrary
) {
    private lateinit var message: EGoiMessage

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
    fun registerToken(token: String, field: String = "", value: String = "") {
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

        val preferences: EgoiPreferences? =
            instance.dataStore.getDSPreferences()

        if (preferences != null) {
            instance.requestWork(
                workRequest = OneTimeWorkRequestBuilder<RegisterTokenWorker>()
                    .setInputData(
                        workDataOf(
                            "apiKey" to preferences.apiKey,
                            "appId" to preferences.appId,
                            "token" to token,
                            "twoStepsData" to twoStepsData.toString()
                        )
                    )
                    .build()
            )
        }
    }

    /**
     * Handle notifications received. If it is a notification with geolocation, creates a geofence
     * that will trigger the notification. Otherwise, displays a dialog to the user.
     */
    fun messageReceived() {
        if (this::message.isInitialized) {
            if (!geoPush) {
                fireNotification()
            } else {
                instance.addGeofence(message)
            }
        }
    }

    /**
     * Build a local message with the data received from the remote notification
     * @param intent The intent of the notification received
     */
    fun processMessage(intent: Intent) {
        val extras = intent.extras

        if (extras != null && extras.getString("key") == "E-GOI_PUSH") {
            this.message = EGoiMessage(
                notification = EGoiMessageNotification(
                    title = extras.getString("title") ?: "",
                    body = extras.getString("body") ?: "",
                    image = extras.getString("image") ?: ""
                ),
                data = EGoiMessageData(
                    os = extras.getString("os") ?: "android",
                    messageHash = extras.getString("message-hash") ?: "",
                    listId = extras.getString("list-id")?.toInt() ?: 0,
                    contactId = extras.getString("contact-id") ?: "",
                    accountId = extras.getString("account-id")?.toInt() ?: 0,
                    applicationId = extras.getString("application-id") ?: "",
                    messageId = extras.getString("message-id")?.toInt() ?: 0,
                    deviceId = extras.getString("device-id")?.toInt() ?: 0,
                )
            )

            // [Assign action to notification]
            val actionsJson = extras.getString("actions")

            if (actionsJson != null) {
                val actions = JSONObject(actionsJson)

                if (actions.has("type") && actions.has("text") && actions.has("url")) {
                    this.message.data.actions.type = actions.getString("type")
                    this.message.data.actions.text = actions.getString("text")
                    this.message.data.actions.url = actions.getString("url")
                }
            }

            // [Handle geolocation]
            if (
                instance.location.checkPermissions() &&
                extras.containsKey("latitude") && extras.getString("latitude") != "" &&
                extras.containsKey("longitude") && extras.getString("longitude") != "" &&
                extras.containsKey("radius") && extras.getString("radius") != ""
            ) {
                this.message.data.geo.latitude =
                    extras.getString("latitude")?.toDouble() ?: Double.NaN
                this.message.data.geo.longitude =
                    extras.getString("longitude")?.toDouble() ?: Double.NaN
                this.message.data.geo.radius = extras.getString("radius")?.toFloat() ?: Float.NaN

                this.geoPush = true
            }
        }
    }

    /**
     * Validate if the intent received was from a click on a notification. If it was, display a
     * dialog to the user.
     * @param intent The intent of the notification received
     */
    fun showDialog(intent: Intent) {
        if (
            intent.action == "com.google.firebase.messaging.NOTIFICATION_OPEN" &&
            this::message.isInitialized
        ) {
            fireDialog()
        }
    }

    /**
     * Display a dialog with the content of the notification to the user when the notification is
     * pressed in the notification bar.
     */
    private fun fireDialog() {
        val preferences: EgoiPreferences? =
            instance.dataStore.getDSPreferences()

        if (preferences != null) {
            instance.requestWork(
                workRequest = OneTimeWorkRequestBuilder<FireDialogWorker>()
                    .setInputData(
                        workDataOf(
                            /* Dialog Data */
                            "title" to message.notification.title,
                            "body" to message.notification.body,
                            "actionType" to message.data.actions.type,
                            "actionText" to message.data.actions.text,
                            "actionUrl" to message.data.actions.url,
                            /* Event Data*/
                            "apiKey" to preferences.apiKey,
                            "appId" to preferences.appId,
                            "contactId" to message.data.contactId,
                            "messageHash" to message.data.messageHash,
                            "deviceId" to message.data.deviceId,
                            "messageId" to message.data.messageId
                        )
                    )
                    .build()
            )
        }
    }

    private fun fireNotification() {
        val preferences: EgoiPreferences? =
            instance.dataStore.getDSPreferences()

        if (preferences != null) {
            instance.requestWork(
                workRequest = OneTimeWorkRequestBuilder<FireNotificationWorker>()
                    .setInputData(
                        workDataOf(
                            "title" to message.notification.title,
                            "text" to message.notification.body,
                            "image" to message.notification.image,
                            "actionType" to message.data.actions.type,
                            "actionText" to message.data.actions.text,
                            "actionUrl" to message.data.actions.url,
                            "apiKey" to preferences.apiKey,
                            "appId" to preferences.appId,
                            "contactId" to message.data.contactId,
                            "messageHash" to message.data.messageHash,
                            "deviceId" to message.data.deviceId,
                            "messageId" to message.data.messageId
                        )
                    )
                    .build()
            )
        }
    }

    companion object {
        var tokenRegistered: Boolean = false

        private var token: String = ""
        private var field: String = ""
        private var value: String = ""
    }
}