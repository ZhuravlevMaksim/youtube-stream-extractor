package com.ystract.client

import okhttp3.*


object Client {

    private val client: OkHttpClient by lazy { OkHttpClient() }

    fun head(url: String, headers: Headers = Headers.headersOf()): Response {
        return Request.Builder()
                .head()
                .url(url)
                .headers(headers)
                .build().let { call(it) }
    }

    fun get(url: String, headers: Headers = Headers.headersOf()): Response {
        return Request.Builder()
                .get()
                .url(url)
                .headers(headers)
                .build().let { call(it) }
    }

    fun post(url: String, body: RequestBody, headers: Headers = Headers.headersOf()): Response {
        return Request.Builder()
                .post(body)
                .url(url)
                .headers(headers)
                .build().let { call(it) }
    }

    private fun call(request: Request): Response {
        return client.newCall(request).execute()
    }

}
