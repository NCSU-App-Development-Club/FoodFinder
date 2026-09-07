package org.appdevncsu.foodfinder.scraper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import java.net.CookieManager
import java.util.concurrent.Semaphore

const val nnBaseURL = "https://netmenu2.cbord.com/NetNutrition/ncstate-dining"
const val diningBaseURL = "https://dining.ncsu.edu"

private val client = OkHttpClient.Builder()
    .cookieJar(JavaNetCookieJar(CookieManager()))
    .build()

object HttpClient {

    private val nnLimiter = Semaphore(4) // Limit the number of inflight requests to 4 at a time for NetNutrition
    private val diningLimiter = Semaphore(2) // and 2 at a time for dining.ncsu.edu

    fun getNetNutritionHTML(url: String): Document = getHTMLContent(nnBaseURL + url, nnLimiter)
    fun getDiningHTML(url: String): Document = getHTMLContent(diningBaseURL + url, diningLimiter)

    private fun getHTMLContent(url: String, semaphore: Semaphore): Document {
        val response: Response
        try {
            semaphore.acquire()
            println("Fetching $url")
            response = client.newCall(Request.Builder().url(url).build()).execute()
        } finally {
            semaphore.release()
        }
        if (response.code != 200) {
            error("Failed to fetch $url: $response")
        }
        return Ksoup.parse(response.body.string())
    }

    fun postWithFormData(path: String, requestBody: String): JsonObject {
        val body = requestBody.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
        val request = Request.Builder()
            .post(body)
            .url(nnBaseURL + path)
            .build()
        val response: Response
        try {
            nnLimiter.acquire()
            println("Fetching ${nnBaseURL + path} with $requestBody")
            response = client.newCall(request).execute()
        } finally {
            nnLimiter.release()
        }

        if (response.code != 200) {
            error("Invalid response code: $response")
        }

        val root = Json.decodeFromString<JsonObject>(response.body.string())
        if (root["success"]?.jsonPrimitive?.boolean != true) {
            error("success=false")
        }
        return root
    }
}