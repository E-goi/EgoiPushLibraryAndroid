package com.egoi.egoipushlibrary.structures

data class EGoiMessage(
    var notification: EGoiMessageNotification,
    var data: EGoiMessageData
)