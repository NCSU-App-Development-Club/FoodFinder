package org.appdevncsu.foodfinder.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import org.appdevncsu.foodfinder.R
import kotlin.math.roundToInt

/**
 * Formats a weekly frequency. At least once per week is shown as "N / week"; less than that is
 * shown as the inverted ratio, e.g. "one in every 5 weeks" for 0.2 per week.
 */
@Composable
fun formatItemFrequency(frequencyPerWeek: Double): String = when {
    frequencyPerWeek >= 1.0 -> stringResource(
        R.string.item_history_frequency_value,
        String.format(LocalLocale.current.platformLocale, "%.1f", frequencyPerWeek),
    )

    frequencyPerWeek <= 0.0 -> stringResource(R.string.item_history_frequency_unknown)

    else -> {
        val weeks = (1.0 / frequencyPerWeek).roundToInt()
        if (weeks <= 1) {
            stringResource(R.string.item_history_frequency_weekly)
        } else {
            stringResource(R.string.item_history_frequency_every_weeks, weeks)
        }
    }
}
