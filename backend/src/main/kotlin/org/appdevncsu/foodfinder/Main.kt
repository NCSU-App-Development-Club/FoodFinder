package org.appdevncsu.foodfinder

import org.appdevncsu.foodfinder.scraper.runScraper
import org.appdevncsu.foodfinder.server.runServer
import org.appdevncsu.foodfinder.server.runServerScheduled
import kotlin.system.exitProcess

private fun usage() {
    println(
        "Please specify one command-line argument:\n" +
            "\tscrape:          Run the scraper once and exit\n" +
            "\tserve:           Run the API server\n" +
            "\tserve-scheduled: Run the API server and scrape daily at 12am America/New_York",
    )
    exitProcess(1)
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        usage()
    }
    when (args[0]) {
        "scrape" -> runScraper()
        "serve" -> runServer()
        "serve-scheduled" -> runServerScheduled()
        else -> usage()
    }
}
