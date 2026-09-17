package org.appdevncsu.foodfinder.server

import org.appdevncsu.foodfinder.scraper.ScrapeTarget
import org.appdevncsu.foodfinder.scraper.runScraper
import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.util.concurrent.Executors

private val log = LoggerFactory.getLogger("manualScrape")

private val scrapeExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "manual-scrape").apply { isDaemon = true }
}

/**
 * Requests a manual scrape. Work is queued on a single background thread, so
 * requests run one at a time and never block the server.
 */
fun triggerScrape(target: ScrapeTarget) {
    log.info("Queued manual {} scrape", target)
    scrapeExecutor.submit {
        runCatching { runScraper(target) }
            .onFailure { log.error("Manual {} scrape failed", target, it) }
    }
}

/**
 * Triggers manual scrapes via Unix signals:
 *
 *   SIGHUP  all (menus + hours + events)
 *   SIGUSR1 menus
 *   SIGUSR2 hours
 *
 * For example: `docker kill -s USR1 <container>`.
 */
fun startManualScrapeListener() {
    startSignalListener("HUP", ScrapeTarget.ALL)
    startSignalListener("USR1", ScrapeTarget.MENUS)
    startSignalListener("USR2", ScrapeTarget.HOURS)
}

private fun startSignalListener(name: String, target: ScrapeTarget) {
    try {
        Signal.handle(Signal(name)) {
            log.info("Received SIG{}; requesting {} scrape", name, target)
            triggerScrape(target)
        }
    } catch (e: IllegalArgumentException) {
        log.warn("SIG{} is unavailable on this platform; {} scrape via signal is disabled", name, target)
    }
}
