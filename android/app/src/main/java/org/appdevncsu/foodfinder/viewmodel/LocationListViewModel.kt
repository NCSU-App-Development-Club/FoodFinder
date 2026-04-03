package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.Location
import javax.inject.Inject

@HiltViewModel
class LocationListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {

    fun loadLocations() {
        viewModelScope.launch {
            val response = apiClient.listLocations()
            _locations.update { response.locations }
        }
    }

    private val _locations: MutableStateFlow<List<Location>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<Location>> = _locations

    init {
        loadLocations()
    }
}
