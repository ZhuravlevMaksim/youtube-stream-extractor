package com.ystract.services

import com.ystract.client.Client
import java.util.*
import java.util.regex.Pattern


class YoutubeUrlDecoder {

    var decryptionCode: String? = null
    var functionName: String? = null
    var decryptionFunctionFinal: String? = null

    fun loadDecryptionCode(url: String): String? {
        if (decryptionFunctionFinal != null) return decryptionFunctionFinal

        if (decryptionCode == null) {
            decryptionCode = Client.get("https://youtube.com$url").body?.string()
        }

        if (functionName == null) {
            functionName = findMatch(decryptionRegex, decryptionCode)
        }

        val decryptionFunction = "var " + findMatch(
            "(" + functionName!!.replace("$", "\\$") + "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})",
            decryptionCode
        ) + ";"

        val helperObject = findMatch(helperObjectRegex, decryptionFunction)
            .let { "(var " + it.replace("$", "\\$") + "=\\{.+?\\}\\};)" }
            .let { findMatch(it, decryptionCode!!.replace("\n", "")) }

        val callerFunction = "function decrypt(a){return $functionName(a);}"

        decryptionFunctionFinal = helperObject + decryptionFunction + callerFunction
        return decryptionFunctionFinal
    }

    private fun findMatch(patterns: List<Pattern>, string: String?): String {
        patterns.forEach {
            val matcher = it.matcher(string!!)
            val found = matcher.find()
            if (found) {
                return matcher.group(1)
            }
        }
        throw RuntimeException("No group found")
    }

    private fun findMatch(regex: String, string: String?): String {
        return findMatch(listOf(Pattern.compile(regex)), string)
    }

    private fun findMatch(pattern: Pattern, string: String?): String {
        return findMatch(listOf(pattern), string)
    }

    companion object {
        private val cache = object : LinkedHashMap<String, YoutubeUrlDecoder>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, YoutubeUrlDecoder>?): Boolean {
                return size >= 3
            }
        }
        private val helperObjectRegex = Pattern.compile(";([A-Za-z0-9_\\$]{2})\\...\\(")
        private val decryptionRegex = listOf<Pattern>(
            Pattern.compile("\\b([\\w$]{2})\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;"),
            Pattern.compile("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)"),
            Pattern.compile("([\\w$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;"),
            Pattern.compile("yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*c\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(:encodeURIComponent\\s*\\()([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(:encodeURIComponent\\s*\\()([a-zA-Z0-9$]+)\\(")
        )

        fun cachedInstance(js: String?): YoutubeUrlDecoder? {
            if (js == null){
                return null
            }else{
                return  if (cache.containsKey(js)) cache[js]!! else {
                    val youtubeUrlDecoder = YoutubeUrlDecoder()
                    cache[js] = youtubeUrlDecoder
                    return youtubeUrlDecoder
                }
            }
        }
    }
}
