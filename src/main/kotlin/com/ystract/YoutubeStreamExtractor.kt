package com.ystract

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.ystract.client.Client
import com.ystract.services.AudioStreamInfo
import com.ystract.services.PlayerExtractor
import okhttp3.Headers
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

val headers = Headers.headersOf(
    "X-YouTube-Client-Name", "1",
    "X-YouTube-Client-Version", "2.20200214.04.00",
    "Accept-Language", "en-EN, en;q=0.9"
)

fun playlistUrl(uid: String) = "https://www.youtube.com/playlist?list=$uid"

class YoutubeStreamExtractor {

    companion object {
        public fun streamFromVideo(videoUid: String): AudioStreamInfo? {
            return PlayerExtractor(videoUid).loadPlayer().getAudioStream()
        }

        public fun streamsFromPlaylist(playlistUid: String, peek: ((AudioStreamInfo) -> Unit)? = null): List<AudioStreamInfo> {
            val response = Client.get("${playlistUrl(playlistUid)}&pbj=1", headers).body?.string()
            val list = Gson().fromJson(response, List::class.java)
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
                .mapNotNull { getVideoUrlOrNull(it) }
                .parallelStream()
                .map { PlayerExtractor(it).loadPlayer().getAudioStream() }
                .peek {
                    if (peek != null && it != null) {
                        peek(it)
                    }
                }
                .toList()
                .filterNotNull()
        }

        fun streamFromVideoAsync(videoUid: String): CompletableFuture<AudioStreamInfo>? {
            return CompletableFuture.supplyAsync {
                streamFromVideo(videoUid)
            }
        }

        fun streamFromPlaylistAsync(playlistUid: String): CompletableFuture<List<AudioStreamInfo>>? {
            return CompletableFuture.supplyAsync {
                streamsFromPlaylist(playlistUid)
            }
        }

    }
}

fun getVideoUrlOrNull(item: Any?): String? {
    return try {
        val video = (item as LinkedTreeMap<*, *>).getMap("playlistVideoRenderer")
        if (video.containsKey("upcomingEventData")) return null
        return video["videoId"] as String
    } catch (e: Exception) {
        null
    }
}