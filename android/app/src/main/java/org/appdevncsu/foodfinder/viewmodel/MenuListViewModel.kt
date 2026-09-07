package org.appdevncsu.foodfinder.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.userMessageFor
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(private val apiClient: APIClient) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val menuList: MenuList? = null,
        val error: String? = null,
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun loadMenusForLocation(locationId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val menus = apiClient.listMenus(locationId)
                _uiState.update { it.copy(loading = false, menuList = menus, error = null) }
                prefetchTodayMenus(menus)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logApiError(TAG, e)
                _uiState.update { it.copy(loading = false, error = userMessageFor(e)) }
            }
        }
    }

    fun retry(locationId: Int) = loadMenusForLocation(locationId)

    // Warms the HTTP cache so today's menus render instantly when opened
    private fun prefetchTodayMenus(menus: MenuList) {
        val today = LocalDate.now().toString()
        menus.menus
            .filter { it.date == today }
            .forEach { menu ->
                viewModelScope.launch {
                    runCatching { apiClient.listSection(menu.locationId, menu.id) }
                        .onFailure { logApiError(TAG, it) }
                }
            }
    }

    private companion object {
        const val TAG = "MenuListViewModel"
    }
}
