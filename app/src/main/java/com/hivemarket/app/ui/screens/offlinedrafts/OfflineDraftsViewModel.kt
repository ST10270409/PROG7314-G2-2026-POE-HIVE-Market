package com.hivemarket.app.ui.screens.offlinedrafts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Listing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfflineDraftsUiState(
    val drafts: List<Listing> = emptyList(),
    val retryingClientId: String? = null,
    val lastRetryFailed: Boolean = false
)

/**
 * Backs Offline Drafts (FR4/FR9). Lists whatever ListingRepository still
 * has marked pendingSync = true, live — as WorkManager or a manual retry
 * clears a draft, it disappears from this list automatically via the Flow
 * from observePendingDrafts.
 *
 * There's no automatic background retry implemented yet (see
 * ListingRepository.retrySync's comment — WorkManager is a dependency but
 * no sync Worker class exists), so this screen's retry button is
 * currently the only way a stuck draft gets another sync attempt.
 */
@HiltViewModel
class OfflineDraftsViewModel @Inject constructor(
    private val repository: ListingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OfflineDraftsViewModel"
    }

    private val _uiState = MutableStateFlow(OfflineDraftsUiState())
    val uiState: StateFlow<OfflineDraftsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePendingDrafts().collect { drafts ->
                _uiState.value = _uiState.value.copy(drafts = drafts)
            }
        }
    }

    fun retry(clientId: String) {
        Log.d(TAG, "Retrying sync for $clientId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(retryingClientId = clientId, lastRetryFailed = false)
            when (val result = repository.retrySync(clientId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Retry succeeded for $clientId")
                    _uiState.value = _uiState.value.copy(retryingClientId = null, lastRetryFailed = false)
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Retry failed for $clientId: ${result.message}")
                    _uiState.value = _uiState.value.copy(retryingClientId = null, lastRetryFailed = true)
                }
            }
        }
    }
}
