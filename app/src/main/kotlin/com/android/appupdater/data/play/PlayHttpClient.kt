package com.android.appupdater.data.play

import com.android.appupdater.data.api.SharedHttpClient
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.network.IHttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PlayHttpClient(private val userAgent: String) : IHttpClient {

    private val lastCode = MutableStateFlow(0)

    override val responseCode: StateFlow<Int> = lastCode.asStateFlow()

    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse =
        execute(Request.Builder().url(url).headers(headers.toHeaders()).post(body.toRequestBody()).build())

    override fun post(url: String, headers: Map<String, String>, params: Map<String, String>): PlayResponse =
        execute(
            Request.Builder()
                .url(url.withQuery(params))
                .headers(headers.toHeaders())
                .post(ByteArray(0).toRequestBody())
                .build()
        )

    override fun get(url: String, headers: Map<String, String>): PlayResponse = get(url, headers, emptyMap())

    override fun get(url: String, headers: Map<String, String>, params: Map<String, String>): PlayResponse =
        execute(Request.Builder().url(url.withQuery(params)).headers(headers.toHeaders()).build())

    override fun get(url: String, headers: Map<String, String>, paramString: String): PlayResponse =
        execute(Request.Builder().url(url + paramString).headers(headers.toHeaders()).build())

    override fun getAuth(url: String): PlayResponse =
        execute(Request.Builder().url(url).header("User-Agent", userAgent).build())

    override fun postAuth(url: String, body: ByteArray): PlayResponse =
        execute(
            Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .post(body.toRequestBody(JSON_MEDIA_TYPE))
                .build()
        )

    private fun execute(request: Request): PlayResponse =
        SharedHttpClient.instance.newCall(request).execute().use { response ->
            lastCode.value = response.code
            PlayResponse(
                responseBytes = response.body.bytes(),
                errorString = if (response.isSuccessful) "" else response.message.ifBlank { "HTTP ${response.code}" },
                isSuccessful = response.isSuccessful,
                code = response.code,
                type = response.header("Content-Type", DEFAULT_CONTENT_TYPE)
            )
        }

    private fun String.withQuery(params: Map<String, String>): HttpUrl =
        toHttpUrl().newBuilder().apply { params.forEach { (key, value) -> addQueryParameter(key, value) } }.build()

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
