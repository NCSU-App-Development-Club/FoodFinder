package org.appdevncsu.foodfinder.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path

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

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    fun provideAPIService(): APIClient {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()
                )
            )
            .build()
            .create<APIClient>()
    }
}
