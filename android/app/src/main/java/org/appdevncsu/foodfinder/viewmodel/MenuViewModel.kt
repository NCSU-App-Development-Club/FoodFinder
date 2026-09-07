package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.SectionList
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {
    fun loadMenu(menuId: Int, locationId: Int) {
        viewModelScope.launch {
            val sections = apiClient.listSection(locationId, menuId)
            _sections.update { sections }
        }
    }

    private val _sections: MutableStateFlow<SectionList?> = MutableStateFlow(null)

    val sections: StateFlow<SectionList?> = _sections
}
