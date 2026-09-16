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
import org.appdevncsu.foodfinder.data.ItemHistory
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.normalizeFavoriteName
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import org.appdevncsu.foodfinder.data.userMessageFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ItemHistoryViewModel @Inject constructor(
    private val repository: ContentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class History(
        val seenDates: Set<LocalDate>,
        val firstSeen: LocalDate?,
        val frequencyPerWeek: Double,
    )

    data class UiState(
        val loading: Boolean = true,
        val history: History? = null,
        val error: String? = null,
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var observedKey: Triple<Int, String, String>? = null

    fun load(locationId: Int, itemName: String, date: String) {
        val key = Triple(locationId, normalizeFavoriteName(itemName), date)
        if (observedKey != key) {
            observeJob?.cancel()
            observedKey = key
            observeJob = repository.observeItemHistory(locationId, itemName, date)
                .distinctUntilChanged()
                .onEach { history ->
                    _uiState.update {
                        it.copy(
                            loading = history == null && it.error == null,
                            history = history?.toUiHistory(),
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
        refresh(locationId, itemName, date)
    }

    fun retry(locationId: Int, itemName: String, date: String) = load(locationId, itemName, date)

    @Suppress("TooGenericExceptionCaught")
    private fun refresh(locationId: Int, itemName: String, date: String) {
        viewModelScope.launch {
            try {
                repository.refreshItemHistory(locationId, itemName, date)
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

    private fun ItemHistory.toUiHistory(): History = History(
        seenDates = dates.mapNotNull(::parseIsoDate).toSet(),
        firstSeen = firstSeen?.let(::parseIsoDate),
        frequencyPerWeek = frequencyPerWeek,
    )

    private fun parseIsoDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()

    private companion object {
        const val TAG = "ItemHistoryViewModel"
    }
}
