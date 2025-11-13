package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.sampleLocations

class LocationListViewModel : ViewModel() {
    private val _locations: MutableStateFlow<List<DiningLocation>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<DiningLocation>> = _locations

    init {
        _locations.update { sampleLocations }
    }
}
