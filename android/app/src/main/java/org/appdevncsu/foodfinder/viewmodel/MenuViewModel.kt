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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.SectionList
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import org.appdevncsu.foodfinder.data.userMessageFor
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val repository: ContentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val sections: SectionList? = null,
        val error: String? = null,
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var observedKey: Pair<Int, Int>? = null

    fun loadMenu(menuId: Int, locationId: Int) {
        val key = menuId to locationId
        if (observedKey != key) {
            observeJob?.cancel()
            observedKey = key
            observeJob = repository.observeSections(locationId, menuId)
                .distinctUntilChanged()
                .onEach { sections ->
                    _uiState.update {
                        it.copy(loading = sections == null && it.error == null, sections = sections)
                    }
                }
                .launchIn(viewModelScope)
        }
        refresh(menuId, locationId)
    }

    fun retry(menuId: Int, locationId: Int) = loadMenu(menuId, locationId)

    @Suppress("TooGenericExceptionCaught")
    private fun refresh(menuId: Int, locationId: Int) {
        viewModelScope.launch {
            try {
                repository.refreshSections(locationId, menuId)
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
        const val TAG = "MenuViewModel"
    }
}
