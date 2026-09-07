package org.appdevncsu.foodfinder.data

import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoField

sealed interface LocationStatus {
    val rawText: String

    data class Open(override val rawText: String) : LocationStatus
    data class ClosingSoon(override val rawText: String) : LocationStatus
    data class Closed(override val rawText: String) : LocationStatus
    data class Unavailable(override val rawText: String) : LocationStatus
}

data class LocationListItem(
    val location: Location,
    // Null while the hours list is still loading; the UI shows a skeleton in its place.
    val status: LocationStatus?,
)

private const val ClosingSoonWindowMinutes = 30
private const val HoursUnavailableText = "Hours unavailable"
private val ncsuZone = ZoneId.of("America/New_York")

fun currentStatus(hours: List<HoursRange>?, now: LocalTime = LocalTime.now(ncsuZone)): LocationStatus {
    if (hours.isNullOrEmpty()) {
        return LocationStatus.Unavailable(HoursUnavailableText)
    }
    val nowMinute = now.get(ChronoField.MINUTE_OF_DAY)
    val activeCloseMinute = hours.firstNotNullOfOrNull { range ->
        val closeMinute = range.closeMinute
        val isActive = range.status == "open" && range.openMinute != null && closeMinute != null &&
            nowMinute in range.openMinute until closeMinute
        if (isActive) closeMinute else null
    }
    val unknown = hours.any { it.status == "unknown" }
    val hoursText = hours.joinToString(" | ") { it.rawText }.ifBlank { HoursUnavailableText }
    return when {
        activeCloseMinute != null -> {
            if (activeCloseMinute - nowMinute <= ClosingSoonWindowMinutes) {
                LocationStatus.ClosingSoon(hoursText)
            } else {
                LocationStatus.Open(hoursText)
            }
        }
        unknown -> LocationStatus.Unavailable(hoursText)
        else -> LocationStatus.Closed(hoursText)
    }
}
