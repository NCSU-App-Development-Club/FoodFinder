package org.appdevncsu.foodfinder.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File

@Serializable
data class LocationList(
    val locations: List<Location>
)

@Serializable
data class Location(
    val name: String,
    val id: Int,
    val slug: String? = null,
    val type: String? = null,
    val imageUrl: String? = null
) {
    val absoluteImageUrl: String?
        get() = imageUrl?.let { "${APIClientModule.API_ORIGIN}$it" }
}

@Serializable
data class HoursList(
    val locations: List<LocationHours>
)

@Serializable
data class LocationHours(
    val slug: String,
    val name: String,
    val type: String,
    val days: List<DayHours>
)

@Serializable
data class DayHours(
    val date: String,
    val hours: List<HoursRange>
)

@Serializable
data class HoursRange(
    val status: String,
    val openMinute: Int? = null,
    val closeMinute: Int? = null,
    val rawText: String
)

@Serializable
data class MenuList(
    val menus: List<Menu>
)


@Serializable
data class Menu(
    val name: String,
    val id: Int,
    val date: String,
    val locationId: Int
)

@Serializable
data class SectionList(
    val sections: List<Section>
)

@Serializable
data class Section(
    val name: String,
    val id: Int,
    val items: List<Item>
)

@Serializable
data class Item(
    val name: String,
    val id: Int,
    val sectionId: Int,
    val flags: List<String>,
    val isNew: Boolean = false
) {
    val normalizedName: String
        get() = normalizeFavoriteName(name)
}

interface APIClient {
    @GET("locations")
    suspend fun listLocations(): LocationList

    @GET("locations/{locId}/menus")
    suspend fun listMenus(@Path("locId") locId: Int): MenuList

    @GET("locations/{locId}/menus/{menuId}")
    suspend fun listSection(@Path("locId") locID: Int, @Path("menuId") menuID: Int): SectionList

    @GET("locations/{locId}/item-history")
    suspend fun itemHistory(
        @Path("locId") locId: Int,
        @Query("name") name: String,
        @Query("date") date: String,
    ): ItemHistoryResponse

    @GET("hours")
    suspend fun listHours(@Query("days") days: Int): HoursList

    @GET("events")
    suspend fun listEvents(): EventList

    @HTTP(method = "QUERY", path = "favorites/menus", hasBody = true)
    suspend fun favoriteMenus(@Body body: FavoriteRequest): FavoritesResponse
}

@Serializable
data class FavoriteRequest(
    val items: List<String>,
    val days: Int = 1,
)

@Serializable
data class FavoritesResponse(
    val matches: List<FavoriteMatch>,
)

@Serializable
data class FavoriteMatch(
    val locationId: Int,
    val locationName: String,
    val menuId: Int,
    val menuName: String,
    val date: String,
    val items: List<String>,
)

@Serializable
data class EventList(
    val events: List<Event>
)

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val start: String, // ISO-8601 UTC instant, e.g. "2026-09-18T15:00:00Z"
    val end: String? = null,
    val allDay: Boolean = false,
)

@Serializable
data class ItemHistoryResponse(
    val history: ItemHistory,
)

@Serializable
data class ItemHistory(
    val locationId: Int,
    val name: String,
    val firstSeen: String? = null,
    val frequencyPerWeek: Double = 0.0,
    val dates: List<String> = emptyList(),
)

@Module
@InstallIn(SingletonComponent::class)
internal object APIClientModule {

    internal const val API_ORIGIN = "https://foodfinder-api.appdevncsu.org"
    private const val BASE_URL = "$API_ORIGIN/api/"
    private const val HTTP_CACHE_SIZE_BYTES = 50L * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http-cache"), HTTP_CACHE_SIZE_BYTES))
            .build()

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
