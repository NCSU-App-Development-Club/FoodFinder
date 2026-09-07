package org.appdevncsu.foodfinder.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File

@Serializable data class LocationList (
    val locations : List<Location>
)

@Serializable data class Location (
    val name : String,
    val id : Int,
    val slug : String? = null,
    val type : String? = null,
    val imageUrl : String? = null
) {
    val absoluteImageUrl : String?
        get() = imageUrl?.let { "${APIClientModule.API_ORIGIN}$it" }
}

@Serializable data class HoursList (
    val date : String,
    val locations : List<LocationHours>
)

@Serializable data class LocationHours (
    val slug : String,
    val name : String,
    val type : String,
    val hours : List<HoursRange>
)

@Serializable data class HoursRange (
    val status : String,
    val openMinute : Int? = null,
    val closeMinute : Int? = null,
    val rawText : String
)

@Serializable data class MenuList (
    val menus : List<Menu>
)


@Serializable data class Menu (
    val name : String,
    val id : Int,
    val date : String,
    val locationId : Int
)

@Serializable data class SectionList (
    val sections : List<Section>
)

@Serializable data class Section (
    val name : String,
    val id : Int,
    val items : List<Item>
)

@Serializable data class Item (
    val name : String,
    val id : Int,
    val sectionId : Int,
    val flags : List<String>
)

interface APIClient {
    @GET("locations")
    suspend fun listLocations(): LocationList
    @GET("locations/{locId}/menus")
    suspend fun listMenus(@Path("locId") locId : Int) : MenuList
    @GET("locations/{locId}/menus/{menuId}")
    suspend fun listSection(@Path("locId") locID : Int, @Path("menuId") menuID : Int) : SectionList
    @GET("hours")
    suspend fun listHours(): HoursList
}

@Module
@InstallIn(SingletonComponent::class)
internal object APIClientModule {

    internal const val API_ORIGIN = "https://foodfinder-api.appdevncsu.org"
    private const val BASE_URL = "$API_ORIGIN/api/"
    private const val HTTP_CACHE_DIR = "http-cache"
    private const val HTTP_CACHE_SIZE_BYTES = 10L * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, HTTP_CACHE_DIR), HTTP_CACHE_SIZE_BYTES))
            .addInterceptor(OfflineFallbackInterceptor(context))
            .build()
    }

    @Provides
    fun provideAPIClient(okHttpClient: OkHttpClient): APIClient {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()
                )
            )
            .build()
            .create<APIClient>()
    }
}

private class OfflineFallbackInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isOnline()) return chain.proceed(request)
        val offlineRequest = request.newBuilder()
            .cacheControl(CacheControl.FORCE_CACHE)
            .build()
        return chain.proceed(offlineRequest)
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivityManager.activeNetwork
            ?.let(connectivityManager::getNetworkCapabilities)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
