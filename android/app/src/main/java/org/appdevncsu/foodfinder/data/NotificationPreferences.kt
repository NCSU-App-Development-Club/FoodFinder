package org.appdevncsu.foodfinder.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Remembers whether the user has already answered the notification opt-in prompt,
 * so the card on the location list only appears until they make a choice.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _promptDismissed = MutableStateFlow(prefs.getBoolean(KEY_PROMPT_DISMISSED, false))
    val promptDismissed: StateFlow<Boolean> = _promptDismissed.asStateFlow()

    fun dismissPrompt() {
        if (_promptDismissed.value) return
        prefs.edit {
            putBoolean(KEY_PROMPT_DISMISSED, true)
        }
        _promptDismissed.value = true
    }

    private companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_PROMPT_DISMISSED = "prompt_dismissed"
    }
}
