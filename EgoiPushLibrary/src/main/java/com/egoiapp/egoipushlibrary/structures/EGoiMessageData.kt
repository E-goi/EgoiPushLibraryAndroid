package com.egoiapp.egoipushlibrary.structures

data class EGoiMessageData(
    var os: String,
    var messageHash: String,
    var listId: Int,
    var contactId: String,
    var accountId: Int,
    var applicationId: String,
    var messageId: Int,
    var deviceId: Int,
    var geo: EGoiMessageDataGeo = EGoiMessageDataGeo(),
    var actions: EGoiMessageDataAction = EGoiMessageDataAction()
)
