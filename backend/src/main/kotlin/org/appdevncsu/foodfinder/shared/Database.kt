package org.appdevncsu.foodfinder.shared

import com.google.gson.Gson
import com.google.gson.JsonArray
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
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

    private object MenusToSections : Table("menusMenuSections") {
        val menuId = reference("menuId", Menus.id)
        val sectionId = reference("sectionId", MenuSections.id)

        override val primaryKey = PrimaryKey(menuId, sectionId)
    }

    private object MenuSections : Table("menuSections") {
        val id = integer("id")
        val name = varchar("name", MAX_VARCHAR_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object SectionsToItems : Table("menuSectionsMenuItems") {
        val sectionId = reference("sectionId", MenuSections.id)
        val itemId = reference("itemId", MenuItems.id)

        override val primaryKey = PrimaryKey(sectionId, itemId)
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
            SchemaUtils.create(Locations, Menus, MenusToSections, MenuSections, SectionsToItems, MenuItems)
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
        MenusToSections.batchUpsert(sections) {
            this[MenusToSections.sectionId] = it.id
            this[MenusToSections.menuId] = it.menuId
        }
    }

    fun upsertMenuItems(items: List<MenuItem>) {
        MenuItems.batchUpsert(items) {
            this[MenuItems.id] = it.id
            this[MenuItems.name] = it.name
            this[MenuItems.flags] = it.flags
        }
        SectionsToItems.batchUpsert(items) {
            this[SectionsToItems.itemId] = it.id
            this[SectionsToItems.sectionId] = it.sectionId
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
                .leftJoin(SectionsToItems) { MenuItems.id eq SectionsToItems.itemId }
                .leftJoin(MenuSections) { MenuSections.id eq SectionsToItems.sectionId }
                .innerJoin(MenusToSections) { (MenusToSections.sectionId eq MenuSections.id) and (MenusToSections.menuId eq menuId) }
                .selectAll()
                .orderBy(MenusToSections.sectionId to SortOrder.ASC, MenusToSections.menuId to SortOrder.ASC).toList()
        }

        var section: MenuSection? = null
        for (row in iterator) {
            if (section == null || row[MenusToSections.sectionId] != section.id) {
                if (section != null) sections.add(section)
                section = MenuSection(
                    id = row[MenusToSections.sectionId],
                    menuId = row[MenusToSections.menuId],
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
