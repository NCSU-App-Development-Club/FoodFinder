package org.appdevncsu.foodfinder.scraper

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import org.appdevncsu.foodfinder.shared.DiningLocation
import org.appdevncsu.foodfinder.shared.DiningLocationHours
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import java.time.LocalDate

/**
 * Scrapes location tiles and daily open hours from dining.ncsu.edu.
 */
object DiningHoursScraper {

    /**
     * Maps dining.ncsu.edu slugs to NetNutrition unit IDs (menuLocations.id)
     */
    val SLUG_TO_UNIT_ID = mapOf(
        "fountain" to 1,
        "clark" to 2,
        "case" to 3,
        "university-towers" to 4,
        "one-earth" to 5,
        "on-the-oval" to 6,
        "brickyard-pizza-and-pasta" to 7,
        "smoothie-u" to 8,
        "tuffys-diner" to 9,
        "los-lobos" to 10,
        "jasons-deli" to 11,
        "starbucks" to 13,
        "hill-of-beans-cafe" to 14,
        "common-grounds-cafe" to 15,
        "creature-comforts" to 16,
        "pcj-eb-ii" to 17,
        "pcj-talley" to 18,
        "elements-cafe" to 19,
        "social-fabric" to 20,
        "pcj-nelson-hall" to 21,
        "la-farm" to 22,
        "wolves-den" to 23,
        "terrace-dining-room" to 24,
    )

    private val locationTypes = listOf("dining-halls", "restaurants", "food-courts", "cafes", "markets")

    private const val resultsPath =
        "/wp-admin/admin-ajax.php?action=ncdining_ajax_location_results" +
                "&campus%5B%5D=east&campus%5B%5D=central&campus%5B%5D=west" +
                "&campus%5B%5D=centennial&campus%5B%5D=biomedical" +
                "&time=08%3A00&closed_hidden=1"

    private val slugRegex = Regex("/location/([a-z0-9-]+)/")
    private val imageUrlRegex = Regex("url\\(([^)]+)\\)")
    private val closedRegex = Regex("^Closed all day(?:\\s+-\\s+(.+))?$", RegexOption.IGNORE_CASE)
    private val timeRangeRegex =
        Regex(
            "^(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?m\\.?\\s*-\\s*(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?m\\.?$",
            RegexOption.IGNORE_CASE
        )

    private data class Tile(
        val slug: String,
        val name: String,
        val type: String,
        val imageUrl: String?,
        val ranges: List<String>
    )

    data class ScrapeResult(
        val locations: List<DiningLocation>,
        val hours: List<DiningLocationHours>,
    )

    /** Fetches the next [days] days (including today) of hours data and stores it. */
    fun scrapeAndStore(days: Int = 3): ScrapeResult {
        val today = LocalDate.now(NCSU_ZONE)
        val result = fetchAll((0 until days).map { today.plusDays(it.toLong()) })

        println(
            "Found ${result.locations.size} dining locations and " +
                    "${result.hours.size} hours rows for the next $days day(s)."
        )
        return result
    }

    /**
     * Fetches hours for every date. Failed requests are retried once.
     */
    private fun fetchAll(dates: List<LocalDate>): ScrapeResult {
        val tilesBySlug = mutableMapOf<String, Tile>() // slug -> tile (first sighting wins)
        val hours = mutableListOf<DiningLocationHours>()
        val failed = mutableListOf<String>()

        for (date in dates) {
            for (type in locationTypes) {
                val fetched = fetchType(type, date)
                if (fetched == null) {
                    failed.add("$type on $date")
                } else {
                    for (tile in fetched) {
                        tilesBySlug.putIfAbsent(tile.slug, tile)
                        hours.addAll(parseHours(tile.slug, date, tile.ranges))
                    }
                }
            }
        }

        val unmapped = tilesBySlug.keys.filter { it !in SLUG_TO_UNIT_ID }
        val mappedButUnseen = SLUG_TO_UNIT_ID.keys.filter { it !in tilesBySlug.keys }
        if (unmapped.isNotEmpty()) {
            println("Warning: dining locations with no NetNutrition mapping: $unmapped")
        }
        if (mappedButUnseen.isNotEmpty()) {
            println("Warning: mapped locations missing from dining site: $mappedButUnseen")
        }
        if (failed.isNotEmpty()) {
            println("Warning: failed to fetch (after retry): $failed")
        }

        val locations = tilesBySlug.values.map { tile ->
            DiningLocation(
                slug = tile.slug,
                name = tile.name,
                type = tile.type,
                imageUrl = tile.imageUrl,
                unitId = SLUG_TO_UNIT_ID[tile.slug]
            )
        }.sortedBy { it.slug }

        return ScrapeResult(locations = locations, hours = hours)
    }

