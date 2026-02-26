package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.data.sampleMenuListItems
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(apiClient: APIClient) : ViewModel() {
    fun loadMenusForLocation(locationId: Int) {
        TODO("Not yet implemented")
    }

    private val _menuList: MutableStateFlow<List<DiningMenuListItem>> = MutableStateFlow(emptyList())

    val menuList: StateFlow<List<DiningMenuListItem>> = _menuList

    init {
        _menuList.update { sampleMenuListItems }
    }
}
