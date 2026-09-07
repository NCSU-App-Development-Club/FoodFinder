package org.appdevncsu.foodfinder.scraper

import org.appdevncsu.foodfinder.shared.Database
import org.appdevncsu.foodfinder.shared.Menu
import org.appdevncsu.foodfinder.shared.MenuItem
import org.appdevncsu.foodfinder.shared.MenuSection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.Callable
import java.util.concurrent.Executors

fun main() {
    runScraper()
}

fun runScraper() {
    val pool = Executors.newVirtualThreadPerTaskExecutor()
    val dbInitTask = pool.submit { Database.init() }
    val locationsTask = pool.submit(Callable { Scraper.getLocations() })
    val locDBTask = pool.submit {
        dbInitTask.get()
        val locations = locationsTask.get()
        transaction { Database.upsertLocations(locations) }
    }

    // Scrape dining menu hours (dining.ncsu.edu)
    val hoursTask = pool.submit(Callable {
        dbInitTask.get()
        DiningHoursScraper.scrapeAndStore()
    })

    // Scrape menus (NetNutrition)
    val locations = locationsTask.get()
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

    val menus = mutableListOf<Menu>()
    val menuSections = mutableListOf<MenuSection>()
    val menuItems = mutableListOf<Pair<Int /* menuId */, MenuItem>>()

    for (future in futures) { // For every location
        val menuFutures = future.get()
        val pairs = menuFutures.map { it.get() }
        for ((menu, sections) in pairs) { // For every menu found at that location
            menus.add(menu)
            menuSections.addAll(sections)
            menuItems.addAll(sections.map { section -> section.items.map { menu.id to it } }.flatten())
        }
    }

    println(
        "Found ${locations.size} locations, ${menus.size} menus, " +
            "${menuSections.size} menu sections, and ${menuItems.size} menu items " +
            "(${menuItems.distinctBy { it.second.id }.count()} excluding duplicates)."
    )

    locDBTask.get()
    transaction { Database.upsertMenus(menus) }
    transaction { Database.upsertMenuSections(menuSections) }
    transaction { Database.upsertMenuItems(menuItems) }

    val hoursResult = hoursTask.get()
    transaction {
        Database.upsertDiningLocations(hoursResult.locations)
        Database.replaceHours(hoursResult.hours)
    }

    pool.shutdown()
}
