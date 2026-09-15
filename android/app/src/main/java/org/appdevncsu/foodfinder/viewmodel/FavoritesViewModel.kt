package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.FavoriteMatch
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.mealOrder
import org.appdevncsu.foodfinder.data.normalizeFavoriteName
import org.appdevncsu.foodfinder.data.repository.FavoritesRepository
import javax.inject.Inject

/** Today's menus that contain the user's favorites, grouped by location and then by menu. */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoritesRepository,
) : ViewModel() {

    data class MenuGroup(
        val locationId: Int,
        val menuId: Int,
        val menuName: String,
        val date: String,
        val items: List<String>,
    )

    data class LocationGroup(
        val locationId: Int,
        val locationName: String,
        val menus: List<MenuGroup>,
    )

    data class UiState(
        val loading: Boolean = true,
        val groups: List<LocationGroup> = emptyList(),
    )

    val uiState: StateFlow<UiState> = combine(
        repository.observeTodayMatches(),
        repository.observeNormalizedNames(),
    ) { response, favoriteNames ->
        UiState(
            loading = response == null,
            groups = groupMatches(response?.matches.orEmpty(), favoriteNames),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        refreshMatches()
    }

    fun toggleFavorite(name: String) {
        viewModelScope.launch {
            // Removing patches the cache locally; adding needs a fetch to find its locations.
            if (repository.toggle(name)) {
                refreshMatchesBestEffort()
            }
        }
    }

    private fun refreshMatches() {
        viewModelScope.launch { refreshMatchesBestEffort() }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshMatchesBestEffort() {
        try {
            repository.refreshMatches()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logApiError(TAG, e)
        }
    }

    private fun groupMatches(
        matches: List<FavoriteMatch>,
        favoriteNames: Set<String>,
    ): List<LocationGroup> =
        matches
            .mapNotNull { match ->
                val items = match.items.filter { normalizeFavoriteName(it) in favoriteNames }
                if (items.isEmpty()) null else match to items
            }
            .groupBy { it.first.locationId }
            .map { (locationId, entries) ->
                val menus = entries
                    .map { (match, items) ->
                        MenuGroup(
                            locationId = match.locationId,
                            menuId = match.menuId,
                            menuName = match.menuName,
                            date = match.date,
                            items = items,
                        )
                    }
                    .sortedWith(
                        compareBy(
                            { mealOrder(it.menuName) },
                            { it.menuName.lowercase() },
                            { it.menuId },
                        )
                    )
                LocationGroup(locationId, entries.first().first.locationName, menus)
            }
            .sortedBy { it.locationName.lowercase() }

    private companion object {
        const val TAG = "FavoritesViewModel"
    }
}
