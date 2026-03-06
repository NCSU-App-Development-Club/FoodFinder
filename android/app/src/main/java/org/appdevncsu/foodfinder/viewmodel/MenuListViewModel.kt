package org.appdevncsu.foodfinder.viewmodel
import org.appdevncsu.foodfinder.data.Menu
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.DiningMenuListItem
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.data.sampleMenuListItems
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {
    fun loadMenusForLocation(locationId: Int) {
        _menuList.update { apiClient.listMenus(locationId).execute().body() }
    }



    private val _menuList: MutableStateFlow<MenuList?> = MutableStateFlow(null)

    val menuList: StateFlow<MenuList?> = _menuList

    init {

    }
}
