package org.appdevncsu.foodfinder

import org.appdevncsu.foodfinder.scraper.ScrapeTarget
import org.appdevncsu.foodfinder.scraper.runScraper
import org.appdevncsu.foodfinder.server.runServer
import org.appdevncsu.foodfinder.server.runServerScheduled
import kotlin.system.exitProcess

private fun usage(): Nothing {
    println(
        "Usage:\n" +
            "\tscrape [all|menus|hours|events]: Run a scrape once and exit (default: all)\n" +
            "\tserve:                          Run the API server\n" +
            "\tserve-scheduled:                Run the API server and scrape daily at 6am America/New_York",
    )
    exitProcess(1)
}

private fun scrapeTarget(name: String?): ScrapeTarget = when (name?.lowercase()) {
    null, "all" -> ScrapeTarget.ALL
    "menus" -> ScrapeTarget.MENUS
    "hours" -> ScrapeTarget.HOURS
    "events" -> ScrapeTarget.EVENTS
    else -> usage()
}

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "scrape" -> runScraper(scrapeTarget(args.getOrNull(1)))
        "serve" -> runServer()
        "serve-scheduled" -> runServerScheduled()
        else -> usage()
    }
}
