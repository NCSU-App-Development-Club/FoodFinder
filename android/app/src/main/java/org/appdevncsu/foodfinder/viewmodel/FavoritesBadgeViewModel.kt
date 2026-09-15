package org.appdevncsu.foodfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.repository.FavoritesRepository
import javax.inject.Inject

/** Backs the top-bar favorites shortcut: how many favorite items are being served today. */
@HiltViewModel
class FavoritesBadgeViewModel @Inject constructor(
    private val repository: FavoritesRepository,
) : ViewModel() {

    val count: StateFlow<Int> = repository.observeTodayMatches()
        .map { response -> response?.matches.orEmpty().flatMap { it.items }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Refetches today's matches; the cache is invalidated whenever the favorites change. */
    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        viewModelScope.launch {
            try {
                repository.refreshMatches()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logApiError(TAG, e)
            }
        }
    }

    private companion object {
        const val TAG = "FavoritesBadgeViewModel"
    }
}
