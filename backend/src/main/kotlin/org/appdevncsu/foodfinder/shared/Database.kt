package org.appdevncsu.foodfinder.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json
import java.time.LocalDate

object Database {

    private const val MAX_VARCHAR_LENGTH = 128

    private const val TURNOVER_WINDOW_DAYS = 14L
    private const val TURNOVER_MIN_MENUS = 3L

    // Locations sourced from NetNutrition (netmenu2.cbord.com), keyed by NetNutrition unit ID.
    private object MenuLocations : Table("menuLocations") {
        val id = integer("id")
        val name = varchar("name", MAX_VARCHAR_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object Menus : Table("menus") {
        val id = integer("id")
        val locationId = reference("locationId", MenuLocations.id)
        val date = date("date")
        val name = varchar("name", MAX_VARCHAR_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object MenuSections : Table("menuSections") {
        val id = integer("id")
        val name = varchar("name", MAX_VARCHAR_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object SectionsToItems : Table("menuSectionsMenuItems") {
        val menuId = reference("menuId", Menus.id)
        val sectionId = reference("sectionId", MenuSections.id)
        val itemId = reference("itemId", MenuItems.id)

        override val primaryKey = PrimaryKey(sectionId, itemId, menuId)
    }

    private object MenuItems : Table("menuItems") {
        val id = integer("id")
        val name = varchar("name", MAX_VARCHAR_LENGTH)
        val flags = json(
            "flags",
            { obj: List<String> -> Json.encodeToString(obj) },
            { str -> Json.decodeFromString<JsonArray>(str).map { it.jsonPrimitive.content } })

        override val primaryKey = PrimaryKey(id)
    }

    // Locations sourced from dining.ncsu.edu, keyed by URL slug.
    private object DiningLocations : Table("diningLocations") {
        val slug = varchar("slug", 64) // e.g. "fountain"
        val name = varchar("name", MAX_VARCHAR_LENGTH)
        val type = varchar("type", 32) // e.g. "dining-halls"
        val imageUrl = varchar("imageUrl", 256).nullable()
        val unitId = integer("unitId").nullable() // NetNutrition unit ID, if applicable

        override val primaryKey = PrimaryKey(slug)
    }

    // One row per open range per location per day, or a single "closed"/"unknown" row.
    private object LocationHours : Table("locationHours") {
        val slug = reference("slug", DiningLocations.slug)
        val date = date("date")
        val seq = integer("seq")
        val status = varchar("status", 16) // "open" | "closed" | "unknown"
        val openMinute = integer("openMinute").nullable() // minutes after midnight
        val closeMinute = integer("closeMinute").nullable() // may exceed 1440
        val rawText = varchar("rawText", 256) // verbatim text from the site

        override val primaryKey = PrimaryKey(slug, date, seq)
    }

    fun init() {
        Database.connect("jdbc:h2:./data.db", driver = "org.h2.Driver")

        transaction {
            SchemaUtils.create(
                MenuLocations,
                Menus,
                MenuSections,
                SectionsToItems,
                MenuItems,
                DiningLocations,
                LocationHours
            )
        }
    }

    fun upsertLocations(menuLocations: List<MenuLocation>) {
        MenuLocations.batchUpsert(menuLocations) {
            this[MenuLocations.id] = it.id
            this[MenuLocations.name] = it.name
        }
    }

    fun upsertDiningLocations(locations: List<DiningLocation>) {
        for (location in locations) {
            DiningLocations.upsert {
                it[DiningLocations.slug] = location.slug
                it[DiningLocations.name] = location.name
                it[DiningLocations.type] = location.type
                it[DiningLocations.imageUrl] = location.imageUrl
                it[DiningLocations.unitId] = location.unitId
            }
        }
    }

    fun replaceHours(rows: List<DiningLocationHours>) {
        val keys = rows.map { it.slug to it.date }.distinct()
        for ((slug, date) in keys) {
            LocationHours.deleteWhere {
                (LocationHours.slug eq slug) and (LocationHours.date eq date)
            }
        }
        LocationHours.batchInsert(rows, shouldReturnGeneratedValues = false) {
            this[LocationHours.slug] = it.slug
            this[LocationHours.date] = it.date
            this[LocationHours.seq] = it.seq
            this[LocationHours.status] = it.status
            this[LocationHours.openMinute] = it.openMinute
            this[LocationHours.closeMinute] = it.closeMinute
            this[LocationHours.rawText] = it.rawText
        }
    }

    fun upsertMenus(menus: List<Menu>) {
        Menus.batchUpsert(menus) {
            this[Menus.id] = it.id
            this[Menus.date] = it.date
            this[Menus.name] = it.name
            this[Menus.locationId] = it.locationId
        }
    }

    fun upsertMenuSections(sections: List<MenuSection>) {
        MenuSections.batchUpsert(sections) {
            this[MenuSections.id] = it.id
            this[MenuSections.name] = it.name
        }
    }

    fun upsertMenuItems(items: List<Pair<Int /* menuId*/, MenuItem>>) {
        MenuItems.batchUpsert(items) {
            this[MenuItems.id] = it.second.id
            this[MenuItems.name] = it.second.name
            this[MenuItems.flags] = it.second.flags
        }
        SectionsToItems.batchUpsert(items) {
            this[SectionsToItems.itemId] = it.second.id
            this[SectionsToItems.sectionId] = it.second.sectionId
            this[SectionsToItems.menuId] = it.first
        }
    }

    fun getLocationSummaries(): List<LocationSummary> {
        return transaction {
            MenuLocations
                .leftJoin(DiningLocations, { MenuLocations.id }, { DiningLocations.unitId })
                .selectAll()
                .orderBy(MenuLocations.id to SortOrder.ASC)
                .map {
                    LocationSummary(
                        id = it[MenuLocations.id],
                        name = it[MenuLocations.name],
                        slug = it[DiningLocations.slug],
                        type = it[DiningLocations.type],
                        imageUrl = "/api/locations/${it[DiningLocations.slug]}/image"
                    )
                }
        }
    }

    fun getLocationImageUrl(slug: String): String? {
        return transaction {
            DiningLocations.selectAll()
                .where { DiningLocations.slug eq slug }
                .singleOrNull()?.get(DiningLocations.imageUrl)
        }
    }

    fun getDiningSchedules(date: LocalDate): List<DiningLocationSchedule> {
        return transaction {
            val locationsBySlug = DiningLocations
                .leftJoin(MenuLocations, { DiningLocations.unitId }, { MenuLocations.id })
                .selectAll().associate {
                    it[DiningLocations.slug] to
                            ((it[MenuLocations.name] ?: it[DiningLocations.name]) to it[DiningLocations.type])
                }

            val hours = LocationHours
                .selectAll()
                .where { LocationHours.date eq date }
                .orderBy(LocationHours.slug to SortOrder.ASC, LocationHours.seq to SortOrder.ASC)
                .map {
                    DiningLocationHours(
                        slug = it[LocationHours.slug],
                        date = it[LocationHours.date],
                        seq = it[LocationHours.seq],
                        status = it[LocationHours.status],
                        openMinute = it[LocationHours.openMinute],
                        closeMinute = it[LocationHours.closeMinute],
                        rawText = it[LocationHours.rawText]
                    )
                }

            locationsBySlug.map { (slug, nameAndType) ->
                val (name, type) = nameAndType
                DiningLocationSchedule(
                    slug = slug,
                    name = name,
                    type = type,
                    hours = hours.filter { it.slug == slug }
                        .sortedBy { it.seq }
                        .map { it.toHoursRange() }
                )
            }
        }
    }

    private fun DiningLocationHours.toHoursRange(): HoursRange {
        return HoursRange(
            status = status,
            openMinute = openMinute,
            closeMinute = closeMinute,
            rawText = rawText
        )
    }

    fun getMenus(locationId: Int): List<Menu> {
        return transaction {
            Menus.selectAll()
                .where {
                    (Menus.locationId eq locationId) and (Menus.date greaterEq LocalDate.now())
                }.map {
                    Menu(it[Menus.id], it[Menus.locationId], it[Menus.date], it[Menus.name])
                }
        }
    }

    fun getMenu(menuId: Int): List<MenuSection> {
        val sections = mutableListOf<MenuSection>()
        val iterator = transaction {
            MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId and (SectionsToItems.menuId eq menuId) }
                .leftJoin(MenuSections) { MenuSections.id eq SectionsToItems.sectionId }
                .selectAll()
                .orderBy(MenuSections.id to SortOrder.ASC).toList()
        }

        var section: MenuSection? = null
        for (row in iterator) {
            if (section == null || row[MenuSections.id] != section.id) {
                if (section != null) sections.add(section)
                section = MenuSection(
                    id = row[MenuSections.id],
                    name = row[MenuSections.name],
                    items = mutableListOf()
                )
            }
            (section.items as MutableList).add(
                MenuItem(
                    id = row[MenuItems.id],
                    sectionId = row[SectionsToItems.sectionId],
                    name = row[MenuItems.name],
                    flags = row[MenuItems.flags]
                )
            )
        }
        if (section != null) sections.add(section)

        val locationId = transaction {
            Menus.selectAll()
                .where { Menus.id eq menuId }
                .singleOrNull()?.get(Menus.locationId)
        } ?: return emptyList()

        val turnover = getSectionTurnover(locationId)
        return sections.sortedWith(
            compareBy(
                { if (it.id in turnover) 1 else 0 },
                { turnover[it.id] ?: 0.0 },
                { it.id }
            )
        )
    }

    private fun getSectionTurnover(locationId: Int): Map<Int, Double> {
        val occurrences = SectionsToItems.itemId.count()
        val distinctNames = MenuItems.name.countDistinct()
        val distinctMenus = Menus.id.countDistinct()
        return transaction {
            MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
                .innerJoin(Menus) { SectionsToItems.menuId eq Menus.id }
                .select(listOf(SectionsToItems.sectionId, occurrences, distinctNames, distinctMenus))
                .where {
                    (Menus.locationId eq locationId) and
                        (Menus.date greaterEq LocalDate.now().minusDays(TURNOVER_WINDOW_DAYS))
                }
                .groupBy(SectionsToItems.sectionId)
                .having { distinctMenus greaterEq TURNOVER_MIN_MENUS }
                .associate {
                    it[SectionsToItems.sectionId] to (it[occurrences].toDouble() / it[distinctNames])
                }
        }
    }
}
