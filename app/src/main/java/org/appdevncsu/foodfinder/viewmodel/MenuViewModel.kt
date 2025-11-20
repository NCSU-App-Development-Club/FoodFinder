package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.DiningMenuSection
import org.appdevncsu.foodfinder.service.APIService
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(private val api: APIService) : ViewModel() {

    fun loadMenu(menuId: Int) {
        viewModelScope.launch {
            _loading.update { true }
            _sections.update {
                api.getMenu(menuId)
            }
            _loading.update { false }
        }
    }

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val _sections: MutableStateFlow<List<DiningMenuSection>> = MutableStateFlow(emptyList())

    val sections: StateFlow<List<DiningMenuSection>> = _sections
    val loading: StateFlow<Boolean> = _loading
}
