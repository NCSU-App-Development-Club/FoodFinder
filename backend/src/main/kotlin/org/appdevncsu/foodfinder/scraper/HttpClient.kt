package org.appdevncsu.foodfinder.scraper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import java.net.CookieManager
import java.util.concurrent.Semaphore

const val baseURL = "https://netmenu2.cbord.com/NetNutrition/ncstate-dining"

private val client = OkHttpClient.Builder()
    .cookieJar(JavaNetCookieJar(CookieManager()))
    .build()

object HttpClient {

    private val limiter = Semaphore(4) // Limit the number of inflight requests to 4 at a time

    fun getHTMLContent(url: String): Document {
        val response: Response
        try {
            limiter.acquire()
            println("Fetching ${baseURL + url}")
            response = client.newCall(Request.Builder().url(baseURL + url).build()).execute()
        } finally {
            limiter.release()
        }
        if (response.code != 200) {
            error("Failed to fetch $url: $response")
        }
        return Ksoup.parse(response.body.string())
    }

    fun postWithFormData(path: String, requestBody: String): JsonObject =
        postWithFormData(path, requestBody, JsonObject::class.java)

    fun <T : Any> postWithFormData(path: String, requestBody: String, responseType: Class<T>): T {
        val body = requestBody.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
        val request = Request.Builder()
            .post(body)
            .url(baseURL + path)
            .build()
        val response: Response
        try {
            limiter.acquire()
            println("Fetching ${baseURL + path} with $requestBody")
            response = client.newCall(request).execute()
        } finally {
            limiter.release()
        }

        if (response.code != 200) {
            error("Invalid response code: $response")
        }

        return Gson().fromJson(response.body.string(), responseType)
    }
}