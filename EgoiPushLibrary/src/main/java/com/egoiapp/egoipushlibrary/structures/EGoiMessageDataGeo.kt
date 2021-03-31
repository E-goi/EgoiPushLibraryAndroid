package com.egoiapp.egoipushlibrary.structures

data class EGoiMessageDataGeo(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var radius: Float = 0.1.toFloat(),
    var duration: Long = 0
)
