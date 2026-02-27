package org.appdevncsu.foodfinder.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path


@Serializable data class Location (
    val name : String,
    val id : Int
)

@Serializable data class Menu (
    val name : String,
    val id : Int,
    val date : String,
    val locId : Int
)

@Serializable data class Section (
    val name : String,
    val id : Int,
    val items : List<Item>
)

@Serializable data class Item (
    val name : String,
    val id : Int,
    val secId : Int,
    val flags : List<String>
)

interface APIClient {
    @GET("locations")
    fun listLocations(): Call<List<Location>>
    @GET("{locId}/menus")
    fun listMenus(@Path("locId") locId : Int) : Call<List<Menu>>
    @GET("{locId}/menus{menuId}")
    fun listSection(@Path("locId") locID : Int, @Path("menuId") menuID : Int) : Call<List<Section>>
}

@Module
@InstallIn(SingletonComponent::class)
internal object APIClientModule {

    private const val BASE_URL = "https://foodfinder-api.appdevncsu.org/api/"

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
