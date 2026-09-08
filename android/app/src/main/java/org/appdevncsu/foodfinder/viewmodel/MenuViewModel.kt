package org.appdevncsu.foodfinder.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.APIClient
import org.appdevncsu.foodfinder.data.SectionList
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.userMessageFor
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val apiClient: APIClient,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val sections: SectionList? = null,
        val error: String? = null,
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun loadMenu(menuId: Int, locationId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val sections = apiClient.listSection(locationId, menuId)
                _uiState.update { it.copy(loading = false, sections = sections, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logApiError(TAG, e)
                _uiState.update { it.copy(loading = false, error = userMessageFor(e, context.resources)) }
            }
        }
    }

    fun retry(menuId: Int, locationId: Int) = loadMenu(menuId, locationId)

    private companion object {
        const val TAG = "MenuViewModel"
    }
}
