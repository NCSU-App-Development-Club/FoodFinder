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
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.round

object Database {

    private val initialized = AtomicBoolean(false)

    private const val MAX_VARCHAR_LENGTH = 128

    private const val TURNOVER_WINDOW_DAYS = 14L
    private const val TURNOVER_MIN_MENUS = 3L
    private const val TURNOVER_CACHE_TTL_NANOS = 10L * 60 * 1_000_000_000 // 10 minutes

    private const val HISTORY_WINDOW_DAYS = 90L
    private const val DAYS_PER_WEEK = 7.0

    private data class TurnoverCacheEntry(
        val expiresAtNanos: Long,
        val value: Map<Int, Double>
    )

    private data class FavoriteRow(
        val menuId: Int,
        val menuName: String,
        val date: LocalDate,
        val locationId: Int,
        val locationName: String,
        val itemName: String,
    )

    private data class MenuQueryData(
        val menuDate: LocalDate,
        val turnover: Map<Int, Double>,
        val firstSeenByName: Map<String, LocalDate>,
        val rows: List<ResultRow>,
    )

    private val turnoverCache = ConcurrentHashMap<Int, TurnoverCacheEntry>()

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

        init {
            index("menus_location_date", false, locationId, date)
        }
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

        init {
            index("sections_to_items_menu", false, menuId)
        }
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

