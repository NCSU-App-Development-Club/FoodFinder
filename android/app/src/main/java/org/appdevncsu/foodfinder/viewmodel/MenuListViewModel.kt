package org.appdevncsu.foodfinder.viewmodel
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.Menu
import org.appdevncsu.foodfinder.data.MenuList
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.mealOrder
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import org.appdevncsu.foodfinder.data.userMessageFor
import javax.inject.Inject

@HiltViewModel
class MenuListViewModel @Inject constructor(
    private val repository: ContentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val menuList: MenuList? = null,
        val error: String? = null,
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var observedLocationId: Int? = null

    fun loadMenusForLocation(locationId: Int) {
        if (observedLocationId != locationId) {
            observeJob?.cancel()
            observedLocationId = locationId
            observeJob = repository.observeMenus(locationId)
                .map { it?.let(::sortMenus) }
                .distinctUntilChanged()
                .onEach { menus ->
                    _uiState.update {
                        it.copy(loading = menus == null && it.error == null, menuList = menus)
                    }
                }
                .launchIn(viewModelScope)
        }
        refresh(locationId)
    }

    fun retry(locationId: Int) = loadMenusForLocation(locationId)

    @Suppress("TooGenericExceptionCaught")
    private fun refresh(locationId: Int) {
        viewModelScope.launch {
            try {
                repository.refreshMenus(locationId)
                _uiState.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logApiError(TAG, e)
                _uiState.update {
                    it.copy(loading = false, error = userMessageFor(e, context.resources))
                }
            }
        }
    }

    private companion object {
        const val TAG = "MenuListViewModel"

        fun sortMenus(menus: MenuList): MenuList {
            return menus.copy(
                menus = menus.menus.sortedWith(
                    compareBy(
                        { menu: Menu -> menu.date },
                        { menu: Menu -> mealOrder(menu.name) },
                        { menu: Menu -> menu.name.lowercase() },
                        { menu: Menu -> menu.id },
                    )
                )
            )
        }
    }
}
