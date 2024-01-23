package com.egoiapp.egoipushlibrary.structures

data class EGoiMessageData(
    var os: String,
    var messageHash: String,
    var mailingId: Int,
    var listId: Int,
    var contactId: String,
    var accountId: Int,
    var applicationId: String,
    var messageId: Int,
    var geo: EGoiMessageDataGeo = EGoiMessageDataGeo(),
    var actions: EGoiMessageDataAction = EGoiMessageDataAction()
)
