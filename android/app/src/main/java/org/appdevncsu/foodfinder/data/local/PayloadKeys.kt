package org.appdevncsu.foodfinder.data.local

/** Builders for the [CachedPayload] keys. */
object PayloadKeys {
    const val LOCATIONS = "locations"
    const val HOURS = "hours"
    const val EVENTS = "events"

    const val MenusPrefix = "menus:"
    const val SectionsPrefix = "sections:"
    const val FavoriteMatchesPrefix = "favoriteMatches:"
    const val ItemHistoryPrefix = "itemHistory:"

    fun menus(locationId: Int): String = "$MenusPrefix$locationId"

    fun sections(locationId: Int, menuId: Int): String = "$SectionsPrefix$locationId:$menuId"

    fun favoriteMatches(date: String): String = "$FavoriteMatchesPrefix$date"

    fun itemHistory(locationId: Int, name: String, date: String): String =
        "$ItemHistoryPrefix$locationId:$date:${name.trim().lowercase()}"
}
