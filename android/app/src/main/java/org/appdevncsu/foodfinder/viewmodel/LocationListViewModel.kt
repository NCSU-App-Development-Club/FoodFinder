package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.LocationListItem
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.data.currentStatus
import javax.inject.Inject

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

    fun loadLocations() {
        viewModelScope.launch {
            coroutineScope {
                val locationsDeferred = async { apiClient.listLocations() }
                val hoursDeferred = async { runCatching { apiClient.listHours() }.getOrNull() }
                val hoursBySlug =
                    hoursDeferred.await()?.locations?.associate { it.slug to it.hours }
                _locations.update {
                    locationsDeferred.await().locations.map { location ->
                        LocationListItem(location, currentStatus(hoursBySlug?.get(location.slug)))
                    }.sortedWith(locationComparator)
                }
            }
        }
    }

    private val _locations: MutableStateFlow<List<LocationListItem>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<LocationListItem>> = _locations

    init {
        loadLocations()
    }
}
