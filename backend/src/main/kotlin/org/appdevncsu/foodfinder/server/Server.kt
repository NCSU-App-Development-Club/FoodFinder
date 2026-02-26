package org.appdevncsu.foodfinder.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
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
        get("/locations") {
            call.respond(Database.getLocations())
        }
        get("/locations/{locationId}/menus") {
            call.respond(Database.getMenus(call.parameters["locationId"]!!.toInt()))
        }
        get("/menus/{menuId}") {
            call.respond(Database.getMenu(call.parameters["menuId"]!!.toInt()))
        }
    }
}