        init {
            index("location_hours_date", false, date)
        }
    }

    fun init() {
        if (!initialized.compareAndSet(false, true)) return
        val dbPath = File(dataDir(), "data.db").path
        val config = SQLiteConfig().apply {
            setJournalMode(SQLiteConfig.JournalMode.WAL)
            setBusyTimeout(5_000)
            setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
            enforceForeignKeys(true)
        }
        val dataSource = SQLiteDataSource(config).apply { url = "jdbc:sqlite:$dbPath" }
        Database.connect(dataSource)

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

    /** True when no scraped data has been stored yet. */
    fun isEmpty(): Boolean {
        return transaction {
            MenuLocations.selectAll().none() && DiningLocations.selectAll().none()
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

    fun replaceMenus(menus: List<Menu>) {
        for ((locationId, locationMenus) in menus.groupBy { it.locationId }) {
            val scrapedIds = locationMenus.map { it.id }
            val scrapedDates = locationMenus.map { it.date }.distinct()
            val staleIds = Menus
                .select(Menus.id)
                .where {
                    (Menus.locationId eq locationId) and
                        (Menus.date inList scrapedDates) and
                        (Menus.id notInList scrapedIds)
                }
                .map { it[Menus.id] }
            if (staleIds.isNotEmpty()) {
                // Remove connections that no longer exist after the most recent scrape
                SectionsToItems.deleteWhere { SectionsToItems.menuId inList staleIds }
                Menus.deleteWhere { Menus.id inList staleIds }
            }
        }
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

    /** Returns each location's hours for [days] consecutive days starting at [start]. */
    fun getDiningSchedules(start: LocalDate, days: Int): List<DiningLocationSchedule> {
        val end = start.plusDays((days - 1).toLong())
        return transaction {
            val locationsBySlug = DiningLocations
                .leftJoin(MenuLocations, { DiningLocations.unitId }, { MenuLocations.id })
                .selectAll().associate {
                    it[DiningLocations.slug] to
                            ((it[MenuLocations.name] ?: it[DiningLocations.name]) to it[DiningLocations.type])
                }

            val hours = LocationHours
                .selectAll()
                .where { (LocationHours.date greaterEq start) and (LocationHours.date lessEq end) }
                .orderBy(
                    LocationHours.slug to SortOrder.ASC,
                    LocationHours.date to SortOrder.ASC,
                    LocationHours.seq to SortOrder.ASC
                )
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
                val days = hours.filter { it.slug == slug }
                    .groupBy { it.date }
                    .toSortedMap()
                    .map { (date, rows) ->
                        DiningDayHours(
                            date = date.toString(),
                            hours = rows.sortedBy { it.seq }.map { it.toHoursRange() }
                        )
                    }
                DiningLocationSchedule(
                    slug = slug,
                    name = name,
                    type = type,
                    days = days
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

    /** The latest menu date stored for [locationId], if the location has any menus. */
    fun getLatestMenuDate(locationId: Int): LocalDate? {
        return transaction {
            Menus.select(Menus.date.max())
                .where { Menus.locationId eq locationId }
                .firstOrNull()
                ?.get(Menus.date.max())
        }
    }

    /**
     * Returns the menus in the next [days] days (starting at [start]) that contain any of
     * [names], grouped by menu. Matching is case- and whitespace-insensitive because menu
     * item IDs are not stable across scrapes and only the name identifies a dish.
     */
    fun getMenusContainingItems(
        names: Collection<String>,
        start: LocalDate,
        days: Int,
    ): List<FavoriteMatch> {
        val normalized = names.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        if (normalized.isEmpty()) return emptyList()
        val end = start.plusDays((days - 1).toLong())

        val rows = transaction {
            MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
                .innerJoin(Menus) { SectionsToItems.menuId eq Menus.id }
                .innerJoin(MenuLocations) { Menus.locationId eq MenuLocations.id }
                .select(Menus.id, Menus.name, Menus.date, Menus.locationId, MenuLocations.name, MenuItems.name)
                .where {
                    (MenuItems.name.lowerCase() inList normalized) and
                        (Menus.date greaterEq start) and (Menus.date lessEq end)
                }
                .orderBy(Menus.date to SortOrder.ASC, Menus.id to SortOrder.ASC)
                .map {
                    FavoriteRow(
                        menuId = it[Menus.id],
                        menuName = it[Menus.name],
                        date = it[Menus.date],
                        locationId = it[Menus.locationId],
                        locationName = it[MenuLocations.name],
                        itemName = it[MenuItems.name],
                    )
                }
        }

        return rows
            .groupBy { it.menuId to it.locationId }
            .map { (_, menuRows) ->
                val first = menuRows.first()
                FavoriteMatch(
                    locationId = first.locationId,
                    locationName = first.locationName,
                    menuId = first.menuId,
                    menuName = first.menuName,
                    date = first.date,
                    items = menuRows.map { it.itemName }.distinct(),
                )
            }
    }

    fun getMenu(menuId: Int): List<MenuSection> {
        val data = transaction {
            val menu = Menus
                .selectAll()
                .where { Menus.id eq menuId }
                .singleOrNull()
                ?: return@transaction null
            val locationId = menu[Menus.locationId]

            val rows = MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId and (SectionsToItems.menuId eq menuId) }
                .leftJoin(MenuSections) { MenuSections.id eq SectionsToItems.sectionId }
                .selectAll()
                .orderBy(MenuSections.id to SortOrder.ASC).toList()

            MenuQueryData(
                menuDate = menu[Menus.date],
                turnover = getSectionTurnover(locationId),
                firstSeenByName = getFirstSeenByLocation(locationId),
                rows = rows,
            )
        } ?: return emptyList()

        val sections = mutableListOf<MenuSection>()
        var section: MenuSection? = null
        for (row in data.rows) {
            if (section == null || row[MenuSections.id] != section.id) {
                if (section != null) sections.add(section)
                section = MenuSection(
                    id = row[MenuSections.id],
                    name = row[MenuSections.name],
                    items = mutableListOf()
                )
            }
            val name = row[MenuItems.name]
            val firstSeen = data.firstSeenByName[name.trim().lowercase()]
            (section.items as MutableList).add(
                MenuItem(
                    id = row[MenuItems.id],
                    sectionId = row[SectionsToItems.sectionId],
                    name = name,
                    flags = row[MenuItems.flags],
                    isNew = firstSeen == null || firstSeen >= data.menuDate
                )
            )
        }
        if (section != null) sections.add(section)

        return sections.sortedWith(
            compareBy(
                { if (it.id in data.turnover) 1 else 0 },
                { data.turnover[it.id] ?: 0.0 },
                { it.id }
            )
        )
    }

    /**
     * Maps each normalized menu item name to the earliest menu date it appeared in at
     * [locationId]. Item IDs are recreated on every scrape, so only the name is stable.
     * Used to flag items that are new to a location.
     */
    private fun getFirstSeenByLocation(locationId: Int): Map<String, LocalDate> {
        val normalizedName = MenuItems.name.lowerCase()
        val earliestDate = Menus.date.min()
        return MenuItems
            .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
            .innerJoin(Menus) { SectionsToItems.menuId eq Menus.id }
            .select(normalizedName, earliestDate)
            .where { Menus.locationId eq locationId }
            .groupBy(normalizedName)
            .mapNotNull {
                val earliest = it[earliestDate] ?: return@mapNotNull null
                it[normalizedName].trim().lowercase() to earliest
            }
            .toMap()
    }

    /**
     * Returns the dates in the [HISTORY_WINDOW_DAYS]-day window ending on [endDate] that an
     * item (matched by trimmed, lowercased name) was on the menu at [locationId], plus its
     * all-time first appearance and average frequency in days per week.
     */
    fun getMenuItemHistory(locationId: Int, rawName: String, endDate: LocalDate): MenuItemHistory? {
        val normalized = rawName.trim().lowercase()
        if (normalized.isEmpty()) return null
        val windowStart = endDate.minusDays(HISTORY_WINDOW_DAYS - 1)
        return transaction {
            val firstSeen = MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
                .innerJoin(Menus) { SectionsToItems.menuId eq Menus.id }
                .select(Menus.date.min())
                .where {
                    (Menus.locationId eq locationId) and
                        (MenuItems.name.lowerCase() eq normalized)
                }
                .firstOrNull()
                ?.get(Menus.date.min())

            val dates = MenuItems
                .innerJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
                .innerJoin(Menus) { SectionsToItems.menuId eq Menus.id }
                .select(Menus.date)
                .where {
                    (Menus.locationId eq locationId) and
                        (MenuItems.name.lowerCase() eq normalized) and
                        (Menus.date greaterEq windowStart) and (Menus.date lessEq endDate)
                }
                .map { it[Menus.date] }
                .distinct()
                .sorted()

            MenuItemHistory(
                locationId = locationId,
                name = rawName.trim(),
                firstSeen = firstSeen?.toString(),
                frequencyPerWeek = if (dates.isEmpty()) {
                    0.0
                } else {
                    round(dates.size * DAYS_PER_WEEK / HISTORY_WINDOW_DAYS * 10) / 10
                },
                dates = dates.map { it.toString() },
            )
        }
    }

    private fun getSectionTurnover(locationId: Int): Map<Int, Double> {
        val now = System.nanoTime()
        turnoverCache[locationId]?.let { entry ->
            if (now < entry.expiresAtNanos) return entry.value
        }
        val turnover = computeSectionTurnover(locationId)
        turnoverCache[locationId] = TurnoverCacheEntry(now + TURNOVER_CACHE_TTL_NANOS, turnover)
        return turnover
    }

    private fun computeSectionTurnover(locationId: Int): Map<Int, Double> {
        val occurrences = SectionsToItems.itemId.count()
        val distinctNames = MenuItems.name.countDistinct()
        val distinctMenus = Menus.id.countDistinct()
        return MenuItems
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
