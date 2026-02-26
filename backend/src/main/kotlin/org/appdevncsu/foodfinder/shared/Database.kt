package org.appdevncsu.foodfinder.shared

import com.google.gson.Gson
import com.google.gson.JsonArray
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json

object Database {

    private const val MAX_VARCHAR_LENGTH = 128

    private object Locations : Table("locations") {
        val id = integer("id")
        val name = varchar("name", MAX_VARCHAR_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object Menus : Table("menus") {
        val id = integer("id")
        val locationId = reference("locationId", Locations.id)
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
            { obj: List<String> -> Gson().toJson(obj) },
            { str -> Gson().fromJson(str, JsonArray::class.java).map { it.asString } })

        override val primaryKey = PrimaryKey(id)
    }

    fun init() {
        Database.connect("jdbc:h2:./data.db", driver = "org.h2.Driver")

        transaction {
            SchemaUtils.create(Locations, Menus, MenuSections, SectionsToItems, MenuItems)
        }
    }

    fun upsertLocations(locations: List<Location>) {
        Locations.batchUpsert(locations) {
            this[Locations.id] = it.id
            this[Locations.name] = it.name
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

    fun getLocations(): List<Location> {
        return transaction {
            Locations.selectAll().map {
                Location(it[Locations.id], it[Locations.name])
            }
        }
    }

    fun getMenus(locationId: Int): List<Menu> {
        return transaction {
            Menus.selectAll().where { Menus.locationId eq locationId }.map {
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

        return sections.toList()
    }
}
