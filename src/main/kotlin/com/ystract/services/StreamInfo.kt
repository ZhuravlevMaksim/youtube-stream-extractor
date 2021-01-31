package com.ystract.services

data class StreamInfo(
    var url: String,
    val mimeType: String,
    val bitrate: Double?,
    val contentLength: String
)
