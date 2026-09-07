package org.appdevncsu.foodfinder.server

import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.appdevncsu.foodfinder.shared.dataDir
import java.io.File
import java.util.concurrent.TimeUnit

class CachedImage(val bytes: ByteArray, val contentType: String?)

object Images {
    private const val IMAGE_HOST = "dining.ncsu.edu"
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(dataDir(), "image-cache"), 64L * 1024 * 1024))
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetch(imageUrl: String): CachedImage? {
        val url = resolveImageUrl(imageUrl) ?: return null
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) null
            else CachedImage(response.body.bytes(), response.header("Content-Type"))
        }
    }

    private fun resolveImageUrl(imageUrl: String): HttpUrl? {
        val url = "https://$IMAGE_HOST".toHttpUrl().resolve(imageUrl) ?: return null
        return if (url.host == IMAGE_HOST) url else null
    }
}
