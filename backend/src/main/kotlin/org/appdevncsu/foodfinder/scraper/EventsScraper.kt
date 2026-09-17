package org.appdevncsu.foodfinder.scraper

import org.appdevncsu.foodfinder.shared.CampusEvent
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val log = LoggerFactory.getLogger("calendarEvents")

/**
 * Scrapes the NC State Dining Google Calendar for upcoming dining events.
 */
object EventsScraper {

    const val ICS_URL = "https://calendar.google.com/calendar/ical/" +
            "ncsu.edu_eogq8vgp8tjmgf30rbskkcf740@group.calendar.google.com/public/basic.ics" +
            "?futureevents=true"

    private const val CANCELLED = "CANCELLED"

    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val localDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val utcDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /** Fetches and parses the upcoming events, sorted by start time. */
    fun fetchEvents(): List<CampusEvent> {
        val ics = HttpClient.getCalendarICS(ICS_URL)
        val events = parse(ics)
        log.info("Found {} upcoming calendar events", events.size)
        return events
    }

    /**
     * Parses ICS text into events. Cancelled events and VEVENT blocks without a UID or start time are skipped.
     */
    fun parse(ics: String): List<CampusEvent> {
        val events = mutableListOf<CampusEvent>()
        var current: MutableMap<String, IcsProperty>? = null
        for (line in unfold(ics)) {
            when {
                line == "BEGIN:VEVENT" -> current = mutableMapOf()
                line == "END:VEVENT" -> {
                    current?.toEvent()?.let { events.add(it) }
                    current = null
                }

                current != null -> parseProperty(line)?.let { current[it.name] = it }
            }
        }
        return events
            .filter { it.status != CANCELLED }
            .sortedBy { it.start }
    }

    /** Rejoins RFC 5545 folded lines (continuations start with a space or tab). */
    private fun unfold(ics: String): List<String> {
        val lines = mutableListOf<String>()
        for (raw in ics.split("\r\n", "\n", "\r")) {
            val line = if (lines.isNotEmpty() && raw.isNotEmpty() && (raw[0] == ' ' || raw[0] == '\t')) {
                lines.removeAt(lines.size - 1) + raw.substring(1)
            } else {
                raw
            }
            if (line.isNotEmpty()) lines.add(line)
        }
        return lines
    }

    private fun parseProperty(line: String): IcsProperty? {
        val colon = line.indexOf(':')
        if (colon < 0) return null
        val segments = line.substring(0, colon).split(';')
        val params = segments.drop(1).mapNotNull { segment ->
            val equals = segment.indexOf('=')
            if (equals < 0) null else segment.substring(0, equals).uppercase() to segment.substring(equals + 1)
                .trim('"')
        }.toMap()
        return IcsProperty(
            name = segments.first().uppercase(),
            params = params,
            value = line.substring(colon + 1),
        )
    }

    private fun Map<String, IcsProperty>.toEvent(): CampusEvent? {
        val uid = this["UID"]?.value?.takeIf { it.isNotBlank() } ?: return null
        val startProperty = this["DTSTART"] ?: return null
        val start = startProperty.toInstant() ?: return null
        return CampusEvent(
            uid = uid,
            recurrenceId = this["RECURRENCE-ID"]?.value?.let(::unescape)?.takeIf { it.isNotBlank() },
            title = this["SUMMARY"]?.value?.let(::unescape)?.takeIf { it.isNotBlank() } ?: return null,
            description = this["DESCRIPTION"]?.value?.let(::unescape)?.takeIf { it.isNotBlank() },
            location = this["LOCATION"]?.value?.let(::unescape)?.takeIf { it.isNotBlank() },
            start = start,
            end = this["DTEND"]?.toInstant(),
            allDay = startProperty.params["VALUE"] == "DATE",
            status = this["STATUS"]?.value,
            recurrenceRule = this["RRULE"]?.value,
        )
    }

    private fun IcsProperty.toInstant(): Instant? {
        val raw = value.trim()
        return try {
            when {
                params["VALUE"] == "DATE" || raw.length == 8 ->
                    LocalDate.parse(raw, dateFormat).atStartOfDay(NCSU_ZONE).toInstant()

                raw.endsWith("Z") ->
                    LocalDateTime.parse(raw, utcDateTimeFormat).toInstant(ZoneOffset.UTC)

                else -> {
                    val zone = params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: NCSU_ZONE
                    LocalDateTime.parse(raw, localDateTimeFormat).atZone(zone).toInstant()
                }
            }
        } catch (e: DateTimeParseException) {
            log.warn("Skipping event with unparseable date '{}'", raw)
            null
        }
    }

    /** Unescapes ICS TEXT values: `\n`, `\,`, `\;`, and `\\`. */
    private fun unescape(value: String): String {
        val result = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (val escaped = value[i + 1]) {
                    'n', 'N' -> result.append('\n')
                    ',' -> result.append(',')
                    ';' -> result.append(';')
                    '\\' -> result.append('\\')
                    else -> result.append(escaped)
                }
                i += 2
            } else {
                result.append(c)
                i++
            }
        }
        return result.toString()
    }

    private data class IcsProperty(
        val name: String,
        val params: Map<String, String>,
        val value: String,
    )
}
