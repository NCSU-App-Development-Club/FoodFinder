package org.appdevncsu.foodfinder

import org.appdevncsu.foodfinder.scraper.runScraper
import org.appdevncsu.foodfinder.server.runServer
import kotlin.system.exitProcess

private fun usage() {
    println("Please specify one command-line argument:\n\tscrape: Run the scraper\n\tserve:  Run the API server")
    exitProcess(1)
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        usage()
    }
    when (args[0]) {
        "scrape" -> runScraper()
        "serve" -> runServer()
        else -> usage()
    }
}
