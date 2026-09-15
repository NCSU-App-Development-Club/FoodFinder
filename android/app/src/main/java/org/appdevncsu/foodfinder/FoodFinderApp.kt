package org.appdevncsu.foodfinder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import org.appdevncsu.foodfinder.notifications.FavoritesWorkScheduler
import javax.inject.Inject

@HiltAndroidApp
class FoodFinderApp : Application(), Configuration.Provider {
    @Inject lateinit var repository: ContentRepository
    @Inject lateinit var favoritesWorkScheduler: FavoritesWorkScheduler
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        repository.prefetchHome()
        favoritesWorkScheduler.ensureDailyScheduled()
    }
}
