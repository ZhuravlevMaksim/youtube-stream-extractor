package com.ystract.services

import com.google.gson.internal.LinkedTreeMap
import com.ystract.getList
import com.ystract.getMap
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import java.net.URLDecoder
import java.util.*

data class Player(
    val uid: String,
    val playerData: LinkedTreeMap<*, *>,
    val js: String,
    val decoder: YoutubeUrlDecoder? = YoutubeUrlDecoder.cachedInstance(js)
) {

    private val title by lazy { playerData.getMap("videoDetails")["title"] as String }
    private val lengthSeconds by lazy { playerData.getMap("videoDetails")["lengthSeconds"] as String }

    private val adaptiveFormats by lazy {
        playerData
            .getMap("streamingData")
            .getList("adaptiveFormats") as List<LinkedTreeMap<String, *>>
    }

    fun getAudioStream(): AudioStreamInfo? {
        return try {
            return AudioStreamInfo(uid, title, lengthSeconds, getAudio())
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

    private fun getAudio(): StreamInfo {
        val audioStreamInfo = adaptiveFormats.firstOrNull { isMP3(it) } ?: adaptiveFormats.filter { isAudio(it) }
            .maxByOrNull { it["bitrate"] as Double }

        return extract(audioStreamInfo)
    }

    private fun extract(item: LinkedTreeMap<String, *>?): StreamInfo {
        item as LinkedTreeMap<String, *>
        val formatCipher = item["cipher"] as String?
        val signatureCipher = item["signatureCipher"] as String?
        val actualUrl = if (formatCipher == null && signatureCipher == null) {
            item["url"] as String
        } else {
            val loadDecryptionCode = this.decoder?.loadDecryptionCode(js)
            val cipherString: String = formatCipher ?: signatureCipher!!
            val cipher = compatParseMap(cipherString)
            cipher["url"] + "&" + cipher["sp"] + "=" + decryptSignature(cipher["s"]!!, loadDecryptionCode!!)
        }
        return StreamInfo(
            actualUrl,
            item["mimeType"] as String,
            item["bitrate"] as Double
        )
    }

    val isAudio: (LinkedTreeMap<String, *>) -> Boolean = { map -> (map["mimeType"] as String).contains("audio") }
    val isMP3: (LinkedTreeMap<String, *>) -> Boolean = { map -> (map["mimeType"] as String).contains("mp3") }
}

fun decryptSignature(encryptedSig: String, decryptionCode: String): String? {
    val context = Context.enter()
    context.optimizationLevel = -1

    try {
        val scope = context.initStandardObjects()
        context.evaluateString(scope, decryptionCode, "decryptionCode", 1, null)
        val decryptionFunc: Function = scope.get("decrypt", scope) as Function
        val result: String? = decryptionFunc.call(context, scope, scope, arrayOf<Any>(encryptedSig)) as String
        return result ?: ""
    } catch (e: Exception) {
        throw  RuntimeException("could not get decrypt signature", e)
    } finally {
        Context.exit()
    }
}

fun compatParseMap(input: String): Map<String, String> {
    val map: MutableMap<String, String> = HashMap()
    for (arg in input.split("&".toRegex()).toTypedArray()) {
        val splitArg = arg.split("=".toRegex()).toTypedArray()
        if (splitArg.size > 1) {
            map[splitArg[0]] = URLDecoder.decode(splitArg[1], "UTF-8")
        } else {
            map[splitArg[0]] = ""
        }
    }
    return map
}