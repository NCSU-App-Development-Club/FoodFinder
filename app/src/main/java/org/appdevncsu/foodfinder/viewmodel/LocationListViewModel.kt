package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.sampleLocations
import org.appdevncsu.foodfinder.service.APIService
import javax.inject.Inject

@HiltViewModel
class LocationListViewModel @Inject constructor(private val api: APIService) : ViewModel() {

    private val _locations: MutableStateFlow<List<DiningLocation>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<DiningLocation>> = _locations

    init {
        viewModelScope.launch {
            _locations.update { api.getLocations() }
        }
    }
}
