package org.appdevncsu.foodfinder.shared

import kotlinx.serialization.Serializable
import org.appdevncsu.foodfinder.server.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class Location(
    val id: Int,
    val name: String, // e.g. Fountain Dining Hall
)

@Serializable
data class Menu(
    val id: Int,
    val locationId: Int, // -> Location
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val name: String, // e.g. "Breakfast"
)

@Serializable
data class MenuSection(
    val id: Int,
    val menuId: Int, // -> Menu
    val name: String, // e.g. "Display Station" or "Home Style Entrée"
    val items: List<MenuItem>
)

@Serializable
data class MenuItem(
    val id: Int,
    val sectionId: Int, // -> MenuSection
    val name: String, // e.g. Freshly Scrambled Eggs
    val flags: List<String> // e.g. Halal, Vegan, Wolf Approved
)