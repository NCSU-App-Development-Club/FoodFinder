package org.appdevncsu.foodfinder.data

import android.content.res.Resources
import android.util.Log
import org.appdevncsu.foodfinder.R
import retrofit2.HttpException
import java.io.IOException

fun logApiError(tag: String, error: Throwable) {
    Log.e(tag, "API request failed", error)
}

fun userMessageFor(error: Throwable, resources: Resources): String {
    return when (error) {
        is IOException -> resources.getString(R.string.error_no_connection)
        is HttpException -> httpErrorMessage(error.code(), resources)
        else -> resources.getString(R.string.error_generic)
    }
}

private const val HttpRequestTimeout = 408
private const val HttpGatewayTimeout = 504
private const val HttpNotFound = 404
private const val HttpServerErrorMin = 500
private const val HttpServerErrorMax = 599

private fun httpErrorMessage(code: Int, resources: Resources): String {
    return when (code) {
        HttpRequestTimeout, HttpGatewayTimeout -> resources.getString(R.string.error_no_connection)
        in HttpServerErrorMin..HttpServerErrorMax -> resources.getString(R.string.error_server)
        HttpNotFound -> resources.getString(R.string.error_not_found)
        else -> resources.getString(R.string.error_generic)
    }
}
