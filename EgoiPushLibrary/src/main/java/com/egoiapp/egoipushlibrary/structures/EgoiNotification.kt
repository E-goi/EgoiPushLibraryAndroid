package com.egoiapp.egoipushlibrary.structures

data class EgoiNotification(
    var title: String = "",
    var body: String = "",
    var actionType: String = "",
    var actionText: String = "",
    var actionUrl: String = "",
    var actionTextCancel: String = "",
    var apiKey: String,
    var appId: String,
    var contactId: String,
    var messageHash: String,
    var mailingId: Int = 0,
    var deviceId: Int = 0,
    var messageId: Int = 0
)