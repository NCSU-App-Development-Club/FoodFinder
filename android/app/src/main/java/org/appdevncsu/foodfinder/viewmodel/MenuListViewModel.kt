package org.appdevncsu.foodfinder.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.MenuList
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {
    fun loadMenusForLocation(locationId: Int) {
        viewModelScope.launch {
            val menus = apiClient.listMenus(locationId)
            _menuList.update { menus }
            prefetchTodayMenus(menus)
        }
    }

    // Warms the HTTP cache so today's menus render instantly when opened
    private fun prefetchTodayMenus(menus: MenuList) {
        val today = LocalDate.now().toString()
        menus.menus
            .filter { it.date == today }
            .forEach { menu ->
                viewModelScope.launch {
                    runCatching { apiClient.listSection(menu.locationId, menu.id) }
                }
            }
    }

    private val _menuList: MutableStateFlow<MenuList?> = MutableStateFlow(null)

    val menuList: StateFlow<MenuList?> = _menuList
}
