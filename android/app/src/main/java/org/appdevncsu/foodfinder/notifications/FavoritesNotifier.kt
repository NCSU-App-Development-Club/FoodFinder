package org.appdevncsu.foodfinder.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.appdevncsu.foodfinder.MainActivity
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.FavoriteMatch

/** Sends notifications when a favorite item appears on a dining menu for the day. */
object FavoritesNotifier {

    const val EXTRA_MENU_ID = "org.appdevncsu.foodfinder.extra.MENU_ID"
    const val EXTRA_MENU_NAME = "org.appdevncsu.foodfinder.extra.MENU_NAME"
    const val EXTRA_DATE = "org.appdevncsu.foodfinder.extra.DATE"
    const val EXTRA_LOCATION_ID = "org.appdevncsu.foodfinder.extra.LOCATION_ID"
    const val EXTRA_OPEN_FAVORITES = "org.appdevncsu.foodfinder.extra.OPEN_FAVORITES"

    private const val CHANNEL_ID = "favorites"
    private const val NOTIFICATION_ID = 1001

    @SuppressLint("MissingPermission")
    fun notifyMatches(context: Context, matches: List<FavoriteMatch>) {
        if (matches.isEmpty() || !canNotify(context)) return
        createChannel(context)

        val first = matches.first()
        val body = buildBody(context, matches, first)

        val intent = Intent(context, MainActivity::class.java).apply {
            if (matches.size > 1) {
                putExtra(EXTRA_OPEN_FAVORITES, true)
            } else {
                // There's just one menu, so link directly to it
                putExtra(EXTRA_MENU_ID, first.menuId)
                putExtra(EXTRA_MENU_NAME, first.menuName)
                putExtra(EXTRA_DATE, first.date)
                putExtra(EXTRA_LOCATION_ID, first.locationId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.favorite_24px)
            .setContentTitle(context.getString(R.string.favorites_notification_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun buildBody(
        context: Context,
        matches: List<FavoriteMatch>,
        first: FavoriteMatch,
    ): String {
        val base = context.getString(
            R.string.favorites_notification_body,
            first.items.joinToString(", "),
            first.locationName,
            first.menuName,
        )
        val extra = matches.size - 1
        return if (extra > 0) {
            context.getString(R.string.favorites_notification_more, base, extra)
        } else {
            base
        }
    }

    private fun canNotify(context: Context): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return enabled && permissionGranted
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.favorites_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.favorites_notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
