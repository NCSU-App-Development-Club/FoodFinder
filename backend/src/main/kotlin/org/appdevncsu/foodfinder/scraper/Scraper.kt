package org.appdevncsu.foodfinder.scraper

import com.fleeksoft.ksoup.Ksoup
import org.appdevncsu.foodfinder.shared.Location
import org.appdevncsu.foodfinder.shared.Menu
import org.appdevncsu.foodfinder.shared.MenuItem
import org.appdevncsu.foodfinder.shared.MenuSection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object Scraper {

    private val unitIdPattern = Regex("^javascript:NetNutrition\\.UI\\.unitsSelectUnit\\(([\\d-]+)\\);$")
    private val menuIdPattern = Regex("^javascript:NetNutrition\\.UI\\.menuListSelectMenu\\(([\\d-]+)\\);$")
    private val sectionIdPattern = Regex("^NetNutrition\\.UI\\.toggleCourseItems\\(this, ([\\d-]+)\\);$")
    private val itemIdPattern =
        Regex("^javascript:NetNutrition\\.UI\\.getItemNutritionLabelOnClick\\(event,([\\d-]+)\\);$")

    fun getLocations(): List<Location> {
        val doc = HttpClient.getHTMLContent("/")

        val locations = doc.select(".card.unit a").map { link ->
            val unitId = unitIdPattern.matchEntire(link.attr("onclick"))?.groups?.get(1)?.value?.toInt()
                ?: error("Failed to extract Unit ID from dining location ${link.text()}")
            Location(
                id = unitId,
                name = link.text()
            )
        }

        return locations
    }

    private val dateFormat = DateTimeFormatter.ofPattern(
        "EEEE, MMMM d, yyyy",
        Locale.ENGLISH
    )

    fun getMenus(unitId: Int): List<Menu> {
        val root = HttpClient.postWithFormData("/Unit/SelectUnitFromUnitsList", "unitOid=$unitId")
        if (!root.get("success").asBoolean) {
            error("success=false")
        }

        val menuHtml =
            root.get("panels").asJsonArray.find { it.asJsonObject.get("id").asString == "menuPanel" }?.asJsonObject?.get(
                "html"
            )?.asString
        if (menuHtml == null) {
            error("Failed to find menu panel")
        }

        val doc = Ksoup.parse(menuHtml)

        val cards = doc.select(".card-block")
        val menus = cards.flatMap { card ->
            val dateString = card.select("header").text() // e.g. "Thursday, September 4, 2025"
            val date = LocalDate.parse(dateString, dateFormat)
            card.select("a").map { link ->
                val menuId = menuIdPattern.matchEntire(link.attr("onclick"))?.groups?.get(1)?.value?.toInt()
                    ?: error("Failed to extract Menu ID from dining location ${link.text()}")

                Menu(
                    id = menuId,
                    locationId = unitId,
                    date = date,
                    name = link.text()
                )
            }
        }

        return menus
    }

    fun getMenu(menuId: Int): List<MenuSection> {
        val root = HttpClient.postWithFormData("/Menu/SelectMenu", "menuOid=$menuId")
        if (!root.get("success").asBoolean) {
            error("success=false")
        }

        val itemsHtml = root.get("panels").asJsonArray
            .find { it.asJsonObject.get("id").asString == "itemPanel" }
            ?.asJsonObject?.get("html")?.asString

        if (itemsHtml == null) {
            error("Failed to find item panel")
        }

        val doc = Ksoup.parse(itemsHtml)

        val rows = doc.select("tbody > tr")
        if (rows.isEmpty()) {
            // This menu is empty; no menu items to return
            return listOf()
        }
        val categories = mutableListOf<MenuSection>()
        var categoryName: String? = null
        var categoryId: Int? = null

        val items = mutableListOf<MenuItem>()
        for (row in rows) {
            if (row.hasAttr("onclick")) {
                // This is a category row
                if (categoryName != null && categoryId != null) {
                    categories.add(MenuSection(id = categoryId, menuId = menuId, name = categoryName, items = items))
                }
                categoryId = sectionIdPattern.matchEntire(row.attr("onclick"))!!.groups[1]!!.value.toInt()
                categoryName = row.text()
            } else {
                // This is a menu item row
                if (categoryName == null || categoryId == null) {
                    error("Found menu item row before finding first category name: $row")
                }

                val itemId =
                    itemIdPattern.matchEntire(row.select("a").first()!!.attr("onclick"))?.groups?.get(1)?.value?.toInt()
                        ?: error("Failed to parse Menu Item ID from row: $row")

                items.add(
                    MenuItem(
                        id = itemId,
                        sectionId = categoryId,
                        name = row.select("a").first()!!.text(),
                        flags = row.select("img[title]").map { it.attr("title") },
                    )
                )
            }
        }

        categories.add(MenuSection(id = categoryId!!, menuId = menuId, name = categoryName!!, items = items))

        return categories
    }
}
