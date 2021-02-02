package com.ystract.services

import com.ystract.client.Client
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import okhttp3.Headers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.function.Supplier
import kotlin.streams.toList

val headers = Headers.headersOf(
    "X-YouTube-Client-Name", "1",
    "X-YouTube-Client-Version", "2.20200214.04.00",
    "Accept-Language", "en-EN, en;q=0.9"
)


fun playlistUrl(uid: String) = "https://www.youtube.com/playlist?list=$uid"

class YoutubeService {

    public fun fromVideo(videoUid: String): AudioStreamInfo? {
        return PlayerExtractor(videoUid).loadPlayer().getAudioStream()
    }

    public fun fromPlaylist(playlistUid: String, peek: ((AudioStreamInfo) -> Unit)? = null): List<AudioStreamInfo> {
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

    fun fromVideoAsync(videoUid: String): CompletableFuture<AudioStreamInfo>? {
        return CompletableFuture.supplyAsync {
            fromVideo(videoUid)
        }
    }

    fun fromPlaylistAsync(playlistUid: String): CompletableFuture<List<AudioStreamInfo>>? {
        return CompletableFuture.supplyAsync {
            fromPlaylist(playlistUid)
        }
    }

    private fun getVideoUrlOrNull(item: Any?): String? {
        return try {
            val video = (item as LinkedTreeMap<*, *>).getMap("playlistVideoRenderer")
            if (video.containsKey("upcomingEventData")) return null
            return video["videoId"] as String
        } catch (e: Exception) {
            null
        }
    }
}

fun LinkedTreeMap<*, *>.getMap(key: String): LinkedTreeMap<*, *> {
    try {
        return this[key] as LinkedTreeMap<*, *>
    } catch (e: Exception) {
    }
    throw Exception("null key $key")
}

fun LinkedTreeMap<*, *>.getList(key: String): List<*> {
    try {
        return this[key] as List<*>
    } catch (e: Exception) {
    }
    throw Exception("null key $key")
}

fun List<*>.getMap(index: Int): LinkedTreeMap<*, *> {
    try {
        return this[index] as LinkedTreeMap<*, *>
    } catch (e: Exception) {
    }
    throw Exception("null index $index")
}
