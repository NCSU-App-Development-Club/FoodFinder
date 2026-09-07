package org.appdevncsu.foodfinder.composables

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val MenuDateOutputFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)

fun formatMenuDate(dateString: String, today: LocalDate = LocalDate.now()): String {
    val date = try {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        return dateString
    }
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(MenuDateOutputFormat)
    }
}
