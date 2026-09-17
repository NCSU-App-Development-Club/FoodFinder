package org.appdevncsu.foodfinder.data

import java.time.Instant
import java.time.LocalDate

/** The instant an event starts, or null if the timestamp cannot be parsed. */
fun Event.startInstant(): Instant? = runCatching { Instant.parse(start) }.getOrNull()

/** The instant an event ends, or null if it has no end or it cannot be parsed. */
fun Event.endInstant(): Instant? = end?.let { runCatching { Instant.parse(it) }.getOrNull() }

/** The campus-local date an event starts on, or null if the start cannot be parsed. */
fun Event.localStartDate(): LocalDate? = startInstant()?.atZone(ncsuZone)?.toLocalDate()
