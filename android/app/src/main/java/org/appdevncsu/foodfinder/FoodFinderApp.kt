package org.appdevncsu.foodfinder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import javax.inject.Inject

@HiltAndroidApp
class FoodFinderApp : Application() {
    @Inject lateinit var repository: ContentRepository

    override fun onCreate() {
        super.onCreate()
        repository.prefetchHome()
    }
}
