package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.DiningMenuSection

import org.appdevncsu.foodfinder.data.sampleMenuSections

class MenuViewModel : ViewModel() {
    fun loadMenu(menuId: Int) {
        TODO("Not yet implemented")
    }

    private val _sections: MutableStateFlow<List<DiningMenuSection>> = MutableStateFlow(emptyList())

    val sections: StateFlow<List<DiningMenuSection>> = _sections

    init {
        _sections.update { sampleMenuSections }
    }
}
