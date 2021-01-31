package com.ystract.services

data class AudioStreamInfo(val uid: String, val title: String, val lengthSeconds: String, val audioStream: StreamInfo) {

    companion object {
        val re = Regex("[^A-Za-z0-9А-Яа-я ]")
        val ext = Regex("audio/(\\w+);")
    }

    public fun file(): String {
        return "${re.replace(title, "")}.${ext.find(audioStream.mimeType)!!.destructured.component1()}"
    }
}
