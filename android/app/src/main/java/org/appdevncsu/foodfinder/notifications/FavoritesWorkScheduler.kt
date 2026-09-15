package org.appdevncsu.foodfinder.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.appdevncsu.foodfinder.data.ncsuZone
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the favorites fetching and notification sending
 * background work to happen daily at 6:30 AM Eastern time.
 */
@Singleton
class FavoritesWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Ensures a daily run is queued, without disturbing one that's already pending. */
    fun ensureDailyScheduled() {
        workManager.enqueueUniqueWork(DAILY_WORK_NAME, ExistingWorkPolicy.KEEP, dailyRequest())
    }

    /** Queues the next day's run, replacing the one that just ran. */
    fun scheduleNextDaily() {
        workManager.enqueueUniqueWork(DAILY_WORK_NAME, ExistingWorkPolicy.REPLACE, dailyRequest())
    }

    private fun dailyRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<FavoritesWorker>()
            .setInitialDelay(delayUntilNextNotification(), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .build()

    private fun delayUntilNextNotification(): Long {
        val now = ZonedDateTime.now(ncsuZone)
        var next = now.withHour(NotificationHour)
            .withMinute(NotificationMinute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    companion object {
        private const val DAILY_WORK_NAME = "favorites-daily"
        private const val NotificationHour = 6
        private const val NotificationMinute = 30
    }
}
