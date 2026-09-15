package org.appdevncsu.foodfinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.appdevncsu.foodfinder.notifications.FavoritesWorker

/**
 * Debug-only hook to run the favorites worker on demand instead of waiting for 6:30 AM.
 *
 * Usage:
 * ```sh
 *   adb shell am broadcast -a org.appdevncsu.foodfinder.DEBUG_RUN_FAVORITES \
 *       -n org.appdevncsu.foodfinder/.FavoritesDebugReceiver
 *```
 *
 * This class only exists in debug builds.
 */
class FavoritesDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val request = OneTimeWorkRequestBuilder<FavoritesWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
