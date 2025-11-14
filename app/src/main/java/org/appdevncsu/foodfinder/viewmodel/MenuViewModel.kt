package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.DiningMenuSection
import org.appdevncsu.foodfinder.data.sampleMenuListItems

class MenuViewModel(private val menuId: Int) : ViewModel() {
    private val _sections: MutableStateFlow<List<DiningMenuSection>> = MutableStateFlow(emptyList())
    val sections: StateFlow<List<DiningMenuSection>> = _sections

    init {
        val menu = sampleMenuListItems.find { it.menuId == menuId }
        _sections.update { menu?.menu ?: emptyList() }
    }
}