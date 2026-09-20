package com.hivemarket.app.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hivemarket.app.data.local.LocalSettings
import com.hivemarket.app.data.local.LocaleSwitcher
import com.hivemarket.app.data.local.SettingsStore
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.SettingsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: LocalSettings = LocalSettings(),
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false,
    val signedOut: Boolean = false
)

/**
 * Backs the Settings screen (FR2). Every change is applied in two steps,
 * same offline-first shape as ListingRepository's writes:
 *   1. Written to SettingsStore and reflected in the UI immediately —
 *      the user never waits on a network round-trip to see their tap register.
 *   2. Pushed to PATCH /api/settings in the background. If that fails
 *      (offline, server down), the local value still stands; syncFailed
 *      just surfaces that the server copy may be stale until the next
 *      successful save.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val api: HiveMarketApi,
    private val firebaseAuth: FirebaseAuth,
    private val localeSwitcher: LocaleSwitcher
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState(settings = settingsStore.read()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setLanguage(language: String) {
        Log.d(TAG, "Language changed to $language")
        // Actually switches the app's locale at runtime (FR6) — this is
        // what makes it a real language switch rather than just a stored
        // preference nobody reads. Routed through LocaleSwitcher rather
        // than calling AppCompatDelegate directly so this method stays
        // unit-testable without Robolectric (see LocaleSwitcher.kt).
        localeSwitcher.apply(language)
        applyAndSync(_uiState.value.settings.copy(language = language))
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        Log.d(TAG, "Notifications toggled: $enabled")
        applyAndSync(_uiState.value.settings.copy(notificationsEnabled = enabled))
    }

    fun setBiometricEnabled(enabled: Boolean) {
        Log.d(TAG, "Biometric login toggled: $enabled")
        applyAndSync(_uiState.value.settings.copy(biometricEnabled = enabled))
    }

    fun signOut() {
        Log.i(TAG, "Signing out")
        firebaseAuth.signOut()
        _uiState.value = _uiState.value.copy(signedOut = true)
    }

    private fun applyAndSync(updated: LocalSettings) {
        // Step 1: local write + immediate UI update.
        settingsStore.write(updated)
        _uiState.value = _uiState.value.copy(settings = updated, isSyncing = true, syncFailed = false)

        // Step 2: best-effort push to the API.
        viewModelScope.launch {
            try {
                val response = api.updateSettings(
                    SettingsRequest(
                        language = updated.language,
                        notificationsEnabled = updated.notificationsEnabled,
                        biometricEnabled = updated.biometricEnabled
                    )
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "Settings synced to server")
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                } else {
                    Log.w(TAG, "Settings sync rejected by server: ${response.code()}")
                    _uiState.value = _uiState.value.copy(isSyncing = false, syncFailed = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Settings sync failed (likely offline): ${e.message}")
                _uiState.value = _uiState.value.copy(isSyncing = false, syncFailed = true)
            }
        }
    }
}
