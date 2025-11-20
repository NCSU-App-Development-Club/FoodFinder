package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.data.sampleMenuListItems

class MenuListViewModel : ViewModel() {
    private val _menuList: MutableStateFlow<List<DiningMenuListItem>> = MutableStateFlow(emptyList())

    val locations: StateFlow<List<DiningMenuListItem>> = _menuList

    init {
        _menuList.update { sampleMenuListItems }
    }
}