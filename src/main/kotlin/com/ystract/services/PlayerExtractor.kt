package com.ystract.services

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.ystract.client.Client
import com.ystract.getMap
import com.ystract.headers
import java.time.Duration

const val RETRY = 5

class PlayerExtractor(private val videoUid: String) {

    private val js by lazy {
        JsUrlExtractor.getJsUrl(videoUid)
    }

    fun loadPlayer(): Player {
        for (i in 1..RETRY) {
            val response = Client.get("https://www.youtube.com/watch?v=$videoUid&pbj=1", headers)
            val responseList = Gson().fromJson(response.body?.string(), List::class.java)
            val fromJson = responseList[2] as LinkedTreeMap<*, *>
            try {
                return extractPlayer(fromJson)
            } catch (e: TypeCastException) {
                println("No 'playerResponse'. No 'player'. ${if (i < RETRY) "Retrying" else "Stopping"}")
                Thread.sleep(Duration.ofSeconds(3).toMillis())
            }
        }
        error("No player info exception")
    }

    private fun extractPlayer(fromJson: LinkedTreeMap<*, *>): Player {
        if (fromJson.contains("playerResponse")) {
            val playerResponse = fromJson.getMap("playerResponse")
            val loadBaseJs = js
            return Player(videoUid, playerResponse, loadBaseJs)
        }
        if (fromJson.contains("player")) {
            val player = fromJson.getMap("player")
            val args = player.getMap("args")
            val assets = player.getMap("assets")
            val js = assets["js"] as String
            val playerResponse = Gson().fromJson(args["player_response"] as String, LinkedTreeMap::class.java)
            return Player(videoUid, playerResponse, js)
        }
        error("No player info exception")
    }
}


object JsUrlExtractor {
    @Volatile private var jsUrl: String? = null
    private val findPlayerJsUrl = "\"([/|\\w.]+base\\.js)\"".toRegex()

    @Synchronized fun getJsUrl(videoUid: String): String {
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
