package com.smartpantry.app.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SharedHttp {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    const val USER_AGENT = "SmartPantry/1.2 (Android; educational; barcode-lookup)"
}
