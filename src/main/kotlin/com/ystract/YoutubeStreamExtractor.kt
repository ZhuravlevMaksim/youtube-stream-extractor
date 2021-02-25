package com.ystract

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.ystract.client.Client
import com.ystract.services.AudioStreamInfo
import com.ystract.services.Player
import okhttp3.Headers
import kotlin.streams.toList


val headers = Headers.headersOf(
    "X-YouTube-Client-Name", "1",
    "X-YouTube-Client-Version", "2.20200214.04.00",
    "Accept-Language", "en-EN, en;q=0.9"
)

class YoutubeStreamExtractor {

    companion object {
        public fun streamFromVideo(videoUid: String, unwrap: Boolean = false): AudioStreamInfo? {
            val response = Client.get("https://www.youtube.com/watch?v=$videoUid&pbj=1", headers)
            val responseList = Gson().fromJson(response.body?.string(), List::class.java)
            val fromJson = responseList[2] as LinkedTreeMap<*, *>

            if (fromJson.contains("playerResponse")) {
                val playerResponse = fromJson.getMap("playerResponse")
                val loadBaseJs = if (unwrap) JsUrlExtractor.getJsUrl(videoUid) else null
                return Player(videoUid, playerResponse, loadBaseJs).getAudioStream()
            } else if (fromJson.contains("player")) {
                val player = fromJson.getMap("player")
                val playerResponse = player.getMap("args")["player_response"] as String
                val js = if (unwrap) player.getMap("assets")["js"] as String else null
                return Player(videoUid, Gson().fromJson(playerResponse, LinkedTreeMap::class.java), js).getAudioStream()
            }
            return null
        }

        public fun streamsFromPlaylist(playlistUid: String, unwrap: Boolean = false): List<AudioStreamInfo> {
            val response = Client.get("https://www.youtube.com/playlist?list=$playlistUid&pbj=1", headers)
            val list = Gson().fromJson(response.body?.string(), List::class.java)

            return (list[1] as LinkedTreeMap<*, *>).getMap("response")
                .getMap("contents")
                .getMap("twoColumnBrowseResultsRenderer")
                .getList("tabs").getMap(0)
                .getMap("tabRenderer")
                .getMap("content")
                .getMap("sectionListRenderer")
                .getList("contents").getMap(0)
                .getMap("itemSectionRenderer")
                .getList("contents").getMap(0)
                .getMap("playlistVideoListRenderer")
                .getList("contents")
                .mapNotNull {
                    try {
                        val video = (it as LinkedTreeMap<*, *>).getMap("playlistVideoRenderer")
                        if (video.containsKey("upcomingEventData")) {
                            null
                        } else {
                            video["videoId"] as String
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .parallelStream()
                .map { streamFromVideo(it, unwrap) }
                .toList()
                .filterNotNull()
        }
    }
}

object JsUrlExtractor {
    @Volatile
    private var jsUrl: String? = null
    private val findPlayerJsUrl = "\"([/|\\w.]+base\\.js)\"".toRegex()

    @Synchronized
    fun getJsUrl(videoUid: String): String {
        return if (jsUrl == null) {
            return load(videoUid).also {
                jsUrl = it
            }
        } else jsUrl!!
    }

    private fun load(videoUid: String): String {
        return playerJs(Client.get("https://www.youtube.com/embed/${videoUid}", headers).body!!.string())
    }

    private fun playerJs(body: String) = findPlayerJsUrl.find(body)!!.destructured.component1()
}