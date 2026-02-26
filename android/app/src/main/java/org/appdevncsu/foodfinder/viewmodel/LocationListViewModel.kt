package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.sampleLocations
import javax.inject.Inject

@HiltViewModel
class LocationListViewModel @Inject constructor(apiClient: APIClient) : ViewModel() {
    private val _locations: MutableStateFlow<List<DiningLocation>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<DiningLocation>> = _locations

    init {
        _locations.update { sampleLocations }
    }
}
