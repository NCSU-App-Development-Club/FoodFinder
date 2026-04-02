package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.DiningLocation
import org.appdevncsu.foodfinder.data.Location
import org.appdevncsu.foodfinder.data.sampleLocations
import javax.inject.Inject

@HiltViewModel
class LocationListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {

    fun loadLocations(locationId: Int) {
        viewModelScope.launch {
            try {
                val locationsR = apiClient.listLocations().execute().body()
                _locations.value = locationsR?.locations?: emptyList()

            } catch (e: Exception) {
            }
        }
    }

    private val _locations: MutableStateFlow<List<Location>> = MutableStateFlow(emptyList())
    val locations: StateFlow<List<Location>> = _locations

}
