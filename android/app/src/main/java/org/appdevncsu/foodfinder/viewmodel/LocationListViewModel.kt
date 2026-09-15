package org.appdevncsu.foodfinder.viewmodel

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.LocationListItem
import org.appdevncsu.foodfinder.data.LocationStatus
import org.appdevncsu.foodfinder.data.NotificationPreferences
import org.appdevncsu.foodfinder.data.currentStatus
import org.appdevncsu.foodfinder.data.logApiError
import org.appdevncsu.foodfinder.data.ncsuZone
import org.appdevncsu.foodfinder.data.repository.ContentRepository
import org.appdevncsu.foodfinder.data.repository.FavoritesRepository
import org.appdevncsu.foodfinder.data.userMessageFor
import java.time.LocalDate
import javax.inject.Inject

private val typeOrder = listOf("dining-halls", "food-courts", "restaurants", "cafes", "markets")

private val locationComparator =
    compareByDescending<LocationListItem> {
        it.status is LocationStatus.Open || it.status is LocationStatus.ClosingSoon
    }
        .thenBy { item ->
            item.location.type?.let(typeOrder::indexOf)?.takeIf { it >= 0 } ?: typeOrder.size
        }
        .thenBy { it.location.name }

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val favoritesRepository: FavoritesRepository,
    private val notificationPreferences: NotificationPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val hoursLoading: Boolean = true,
        val items: List<LocationListItem> = emptyList(),
        val error: String? = null,
        val showNotificationPrompt: Boolean = false,
    )

    private val _error = MutableStateFlow<String?>(null)

    // Bumped every time the app returns to the foreground so open/closed
    // statuses recompute against the current time even if the cache is unchanged.
    private val _clockTick = MutableStateFlow(0L)

    // True until the first refresh finishes, so fresh installs show hour skeletons
    // but an offline first run settles into "Hours unavailable" instead.
    private val _initialLoad = MutableStateFlow(true)

    private val _notificationsEnabled = MutableStateFlow(areNotificationsEnabled())

    private var refreshJob: Job? = null

    // The opt-in card shows when the user has a favorite, hasn't answered the
    // prompt yet, and notifications are off.
    private val showNotificationPrompt: Flow<Boolean> =
        combine(
            notificationPreferences.promptDismissed,
            _notificationsEnabled,
            favoritesRepository.observeNormalizedNames(),
        ) { dismissed, enabled, favorites -> !dismissed && !enabled && favorites.isNotEmpty() }

    val uiState: StateFlow<UiState> =
        combine(
            repository.observeHome(),
            _error,
            _clockTick,
            _initialLoad,
            showNotificationPrompt,
        ) { home, error, _, initialLoad, showPrompt ->
            val today = LocalDate.now(ncsuZone).toString()
            val todayHours = home.hoursByDate[today]
            UiState(
                loading = home.locations == null && error == null,
                hoursLoading = initialLoad && todayHours == null,
                items = (home.locations ?: emptyList())
                    .map { location ->
                        LocationListItem(
                            location,
                            todayHours?.get(location.slug)?.let {
                                currentStatus(it, unavailableHoursText())
                            },
                        )
                    }
                    .sortedWith(locationComparator),
                error = error,
                showNotificationPrompt = showPrompt,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState(loading = true))

    init {
        refresh(isRefresh = false)
    }

    fun loadLocations() {
        _error.value = null
        refresh(isRefresh = false)
    }

    /**
     * Called every time the app returns to the foreground. Bumps the clock so
     * open/closed badges recompute instantly, then refreshes from the network.
     */
    fun onForegrounded() {
        _clockTick.value += 1
        _notificationsEnabled.value = areNotificationsEnabled()
        if (_initialLoad.value) return
        refresh(isRefresh = true)
    }

    /** The user answered the notification opt-in card, so stop showing it. */
    fun onNotificationPromptAnswered() {
        _notificationsEnabled.value = areNotificationsEnabled()
        notificationPreferences.dismissPrompt()
    }

    private fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    @Suppress("TooGenericExceptionCaught")
    private fun refresh(isRefresh: Boolean) {
        if (isRefresh && refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshHome()
                _error.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logApiError(TAG, e)
                // A background refresh keeps showing whatever is cached.
                if (!isRefresh) {
                    _error.value = userMessageFor(e, context.resources)
                }
            } finally {
                _initialLoad.value = false
            }
        }
    }

    private fun unavailableHoursText(): String = context.getString(R.string.hours_unavailable)

    private companion object {
        const val TAG = "LocationListViewModel"
    }
}
