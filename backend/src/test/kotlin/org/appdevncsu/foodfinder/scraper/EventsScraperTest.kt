package org.appdevncsu.foodfinder.scraper

import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EventsScraperTest {

    private val now = Instant.parse("2026-09-16T00:00:00Z")

    private fun event(uid: String, summary: String, dtStart: String, dtEnd: String): String =
        """
        BEGIN:VEVENT
        UID:$uid
        DTSTAMP:20260916T184300Z
        $dtStart
        $dtEnd
        STATUS:CONFIRMED
        SUMMARY:$summary
        END:VEVENT
        """.trimIndent()

    private fun ics(vararg lines: String): String =
        (listOf("BEGIN:VCALENDAR") + lines + "END:VCALENDAR").joinToString("\r\n")

    @Test
    fun `parses UTC, TZID, and all-day start times`() {
        val calendar = ics(
            event("utc@google.com", "Nigerian Independence Day", "DTSTART:20261001T203000Z", "DTEND:20261001T223000Z"),
            event(
                "tz@google.com",
                "Latin Heritage Month Meal",
                "DTSTART;TZID=America/New_York:20260917T103000",
                "DTEND;TZID=America/New_York:20260917T140000",
            ),
            event("day@google.com", "Meal Plans Active", "DTSTART;VALUE=DATE:20261027", "DTEND;VALUE=DATE:20261028"),
        )

        val events = EventsScraper.parse(calendar).associateBy { it.uid }

        assertEquals(Instant.parse("2026-10-01T20:30:00Z"), events.getValue("utc@google.com").start)
        // 10:30 America/New_York on 2026-09-17 is 14:30 UTC (EDT).
        assertEquals(Instant.parse("2026-09-17T14:30:00Z"), events.getValue("tz@google.com").start)

        val allDay = events.getValue("day@google.com")
        assertTrue(allDay.allDay)
        assertEquals(LocalDate.of(2026, 10, 27).atStartOfDay(NCSU_ZONE).toInstant(), allDay.start)
    }

    @Test
    fun `unescapes text values`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:esc@google.com",
            "DTSTART;VALUE=DATE:20260918",
            "SUMMARY:Bug Bites\\, Fountain & Clark",
            "DESCRIPTION:First line\\nSecond line",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar)
        assertEquals("Bug Bites, Fountain & Clark", events.single().title)
        assertEquals("First line\nSecond line", events.single().description)
    }

    @Test
    fun `rejoins folded lines`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:fold@google.com",
            "DTSTART;VALUE=DATE:20260911",
            "SUMMARY:Chef Spotlight Series: Roma",
            " nian Cabbage Rolls With Chef Tris",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar)
        assertEquals("Chef Spotlight Series: Romanian Cabbage Rolls With Chef Tris", events.single().title)
    }

    @Test
    fun `filters cancelled events and sorts by start`() {
        val calendar = ics(
            event("later@google.com", "Later", "DTSTART:20261002T180000Z", "DTEND:20261002T190000Z"),
            event("earlier@google.com", "Earlier", "DTSTART:20260918T150000Z", "DTEND:20260918T170000Z"),
            "BEGIN:VEVENT",
            "UID:cancelled@google.com",
            "DTSTART:20260920T150000Z",
            "STATUS:CANCELLED",
            "SUMMARY:Cancelled Event",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar)
        assertEquals(listOf("Earlier", "Later"), events.map { it.title })
    }

    @Test
    fun `expands a weekly recurrence within the window`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:weekly@google.com",
            "DTSTART;TZID=America/New_York:20260917T103000",
            "DTEND;TZID=America/New_York:20260917T113000",
            "RRULE:FREQ=WEEKLY;WKST=SU;UNTIL=20261002T035959Z;BYDAY=TH",
            "SUMMARY:Latin Heritage Month Meal",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar, now)

        assertEquals(
            listOf(
                Instant.parse("2026-09-17T14:30:00Z"),
                Instant.parse("2026-09-24T14:30:00Z"),
                Instant.parse("2026-10-01T14:30:00Z"),
            ),
            events.map { it.start },
        )
    }

    @Test
    fun `excludes EXDATE occurrences`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:exdate@google.com",
            "DTSTART;TZID=America/New_York:20260917T103000",
            "RRULE:FREQ=WEEKLY;COUNT=3;BYDAY=TH",
            "EXDATE;TZID=America/New_York:20260924T103000",
            "SUMMARY:Weekly",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar, now)

        assertEquals(
            listOf(Instant.parse("2026-09-17T14:30:00Z"), Instant.parse("2026-10-01T14:30:00Z")),
            events.map { it.start },
        )
    }

    @Test
    fun `replaces an overridden occurrence`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:series@google.com",
            "DTSTART;TZID=America/New_York:20260917T103000",
            "RRULE:FREQ=WEEKLY;COUNT=3;BYDAY=TH",
            "SUMMARY:Walking Club",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:series@google.com",
            "RECURRENCE-ID;TZID=America/New_York:20260924T103000",
            "DTSTART;TZID=America/New_York:20260924T120000",
            "SUMMARY:Walking Club (rescheduled)",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar, now)

        assertEquals(
            listOf(
                Instant.parse("2026-09-17T14:30:00Z"),
                Instant.parse("2026-09-24T16:00:00Z"), // rescheduled to 12:00 EDT
                Instant.parse("2026-10-01T14:30:00Z"),
            ),
            events.map { it.start },
        )
        assertEquals(listOf("Walking Club", "Walking Club (rescheduled)", "Walking Club"), events.map { it.title })
    }

    @Test
    fun `expands a yearly all-day recurrence to the next occurrence`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:yearly@google.com",
            "DTSTART;VALUE=DATE:20230125",
            "DTEND;VALUE=DATE:20230126",
            "RRULE:FREQ=YEARLY",
            "SUMMARY:Last Day to Change Spring Meal Plan",
            "END:VEVENT",
        )

        val events = EventsScraper.parse(calendar, now)

        assertEquals(1, events.size)
        assertTrue(events.single().allDay)
        assertEquals(LocalDate.of(2027, 1, 25).atStartOfDay(NCSU_ZONE).toInstant(), events.single().start)
        assertEquals(LocalDate.of(2027, 1, 26).atStartOfDay(NCSU_ZONE).toInstant(), events.single().end)
    }

    @Test
    fun `skips events without a uid or start`() {
        val calendar = ics(
            "BEGIN:VEVENT",
            "UID:nodate@google.com",
            "SUMMARY:No date",
            "END:VEVENT",
        )

        assertTrue(EventsScraper.parse(calendar).isEmpty())
    }
}
