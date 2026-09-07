package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.HoursRange
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.LocationListItem
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.data.currentStatus
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.userMessageFor
import javax.inject.Inject

private const val DiningHallType = "dining-halls"

private val typeOrder = listOf("dining-halls", "food-courts", "restaurants", "cafes", "markets")

private val locationComparator =
    compareByDescending<LocationListItem> {
        it.status is LocationStatus.Open || it.status is LocationStatus.ClosingSoon
    }
        .thenBy { item ->
            item.location.type?.let(typeOrder::indexOf)?.takeIf { it >= 0 } ?: typeOrder.size
        }
        .thenBy { it.location.name }

@HiltViewModel
class LocationListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val hoursLoading: Boolean = true,
        val items: List<LocationListItem> = emptyList(),
        val error: String? = null,
    )

    private val _locations = MutableStateFlow<List<Location>?>(null)
    private val _hoursBySlug = MutableStateFlow<Map<String, List<HoursRange>>?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState> = combine(_locations, _hoursBySlug, _error) { locations, hours, error ->
        UiState(
            loading = locations == null && error == null,
            hoursLoading = hours == null,
            items = (locations ?: emptyList())
                .map { LocationListItem(it, hours?.get(it.slug)?.let(::currentStatus)) }
                .sortedWith(locationComparator),
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState(loading = true))

    init {
        loadLocations()
    }

    fun loadLocations() {
        viewModelScope.launch {
            _locations.value = null
            _hoursBySlug.value = null
            _error.value = null
            coroutineScope {
                val locationsDeferred = async { runCatching { apiClient.listLocations() } }
                val hoursDeferred = async { runCatching { apiClient.listHours() } }
                val locationsResult = locationsDeferred.await()
                val hoursResult = hoursDeferred.await()
                val locations = locationsResult.getOrNull()?.locations
                if (locations == null) {
                    _locations.value = emptyList()
                    _hoursBySlug.value = emptyMap()
                    val error = locationsResult.exceptionOrNull()
                    if (error != null) {
                        logApiError(TAG, error)
                    }
                    _error.value = error?.let(::userMessageFor)
                        ?: "Something went wrong. Please try again."
                    return@coroutineScope
                }
                hoursResult.exceptionOrNull()?.let { logApiError(TAG, it) }
                val hoursBySlug = hoursResult.getOrNull()
                    ?.locations
                    ?.associate { it.slug to it.hours }
                _locations.value = locations
                // Hours are non-fatal: missing hours just render as "Hours unavailable".
                _hoursBySlug.value = hoursBySlug ?: emptyMap()
                prefetchOpenDiningHallMenus(locations, hoursBySlug)
            }
        }
    }

    // Warms the HTTP cache so a dining hall's menu list renders instantly when opened;
    // fresh responses are served from cache without a network round trip.
    private fun prefetchOpenDiningHallMenus(
        locations: List<Location>,
        hoursBySlug: Map<String, List<HoursRange>>?,
    ) {
        if (hoursBySlug == null) return
        locations
            .filter { it.type == DiningHallType }
            .filter { currentStatus(hoursBySlug[it.slug]).let(::isOpen) }
            .forEach { location ->
                viewModelScope.launch {
                    runCatching { apiClient.listMenus(location.id) }
                        .onFailure { logApiError(TAG, it) }
                }
            }
    }

    private fun isOpen(status: LocationStatus) =
        status is LocationStatus.Open || status is LocationStatus.ClosingSoon

    private companion object {
        const val TAG = "LocationListViewModel"
    }
}
