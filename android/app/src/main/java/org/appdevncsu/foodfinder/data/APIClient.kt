package org.appdevncsu.foodfinder.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

interface APIClient {
    
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
