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
    Database.init()
    val locations = pool.submit(Callable { Scraper.getLocations() }).get()
    val locDBTask = pool.submit { transaction { Database.upsertLocations(locations) } }

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

    locDBTask.get()

    val menus = mutableListOf<Menu>()
    val menuSections = mutableListOf<MenuSection>()
    val menuItems = mutableListOf<MenuItem>()

    for (future in futures) {
        val menuFutures = future.get()
        val pairs = menuFutures.map { it.get() }
        menus.addAll(pairs.map { it.first })
        menuSections.addAll(pairs.map { it.second }.flatten())
        menuItems.addAll(menuSections.map { it.items }.flatten())
    }

    transaction { Database.upsertLocations(locations) }
    transaction { Database.upsertMenus(menus) }
    transaction { Database.upsertMenuSections(menuSections) }
    transaction { Database.upsertMenuItems(menuItems) }

    pool.shutdown()
}
