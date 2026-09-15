package org.appdevncsu.foodfinder.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.repository.FavoritesRepository

/** Fetches the menus containing favorites and notifies the user where they are today. */
@HiltWorker
class FavoritesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val favoritesRepository: FavoritesRepository,
    private val scheduler: FavoritesWorkScheduler,
) : CoroutineWorker(appContext, params) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        return try {
            // Always fetch fresh data before deciding whether to notify.
            val response = favoritesRepository.refreshMatches(force = true)
            if (response != null && response.matches.isNotEmpty()) {
                FavoritesNotifier.notifyMatches(applicationContext, response.matches)
            }
            // Queue the next run for 6:30 AM tomorrow.
            scheduler.scheduleNextDaily()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logApiError(TAG, e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "FavoritesWorker"
    }
}
