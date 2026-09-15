package org.appdevncsu.foodfinder.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.appdevncsu.foodfinder.data.local.FavoriteDao
import org.appdevncsu.foodfinder.data.local.FoodFinderDatabase
import org.appdevncsu.foodfinder.data.local.PayloadDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    private const val DATABASE_NAME = "foodfinder-cache.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FoodFinderDatabase =
        Room.databaseBuilder(context, FoodFinderDatabase::class.java, DATABASE_NAME)
            .addMigrations(FoodFinderDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePayloadDao(database: FoodFinderDatabase): PayloadDao = database.payloadDao()

    @Provides
    fun provideFavoriteDao(database: FoodFinderDatabase): FavoriteDao = database.favoriteDao()
}
