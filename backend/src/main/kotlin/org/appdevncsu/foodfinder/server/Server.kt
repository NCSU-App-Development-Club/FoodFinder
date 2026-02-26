package org.appdevncsu.foodfinder.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.appdevncsu.foodfinder.shared.Database

fun main() {
    runServer()
}

fun runServer() {
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
}

fun Application.configureRouting() {
    routing {
        route("/api") {
            get("/locations") {
                call.respond(mapOf("locations" to Database.getLocations()))
            }
            route("/locations/{locationId}/menus") {
                get {
                    call.respond(mapOf("menus" to Database.getMenus(call.parameters["locationId"]!!.toInt())))
                }
                get("/{menuId}") {
                    call.respond(mapOf("sections" to Database.getMenu(call.parameters["menuId"]!!.toInt())))
                }
            }
        }
    }
}
