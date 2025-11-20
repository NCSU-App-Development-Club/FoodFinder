package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.service.APIService
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(private val api: APIService) : ViewModel() {

    fun loadMenusForLocation(locationId: Int) {
        viewModelScope.launch {
            _loading.update { true }
            _menuList.update {
                api.getMenus(locationId)
            }
            _loading.update { false }
        }
    }

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val _menuList: MutableStateFlow<List<DiningMenuListItem>> =
        MutableStateFlow(emptyList())

    val menuList: StateFlow<List<DiningMenuListItem>> = _menuList
    val loading: StateFlow<Boolean> = _loading
}
