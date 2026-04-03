package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hilt_aggregated_deps._org_appdevncsu_foodfinder_data_HiltWrapper_APIClientModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.Section
import org.appdevncsu.foodfinder.data.SectionList

import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {
    fun loadMenu(menuId: Int) {
        viewModelScope.launch {
            val sections = apiClient.listSection(0, menuId)
            _sections.update { sections }
        }
    }

    private val _sections: MutableStateFlow<SectionList?> = MutableStateFlow(null)

    val sections: StateFlow<SectionList?> = _sections
}
