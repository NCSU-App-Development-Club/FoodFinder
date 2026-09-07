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
import org.appdevncsu.foodfinder.shared.Database
import org.appdevncsu.foodfinder.shared.HoursResponse
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
                call.respond(mapOf("locations" to Database.getLocationSummaries()))
            }
            get("/locations/{slug}/image") {
                val slug = call.parameters["slug"]!!
                val imageUrl = Database.getLocationImageUrl(slug)
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
                    call.respond(mapOf("menus" to Database.getMenus(call.parameters["locationId"]!!.toInt())))
                }
                get("/{menuId}") {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                    call.respond(mapOf("sections" to Database.getMenu(call.parameters["menuId"]!!.toInt())))
                }
            }
            get("/hours") {
                call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                val dateParam = call.request.queryParameters["date"]
                val date = if (dateParam == null) {
                    LocalDate.now(NCSU_ZONE)
                } else {
                    try {
                        LocalDate.parse(dateParam)
                    } catch (e: DateTimeParseException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid date '$dateParam'; expected YYYY-MM-DD")
                        )
                        return@get
                    }
                }
                call.respond(
                    HoursResponse(
                        date = date.toString(),
                        locations = Database.getDiningSchedules(date)
                    )
                )
            }
        }
    }
}
