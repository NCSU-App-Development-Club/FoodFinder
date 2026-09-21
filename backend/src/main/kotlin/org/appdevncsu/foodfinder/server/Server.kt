package org.appdevncsu.foodfinder.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.appdevncsu.foodfinder.scraper.ScrapeTarget
import org.appdevncsu.foodfinder.shared.Database
import org.appdevncsu.foodfinder.shared.DiningLocationSchedule
import org.appdevncsu.foodfinder.shared.FavoriteRequest
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

private val log = LoggerFactory.getLogger("server")

fun main() {
    runServer()
}

fun runServer() {
    Server.start()
}

fun runServerScheduled() {
    startDailyScrapeScheduler()
    Server.start()
}

object Server {
    fun start() {
        Database.init()
        if (Database.isEmpty()) {
            log.info("Database is empty; running initial scrape")
            triggerScrape(ScrapeTarget.ALL)
        }
        startManualScrapeListener()
        embeddedServer(
            factory = CIO,
            port = 3000,
            host = "0.0.0.0",
            module = Application::module
        ).start(wait = true)
    }
}

fun Application.module() {
    configureRouting()
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)
    install(ConditionalHeaders)
    install(Compression) {
        gzip()
    }
    configureStaticFiles()
}

/** Serves the Astro website for every path that isn't an API route */
private fun Application.configureStaticFiles() {
    val staticDir = File(System.getenv("STATIC_DIR") ?: "static")
    if (!staticDir.isDirectory) {
        log.warn("Static web root {} does not exist; serving the API only", staticDir.absolutePath)
        return
    }
    log.info("Serving the website from {}", staticDir.absolutePath)
    routing {
        staticFiles("/", staticDir)
    }
}

private suspend fun ApplicationCall.respondWithEtag(body: Any) {
    response.header(HttpHeaders.ETag, etagFor(body))
    respond(body)
}

private fun etagFor(body: Any): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(body.toString().toByteArray(Charsets.UTF_8))
    return "\"" + digest.joinToString("") { "%02x".format(it) } + "\""
}

fun Application.configureRouting() {
    routing {
        // Liveness probe for Kamal's healthcheck (expects 200 on /up).
        get("/up") {
            call.respond(HttpStatusCode.OK)
        }
        route("/api") {
            get("/locations") {
                // Only changes when the scrapers learn something new, so clients can cache it.
                call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                val locations = withContext(Dispatchers.IO) { Database.getLocationSummaries() }
                call.respondWithEtag(mapOf("locations" to locations))
            }
            get("/locations/{slug}/image") {
                val slug = call.parameters["slug"]!!
                val imageUrl = withContext(Dispatchers.IO) { Database.getLocationImageUrl(slug) }
                if (imageUrl == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "No image for location '$slug'"))
                    return@get
                }
                val image = withContext(Dispatchers.IO) {
                    runCatching { Images.fetch(imageUrl) }.getOrNull()
                }
                if (image == null) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to "Failed to fetch image for '$slug'"))
                    return@get
                }
                call.response.header(HttpHeaders.CacheControl, "public, max-age=2592000")
                call.respondBytes(
                    image.bytes,
                    image.contentType?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                        ?: ContentType.Application.OctetStream
                )
            }
            get("/locations/{locationId}/item-history") {
                val locationId = call.parameters["locationId"]!!.toInt()
                val name = call.request.queryParameters["name"]
                if (name.isNullOrBlank() || name.length > MAX_ITEM_NAME_LENGTH) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Provide an item 'name' of at most $MAX_ITEM_NAME_LENGTH characters")
                    )
                    return@get
                }
                val dateParam = call.request.queryParameters["date"]
                val endDate = if (dateParam == null) {
                    // Without a menu date, end the window at the latest menu we know about for
                    // this location so that newly-seen items still count once.
                    withContext(Dispatchers.IO) { Database.getLatestMenuDate(locationId) }
                        ?: LocalDate.now(NCSU_ZONE)
                } else {
                    runCatching { LocalDate.parse(dateParam) }.getOrNull()
                }
                if (endDate == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'date'; expected yyyy-MM-dd"))
                    return@get
                }
                // History only changes when new menus are scraped, so clients may cache it for a day.
                call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                val history = withContext(Dispatchers.IO) {
                    Database.getMenuItemHistory(locationId, name, endDate)
                }
                if (history == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid item name"))
                    return@get
                }
                call.respondWithEtag(mapOf("history" to history))
            }
            route("/locations/{locationId}/menus") {
                get {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                    val locationId = call.parameters["locationId"]!!.toInt()
                    val menus = withContext(Dispatchers.IO) { Database.getMenus(locationId) }
                    call.respondWithEtag(mapOf("menus" to menus))
                }
                get("/{menuId}") {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                    val menuId = call.parameters["menuId"]!!.toInt()
                    val sections = withContext(Dispatchers.IO) { Database.getMenu(menuId) }
                    call.respondWithEtag(mapOf("sections" to sections))
                }
            }
            get("/hours") {
                val daysParam = call.request.queryParameters["days"]
                val days = daysParam?.toIntOrNull() ?: DEFAULT_HOURS_DAYS
                if (days !in 1..MAX_HOURS_DAYS) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid days '$daysParam'; expected 1..$MAX_HOURS_DAYS")
                    )
                    return@get
                }
                val locations = withContext(Dispatchers.IO) {
                    Database.getDiningSchedules(LocalDate.now(NCSU_ZONE), days)
                }
                // Don't let clients cache an incomplete payload; retry soon instead.
                val cacheControl = if (mostlyMissingHours(locations)) "no-store" else "public, max-age=3600"
                call.response.header(HttpHeaders.CacheControl, cacheControl)
                call.respondWithEtag(mapOf("locations" to locations))
            }
            get("/events") {
                // Events only change on the daily scrape, so clients may cache for a day.
                call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                val events = withContext(Dispatchers.IO) { Database.getUpcomingEvents(MAX_EVENTS) }
                call.respondWithEtag(mapOf("events" to events))
            }
            route("/favorites/menus") {
                method(HttpMethod.Query) {
                    handle {
                        val request = call.receive<FavoriteRequest>()
                        if (request.items.isEmpty()) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Provide at least one favorite item")
                            )
                            return@handle
                        }
                        if (request.items.size > MAX_FAVORITE_ITEMS) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "At most $MAX_FAVORITE_ITEMS favorite items are supported")
                            )
                            return@handle
                        }
                        val days = request.days.coerceIn(1, MAX_FAVORITE_DAYS)
                        val matches = withContext(Dispatchers.IO) {
                            Database.getMenusContainingItems(request.items, LocalDate.now(NCSU_ZONE), days)
                        }
                        call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                        call.respondWithEtag(mapOf("matches" to matches))
                    }
                }
            }
        }
    }
}

// A location has no hours information when it has no ranges or every range is "unknown".
private fun mostlyMissingHours(locations: List<DiningLocationSchedule>): Boolean {
    if (locations.isEmpty()) return true
    val missing = locations.count { schedule ->
        schedule.days.flatMap { it.hours }.all { it.status == "unknown" }
    }
    return missing * 2 > locations.size
}

private const val DEFAULT_HOURS_DAYS = 3
private const val MAX_HOURS_DAYS = 7
private const val MAX_EVENTS = 20
private const val MAX_FAVORITE_ITEMS = 200
private const val MAX_FAVORITE_DAYS = 14
private const val MAX_ITEM_NAME_LENGTH = 128