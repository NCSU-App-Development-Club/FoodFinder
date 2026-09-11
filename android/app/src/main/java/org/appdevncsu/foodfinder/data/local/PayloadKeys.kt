package org.appdevncsu.foodfinder.data.local

import java.time.LocalDate

/** Builders for the [CachedPayload] keys. */
object PayloadKeys {
    const val LOCATIONS = "locations"

    const val HoursPrefix = "hours:"
    const val MenusPrefix = "menus:"
    const val SectionsPrefix = "sections:"

    fun hours(date: LocalDate): String = "$HoursPrefix$date"

    fun menus(locationId: Int): String = "$MenusPrefix$locationId"

    fun sections(locationId: Int, menuId: Int): String = "$SectionsPrefix$locationId:$menuId"
}
