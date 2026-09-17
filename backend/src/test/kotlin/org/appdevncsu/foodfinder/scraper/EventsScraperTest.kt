package org.appdevncsu.foodfinder.scraper

import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EventsScraperTest {

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

    @Test
    fun `parses UTC, TZID, and all-day start times`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            event("utc@google.com", "Nigerian Independence Day", "DTSTART:20261001T203000Z", "DTEND:20261001T223000Z"),
            event(
                "tz@google.com",
                "Latin Heritage Month Meal",
                "DTSTART;TZID=America/New_York:20260917T103000",
                "DTEND;TZID=America/New_York:20260917T140000",
            ),
            event("day@google.com", "Meal Plans Active", "DTSTART;VALUE=DATE:20261027", "DTEND;VALUE=DATE:20261028"),
            "END:VCALENDAR",
        ).joinToString("\r\n")

        val events = EventsScraper.parse(ics).associateBy { it.uid }

        assertEquals(Instant.parse("2026-10-01T20:30:00Z"), events.getValue("utc@google.com").start)
        // 10:30 America/New_York on 2026-09-17 is 14:30 UTC (EDT).
        assertEquals(Instant.parse("2026-09-17T14:30:00Z"), events.getValue("tz@google.com").start)

        val allDay = events.getValue("day@google.com")
        assertTrue(allDay.allDay)
        assertEquals(
            LocalDate.of(2026, 10, 27).atStartOfDay(NCSU_ZONE).toInstant(),
            allDay.start,
        )
    }

    @Test
    fun `unescapes text values`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "UID:esc@google.com",
            "DTSTART;VALUE=DATE:20260918",
            "SUMMARY:Bug Bites\\, Fountain & Clark",
            "DESCRIPTION:First line\\nSecond line",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n")

        val events = EventsScraper.parse(ics)
        assertEquals("Bug Bites, Fountain & Clark", events.single().title)
        assertEquals("First line\nSecond line", events.single().description)
    }

    @Test
    fun `rejoins folded lines`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "UID:fold@google.com",
            "DTSTART;VALUE=DATE:20260911",
            "SUMMARY:Chef Spotlight Series: Roma",
            " nian Cabbage Rolls With Chef Tris",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n")

        val events = EventsScraper.parse(ics)
        assertEquals("Chef Spotlight Series: Romanian Cabbage Rolls With Chef Tris", events.single().title)
    }

    @Test
    fun `filters cancelled events and sorts by start`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            event("later@google.com", "Later", "DTSTART:20261002T180000Z", "DTEND:20261002T190000Z"),
            event("earlier@google.com", "Earlier", "DTSTART:20260918T150000Z", "DTEND:20260918T170000Z"),
            """
            BEGIN:VEVENT
            UID:cancelled@google.com
            DTSTART:20260920T150000Z
            STATUS:CANCELLED
            SUMMARY:Cancelled Event
            END:VEVENT
            """.trimIndent(),
            "END:VCALENDAR",
        ).joinToString("\r\n")

        val events = EventsScraper.parse(ics)
        assertEquals(listOf("Earlier", "Later"), events.map { it.title })
    }

    @Test
    fun `keeps recurrence overrides distinct from the series`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "UID:series@google.com",
            "DTSTART;TZID=America/New_York:20260901T120000",
            "RRULE:FREQ=WEEKLY;COUNT=4",
            "SUMMARY:Walking Club",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:series@google.com",
            "RECURRENCE-ID;TZID=America/New_York:20260915T120000",
            "DTSTART;TZID=America/New_York:20260915T130000",
            "SUMMARY:Walking Club (rescheduled)",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n")

        val events = EventsScraper.parse(ics)
        assertEquals(2, events.size)
        assertEquals("series@google.com", events[0].identity)
        assertEquals("series@google.com#20260915T120000", events[1].identity)
        assertNull(events[0].recurrenceId)
        assertEquals("20260915T120000", events[1].recurrenceId)
    }

    @Test
    fun `skips events without a uid or start`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "UID:nodate@google.com",
            "SUMMARY:No date",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n")

        assertTrue(EventsScraper.parse(ics).isEmpty())
    }
}
