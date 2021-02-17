package com.egoiapp.egoipushlibrary.structures

data class EgoiNotification(
    var title: String,
    var body: String,
    var actionType: String,
    var actionText: String,
    var actionUrl: String,
    var apiKey: String,
    var appId: String,
    var contactId: String,
    var messageHash: String,
    var deviceId: Int,
    var messageId: Int
)