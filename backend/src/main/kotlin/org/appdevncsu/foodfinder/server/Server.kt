package org.appdevncsu.foodfinder.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.appdevncsu.foodfinder.scraper.ScrapeTarget
import org.appdevncsu.foodfinder.shared.Database
import org.appdevncsu.foodfinder.shared.DiningLocationSchedule
import org.appdevncsu.foodfinder.shared.HoursResponse
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.slf4j.LoggerFactory
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
                call.respond(mapOf("locations" to locations))
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
            route("/locations/{locationId}/menus") {
                get {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                    val locationId = call.parameters["locationId"]!!.toInt()
                    val menus = withContext(Dispatchers.IO) { Database.getMenus(locationId) }
                    call.respond(mapOf("menus" to menus))
                }
                get("/{menuId}") {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                    val menuId = call.parameters["menuId"]!!.toInt()
                    val sections = withContext(Dispatchers.IO) { Database.getMenu(menuId) }
                    call.respond(mapOf("sections" to sections))
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
                call.respond(HoursResponse(locations = locations))
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