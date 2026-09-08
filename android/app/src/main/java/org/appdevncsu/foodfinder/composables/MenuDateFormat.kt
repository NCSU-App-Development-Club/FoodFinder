package org.appdevncsu.foodfinder.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import org.appdevncsu.foodfinder.R

private val MenuDateOutputFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

@Composable
fun formatMenuDate(dateString: String, today: LocalDate = LocalDate.now()): String {
    val date = try {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        return dateString
    }
    return when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.format(MenuDateOutputFormat)
    }
}
