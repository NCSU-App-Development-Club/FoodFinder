package org.appdevncsu.foodfinder.data

import android.util.Log
import retrofit2.HttpException
import java.io.IOException

fun logApiError(tag: String, error: Throwable) {
    Log.e(tag, "API request failed", error)
}

fun userMessageFor(error: Throwable): String {
    return when (error) {
        is IOException -> "No internet connection. Check your connection and try again."
        is HttpException -> httpErrorMessage(error.code())
        else -> "Something went wrong. Please try again."
    }
}

private const val HttpRequestTimeout = 408
private const val HttpGatewayTimeout = 504
private const val HttpNotFound = 404
private const val HttpServerErrorMin = 500
private const val HttpServerErrorMax = 599

private fun httpErrorMessage(code: Int): String {
    return when (code) {
        HttpRequestTimeout, HttpGatewayTimeout -> "No internet connection. Check your connection and try again."
        in HttpServerErrorMin..HttpServerErrorMax -> "Server error. Please try again later."
        HttpNotFound -> "Requested content was not found."
        else -> "Something went wrong. Please try again."
    }
}
