package org.appdevncsu.foodfinder.scraper

import biweekly.component.VEvent
import biweekly.io.TimezoneInfo
import biweekly.io.text.ICalReader
import biweekly.property.DateStart
import biweekly.property.Status
import biweekly.util.ICalDate
import org.appdevncsu.foodfinder.shared.CampusEvent
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.TimeZone

private val log = LoggerFactory.getLogger("calendarEvents")

/**
 * Scrapes the NC State Dining Google Calendar for upcoming dining events.
 */
object EventsScraper {

    const val ICS_URL = "https://calendar.google.com/calendar/ical/" +
            "ncsu.edu_eogq8vgp8tjmgf30rbskkcf740@group.calendar.google.com/public/basic.ics" +
            "?futureevents=true"

    private const val CANCELLED = Status.CANCELLED

    // Occurrences starting up to this far in the past are still stored (covers scrape gaps);
    // occurrences beyond the horizon are left for a later run.
    private val LOOKBACK: Duration = Duration.ofDays(1)
    private val HORIZON: Duration = Duration.ofDays(180)
    private val DEFAULT_TIMED_DURATION: Duration = Duration.ofHours(1)

    private val ncsuTimeZone: TimeZone = TimeZone.getTimeZone(NCSU_ZONE)
    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    /** Fetches and parses the upcoming events, sorted by start time. */
    fun fetchEvents(): List<CampusEvent> {
        val ics = HttpClient.getCalendarICS(ICS_URL)
        val events = parse(ics)
        log.info("Found {} upcoming calendar events", events.size)
        return events
    }

    /**
     * Parses ICS text into concrete event occurrences. Recurring series are expanded to
     * occurrences within the lookback/lookahead window. Cancelled events and VEVENT blocks
     * without a UID or start time are skipped.
     */
    fun parse(ics: String, now: Instant = Instant.now()): List<CampusEvent> {
        val calendar = ICalReader(ics).use { it.readNext() } ?: return emptyList()
        val timezoneInfo = calendar.timezoneInfo
        val components = calendar.events

        // A RECURRENCE-ID marks an override of a single occurrence in a series.
        val overrides = mutableMapOf<Pair<String, Long>, VEvent>()
        for (event in components) {
            val recurrenceId = event.recurrenceId?.value ?: continue
            val uid = event.uid?.value?.takeIf { it.isNotBlank() } ?: continue
            overrides[uid to recurrenceId.toStartInstant().toEpochMilli()] = event
        }

        val events = mutableListOf<CampusEvent>()
        for (event in components) {
            if (event.recurrenceId != null) continue // emitted from overrides below
            events += if (event.recurrenceRule != null) {
                event.expand(timezoneInfo, now, overrides)
            } else {
                listOfNotNull(event.toEvent())
            }
        }
        overrides.values.forEach { events += listOfNotNull(it.toEvent()) }

        return events
            .filter { it.status != CANCELLED }
            .sortedBy { it.start }
    }

    /** Expands a recurring event into occurrences within the lookback/lookahead window. */
    private fun VEvent.expand(
        timezoneInfo: TimezoneInfo,
        now: Instant,
        overrides: Map<Pair<String, Long>, VEvent>,
    ): List<CampusEvent> {
        val uid = uid?.value?.takeIf { it.isNotBlank() } ?: return emptyList()
        val title = summary?.value?.takeIf { it.isNotBlank() } ?: return emptyList()
        val start = dateStart?.value ?: return emptyList()
        val rule = recurrenceRule ?: return listOfNotNull(toEvent())
        val allDay = !start.hasTime()

        val originalStart = start.toStartInstant()
        val originalEnd = dateEnd?.value?.toStartInstant()
        val timedDuration = if (!allDay && originalEnd != null) Duration.between(originalStart, originalEnd) else null
        val allDaySpanDays = if (allDay && originalEnd != null) {
            ChronoUnit.DAYS.between(
                originalStart.atZone(NCSU_ZONE).toLocalDate(),
                originalEnd.atZone(NCSU_ZONE).toLocalDate(),
            )
        } else {
            null
        }

        val excluded = exceptionDates.flatMap { it.values }.mapTo(mutableSetOf()) { it.toStartInstant().toEpochMilli() }
        val windowStart = now.minus(LOOKBACK).toEpochMilli()
        val windowEnd = now.plus(HORIZON).toEpochMilli()

        val events = mutableListOf<CampusEvent>()
        val iterator = rule.getDateIterator(start, recurrenceTimeZone(dateStart, timezoneInfo))
        iterator.advanceTo(Date(windowStart))
        while (iterator.hasNext()) {
            val occurrenceMillis = iterator.next().time
            if (occurrenceMillis > windowEnd) break
            if (uid to occurrenceMillis in overrides) continue
            if (occurrenceMillis in excluded) continue
            val occurrenceStart = Instant.ofEpochMilli(occurrenceMillis)
            events += CampusEvent(
                uid = uid,
                title = title,
                description = description?.value?.takeIf { it.isNotBlank() },
                location = location?.value?.takeIf { it.isNotBlank() },
                start = occurrenceStart,
                end = occurrenceEnd(occurrenceStart, allDay, timedDuration, allDaySpanDays),
                allDay = allDay,
                status = status?.value,
            )
        }
        return events
    }

    private fun occurrenceEnd(
        start: Instant,
        allDay: Boolean,
        timedDuration: Duration?,
        allDaySpanDays: Long?,
    ): Instant {
        return if (allDay) {
            start.atZone(NCSU_ZONE).toLocalDate()
                .plusDays(allDaySpanDays ?: 1L)
                .atStartOfDay(NCSU_ZONE)
                .toInstant()
        } else {
            start.plus(timedDuration ?: DEFAULT_TIMED_DURATION)
        }
    }

    private fun VEvent.toEvent(): CampusEvent? {
        val uid = uid?.value?.takeIf { it.isNotBlank() } ?: return null
        val title = summary?.value?.takeIf { it.isNotBlank() } ?: return null
        val start = dateStart?.value ?: return null
        return CampusEvent(
            uid = uid,
            title = title,
            description = description?.value?.takeIf { it.isNotBlank() },
            location = location?.value?.takeIf { it.isNotBlank() },
            start = start.toStartInstant(),
            end = dateEnd?.value?.toStartInstant(),
            allDay = !start.hasTime(),
            status = status?.value,
        )
    }

    /**
     * The zone an event recurs in: its assigned `TZID`, UTC for `...Z` values, or Eastern for
     * all-day and floating values (this calendar is Eastern).
     */
    private fun recurrenceTimeZone(property: DateStart?, timezoneInfo: TimezoneInfo): TimeZone {
        if (property == null) return ncsuTimeZone
        val value = property.value
        if (value != null && !value.hasTime()) return ncsuTimeZone
        if (timezoneInfo.isFloating(property)) return ncsuTimeZone
        return timezoneInfo.getTimezone(property)?.timeZone ?: utcTimeZone
    }

    /** Resolves an iCalendar date to an instant, anchoring all-day values to Eastern midnight. */
    private fun ICalDate.toStartInstant(): Instant =
        if (hasTime()) {
            Instant.ofEpochMilli(time)
        } else {
            LocalDate.of(rawComponents.year, rawComponents.month, rawComponents.date)
                .atStartOfDay(NCSU_ZONE)
                .toInstant()
        }
}