    /** Returns the parsed tiles for one (type, date), or null after one retry. */
    private fun fetchType(type: String, date: LocalDate): List<Tile>? {
        val url = "$resultsPath&type=$type&date=$date"
        for (attempt in 1..2) {
            try {
                return parseLocationTiles(HttpClient.getDiningHTML(url), type)
            } catch (e: Exception) {
                println("Attempt $attempt to fetch $type on $date failed: $e")
            }
        }
        return null
    }

    private fun parseLocationTiles(doc: Document, type: String): List<Tile> {
        return doc.select("a.location-tile").mapNotNull { parseTile(it, type) }
    }

    private fun parseTile(tile: Element, type: String): Tile? {
        val slug = slugRegex.find(tile.attr("href"))?.groupValues?.get(1)
        if (slug == null) {
            println("Warning: could not extract slug from tile: ${tile.attr("href")}")
            return null
        }

        val name = tile.selectFirst(".location-tile__bar h4")?.text() ?: slug

        val ranges = tile.select(".location-tile__date .range")
            .map { it.text().replace('\u00A0', ' ').trim() }
            .filter { it.isNotEmpty() }

        val style = tile.selectFirst(".location-tile-inner")?.attr("style") ?: ""
        val imageUrl = imageUrlRegex.find(style)?.groupValues?.get(1)

        return Tile(slug = slug, name = name, type = type, imageUrl = imageUrl, ranges = ranges)
    }

    /**
     * Converts one day's range spans into stored rows:
     *  - all spans parse as time ranges -> one "open" row per range (sorted by open time)
     *  - all spans are "Closed all day[- reason]" -> a single "closed" row
     *  - anything else (mixed, empty, unparseable formats) -> a single "unknown" row
     *    whose rawText preserves the site's text for display as a fallback.
     */
    private fun parseHours(slug: String, date: LocalDate, ranges: List<String>): List<DiningLocationHours> {
        if (ranges.isEmpty()) {
            return listOf(DiningLocationHours(slug, date, 0, "unknown", null, null, ""))
        }

        val closedMatches = ranges.map { closedRegex.matchEntire(it) }
        if (closedMatches.all { it != null }) {
            return listOf(DiningLocationHours(slug, date, 0, "closed", null, null, ranges.joinToString(" | ")))
        }

        val parsedRanges = ranges.mapNotNull { text -> parseTimeRange(text)?.let { text to it } }
        if (parsedRanges.size != ranges.size) {
            return listOf(DiningLocationHours(slug, date, 0, "unknown", null, null, ranges.joinToString(" | ")))
        }

        return parsedRanges
            .sortedBy { it.second.first }
            .mapIndexed { seq, (text, times) ->
                DiningLocationHours(slug, date, seq, "open", times.first, times.second, text)
            }
    }

    /** Parses "7:00am - 9:00pm" into minutes after midnight; close may exceed 1440. */
    private fun parseTimeRange(text: String): Pair<Int, Int>? {
        val match = timeRangeRegex.matchEntire(text) ?: return null
        val open = toMinute(match.groupValues[1].toInt(), match.groupValues[2], match.groupValues[3])
        val close = toMinute(match.groupValues[4].toInt(), match.groupValues[5], match.groupValues[6])
        // A closing time at or before the opening time means the range crosses
        // midnight (e.g. 7:00am - 12:00am closes at 24:00).
        val adjustedClose = if (close <= open) close + 24 * 60 else close
        return open to adjustedClose
    }

    private fun toMinute(hour12: Int, minute: String, amPm: String): Int {
        val hour = (hour12 % 12) + if (amPm.equals("p", ignoreCase = true)) 12 else 0
        return hour * 60 + (minute.toIntOrNull() ?: 0)
    }
}
