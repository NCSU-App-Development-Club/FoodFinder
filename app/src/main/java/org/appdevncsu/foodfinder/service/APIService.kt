package org.appdevncsu.foodfinder.service

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.data.DiningMenuSection
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

interface APIService {

    @GET("api_locations")
    suspend fun getLocations(): List<DiningLocation>

    suspend fun getMenus(locationId: Int): List<DiningMenuListItem> {
        return getMenus("eq.$locationId")
    }

    @GET("api_location_menus")
    suspend fun getMenus(@Query("location_id") locationIdQuery: String): List<DiningMenuListItem>

    suspend fun getMenu(menuId: Int): List<DiningMenuSection> {
        return getMenu("eq.${menuId}")
    }

    @GET("menu_sections?&select=*,menu_items_with_names(*)")
    suspend fun getMenu(@Query("menu_id") menuIdQuery: String): List<DiningMenuSection>
}

@Module
@InstallIn(SingletonComponent::class)
internal object APIServiceModule {
    private val json = Json { ignoreUnknownKeys = true }

    private const val BASE_URL = "https://gepaaabqcrdbnxyunmuj.supabase.co/rest/v1/"
    private const val API_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdlcGFhYWJxY3JkYm54eXVubXVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwMDk5MzQsImV4cCI6MjA3NzU4NTkzNH0.gGnwah3aBmucqqm5if9IYbXvnmKz-8Hhq3hHCO38LvU"
    // ^ As far as I know, this key doesn't allow you to access or update anything secret.

    @Provides
    fun provideAPIService(): APIService {
        val client = OkHttpClient()
            .newBuilder()
            .addInterceptor { chain ->
                println("Sending request to ${chain.request().url}")
                chain.proceed(
                    chain.request().newBuilder().header("apikey", API_KEY).build()
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()
                )
            )
            .build()
            .create<APIService>()
    }
}
