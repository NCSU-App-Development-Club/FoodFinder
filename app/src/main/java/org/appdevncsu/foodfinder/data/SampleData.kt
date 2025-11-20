package org.appdevncsu.foodfinder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiningLocation(
    val name: String, // e.g. Fountain Dining Hall
    @SerialName("id")
    val unitId: Int,
)

@Serializable
data class DiningMenuListItem(
    val date: String, // e.g. "Thursday, September 4, 2025"
    val name: String, // e.g. "Breakfast"
    @SerialName("id")
    val menuId: Int,
)

@Serializable
data class DiningMenuSection(
    @SerialName("id")
    val sectionId: Int,
    val name: String, // e.g. "Display Station" or "Home Style Entrée",
    @SerialName("menu_items")
    val menuItems: List<DiningMenuItem>
)

@Serializable
data class DiningMenuItem(
    @SerialName("id")
    val itemId: Int,
    val name: String, // e.g. Freshly Scrambled Eggs
    val flags: List<String>, // e.g. Halal, Vegan, Wolf Approved
)

val sampleMenuItems = listOf(
    DiningMenuItem(1,"Freshly Scrambled Eggs", emptyList()),
    DiningMenuItem(2,"Beer Battered Cod", emptyList()),
    DiningMenuItem(3,"Grilled Onions and Peppers", listOf("Vegetarian", "Vegan")),
    DiningMenuItem(4,"Spinach", listOf("Vegan")),
    DiningMenuItem(5,"Grilled Cheese Panini", listOf("Wolf Approved")),
)

val sampleMenuSections = listOf(
    DiningMenuSection(1, "Home Style Entree", sampleMenuItems),
    DiningMenuSection(2, "Salad Bar", sampleMenuItems),
    DiningMenuSection(3, "Fruit", sampleMenuItems),
)

val sampleMenuListItems = listOf(
    DiningMenuListItem("Thursday, September 18th, 2025", "Breakfast", 1),
    DiningMenuListItem("Thursday, September 18th, 2025", "Lunch", 2),
    DiningMenuListItem("Thursday, September 18th, 2025", "Dinner", 3),
    DiningMenuListItem("Thursday, September 19th, 2025", "Breakfast", 4),
)

val sampleLocations = listOf(
    DiningLocation("Fountain Dining Hall", 1),
    DiningLocation("Clark Dining Hall", 2),
    DiningLocation("Case Dining Hall", 3)
)
