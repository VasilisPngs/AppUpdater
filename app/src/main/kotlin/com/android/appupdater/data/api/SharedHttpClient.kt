package com.android.appupdater.data.api

import okhttp3.OkHttpClient
import okhttp3.android.AndroidDns
import java.util.concurrent.TimeUnit

object SharedHttpClient {
    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(AndroidDns())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
