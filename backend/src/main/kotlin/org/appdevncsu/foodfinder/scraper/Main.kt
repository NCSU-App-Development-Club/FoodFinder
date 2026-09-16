package org.appdevncsu.foodfinder.scraper

import org.appdevncsu.foodfinder.shared.Database
import org.appdevncsu.foodfinder.shared.Menu
import org.appdevncsu.foodfinder.shared.MenuItem
import org.appdevncsu.foodfinder.shared.MenuSection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = LoggerFactory.getLogger("scraper")

private val scrapeLock = ReentrantLock()

enum class ScrapeTarget { MENUS, HOURS, ALL }

fun main() {
    runScraper()
}

/** Runs a scrape, serialized against any other in-flight scrape. */
fun runScraper(target: ScrapeTarget = ScrapeTarget.ALL) {
    scrapeLock.withLock {
        log.info("Starting scrape ({})", target)
        when (target) {
            ScrapeTarget.MENUS -> scrapeMenus()
            ScrapeTarget.HOURS -> scrapeHours()
            ScrapeTarget.ALL -> {
                val pool = Executors.newVirtualThreadPerTaskExecutor()
                try {
                    val menusTask = pool.submit { scrapeMenus() }
                    val hoursTask = pool.submit { scrapeHours() }
                    menusTask.get()
                    hoursTask.get()
                } finally {
                    pool.shutdown()
                }
            }
        }
        log.info("Scrape ({}) finished", target)
    }
}

/** Scrapes NetNutrition menus and stores locations, menus, sections, and items. */
private fun scrapeMenus() {
    Database.init()
    val locations = Scraper.getLocations()
    transaction { Database.upsertLocations(locations) }

    val pool = Executors.newVirtualThreadPerTaskExecutor()
    val menus = mutableListOf<Menu>()
    val menuSections = mutableListOf<MenuSection>()
    val menuItems = mutableListOf<Pair<Int /* menuId */, MenuItem>>()
    try {
        val futures = pool.invokeAll(locations.map { loc ->
            Callable {
                val menus = Scraper.getMenus(loc.id)
                pool.invokeAll(
                    menus.map { menu ->
                        Callable { menu to Scraper.getMenu(menu.id) }
                    }
                )
            }
        })

        for (future in futures) { // For every location
            val menuFutures = future.get()
            val pairs = menuFutures.map { it.get() }
            for ((menu, sections) in pairs) { // For every menu found at that location
                menus.add(menu)
                menuSections.addAll(sections)
                menuItems.addAll(sections.flatMap { section -> section.items.map { menu.id to it } })
            }
        }

        log.info(
            "Found {} locations, {} menus, {} menu sections, and {} menu items ({} excluding duplicates).",
            locations.size,
            menus.size,
            menuSections.size,
            menuItems.size,
            menuItems.distinctBy { it.second.id }.count(),
        )

        transaction { Database.replaceMenus(menus) }
        transaction { Database.upsertMenuSections(menuSections) }
        transaction { Database.upsertMenuItems(menuItems) }
    } finally {
        pool.shutdown()
    }
}

/** Scrapes dining.ncsu.edu hours and stores locations and open hours. */
private fun scrapeHours() {
    Database.init()
    val result = DiningHoursScraper.scrapeAndStore()
    transaction {
        Database.upsertDiningLocations(result.locations)
        Database.replaceHours(result.hours)
    }
}
