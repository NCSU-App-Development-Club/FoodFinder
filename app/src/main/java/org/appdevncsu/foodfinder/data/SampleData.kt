package org.appdevncsu.foodfinder.data

import org.appdevncsu.foodfinder.R

data class DiningLocation(
    val name: String, // e.g. Fountain Dining Hall
    val imageRes: Int,
    val unitId: Int,
    val menus: List<DiningMenuListItem>
)

data class DiningMenuListItem(
    val date: String, // e.g. "Thursday, September 4, 2025"
    val name: String, // e.g. "Breakfast"
    val menuId: Int,
    val menu: List<DiningMenuSection>
)

data class DiningMenuSection(
    val sectionId: Int,
    val name: String, // e.g. "Display Station" or "Home Style Entrée"
    val items: List<DiningMenuItem>
)

data class DiningMenuItem(
    val name: String, // e.g. Freshly Scrambled Eggs
    val flags: List<String>, // e.g. Halal, Vegan, Wolf Approved
    val itemId: Int
)

val sampleMenuItems = listOf(
    DiningMenuItem("Freshly Scrambled Eggs", emptyList(), 1),
    DiningMenuItem("Beer Battered Cod", emptyList(), 2),
    DiningMenuItem("Grilled Onions and Peppers", listOf("Vegetarian"), 3),
    DiningMenuItem("Spinach", listOf("Vegan"), 4),
    DiningMenuItem("Grilled Cheese Panini", listOf("Wolf Approved"), 5),
)

val sampleMenuSections = listOf(
    DiningMenuSection(1, "Home Style Entree", sampleMenuItems),
    DiningMenuSection(2, "Salad Bar", sampleMenuItems),
    DiningMenuSection(3, "Fruit", sampleMenuItems),
)

val sampleMenuListItems = listOf(
    DiningMenuListItem("Thursday, September 18th, 2025", "Breakfast", 1, sampleMenuSections),
    DiningMenuListItem("Thursday, September 18th, 2025", "Lunch", 2, sampleMenuSections),
    DiningMenuListItem("Thursday, September 18th, 2025", "Dinner", 3, sampleMenuSections),
)

val sampleLocations = listOf(
    DiningLocation("Fountain Dining Hall", R.drawable.fount, 1, sampleMenuListItems),
    DiningLocation("Clark Dining Hall", R.drawable.clark, 2, sampleMenuListItems),
    DiningLocation("Case Dining Hall", R.drawable.cased, unitId = 3, menus = sampleMenuListItems)
)