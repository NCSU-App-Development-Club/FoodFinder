package org.appdevncsu.foodfinder.shared

import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.server.LocalDateSerializer
import java.time.LocalDate
import java.time.ZoneId

// NC State Dining operates in Eastern Time; used to resolve "today" for schedules.
val NCSU_ZONE: ZoneId = ZoneId.of("America/New_York")

@Serializable
data class MenuLocation(
    val id: Int,
    val name: String, // e.g. Fountain Dining Hall
)

@Serializable
data class Menu(
    val id: Int,
    val locationId: Int, // -> Location
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val name: String, // e.g. "Breakfast"
)

@Serializable
data class MenuSection(
    val id: Int,
    val name: String, // e.g. "Display Station" or "Home Style Entrée"
    val items: List<MenuItem>
)

@Serializable
data class MenuItem(
    val id: Int,
    val sectionId: Int, // -> MenuSection
    val name: String, // e.g. Freshly Scrambled Eggs
    val flags: List<String> // e.g. Halal, Vegan, Wolf Approved
)

/** A location from dining.ncsu.edu */
@Serializable
data class DiningLocation(
    val slug: String, // e.g. "fountain" (stable URL slug from dining.ncsu.edu)
    val name: String, // e.g. "Fountain"
    val type: String, // e.g. "dining-halls", "restaurants", "food-courts", "cafes", "markets"
    val imageUrl: String?, // tile background image from the website
    val unitId: Int? // -> menuLocations.id; null if the location has no menus
)

/** One range of hours for a location on a particular date. */
data class DiningLocationHours(
    val slug: String, // -> diningLocations.slug
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val seq: Int, // 0..n, ordering of ranges within the day
    val status: String, // "open" | "closed" | "unknown"
    val openMinute: Int?, // minutes after midnight; only for "open" rows
    val closeMinute: Int?, // may exceed 1440 for past-midnight closes (e.g. 12:00am -> 1440)
    val rawText: String, // verbatim text from the site, e.g. "Closed all day - Labor Day"
)

/** API response for /api/locations */
@Serializable
data class LocationSummary(
    // From menuLocations table:
    val id: Int, // NetNutrition unit id (-> menuLocations.id)
    val name: String,
    // From diningLocations table:
    val slug: String?,
    val type: String?,
    val imageUrl: String?,
)

@Serializable
data class HoursRange(
    val status: String, // "open" | "closed" | "unknown"
    val openMinute: Int?,
    val closeMinute: Int?,
    val rawText: String, // e.g. "7:00am - 9:00pm" or "Closed all day - Labor Day"
)

@Serializable
data class DiningLocationSchedule(
    val slug: String,
    val name: String,
    val type: String,
    val hours: List<HoursRange>
)

/** API response for /api/hours */
@Serializable
data class HoursResponse(
    val date: String, // ISO date, e.g. "2026-09-07"
    val locations: List<DiningLocationSchedule>
)