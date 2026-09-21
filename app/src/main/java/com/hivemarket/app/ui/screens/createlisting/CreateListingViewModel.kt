package com.hivemarket.app.ui.screens.createlisting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.local.SyncScheduler
import com.hivemarket.app.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateListingUiState(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val categoryID: Int = 1,
    val condition: String = "",
    val isSubmitting: Boolean = false,
    val submittedClientId: String? = null,
    val errorMessage: String? = null
)

/**
 * Backs Create Listing — the primary offline-capable screen (FR4/FR9).
 * All the offline-first behaviour (write locally first, sync immediately
 * if online, and schedule a real WorkManager retry constrained on
 * connectivity if that immediate attempt fails or the device is offline)
 * lives in ListingRepository.createListingOfflineFirst plus
 * SyncScheduler; this ViewModel is mostly form-state management and
 * validation on top of it.
 *
 * `sellerID` comes from the currently signed-in Firebase user rather than
 * a hardcoded value — see the TODO on how that ID maps to the backend's
 * integer userID once Luke's real API assigns one.
 */
@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val repository: ListingRepository,
    private val firebaseAuth: FirebaseAuth,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "CreateListingViewModel"
    }

    private val _uiState = MutableStateFlow(CreateListingUiState())
    val uiState: StateFlow<CreateListingUiState> = _uiState.asStateFlow()

    fun setTitle(value: String) { _uiState.value = _uiState.value.copy(title = value, errorMessage = null) }
    fun setDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun setPrice(value: String) { _uiState.value = _uiState.value.copy(price = value, errorMessage = null) }
    fun setCategory(categoryID: Int) { _uiState.value = _uiState.value.copy(categoryID = categoryID) }
    fun setCondition(value: String) { _uiState.value = _uiState.value.copy(condition = value) }

    fun submit() {
        val state = _uiState.value
        val price = state.price.toDoubleOrNull()

        if (state.title.isBlank() || price == null) {
            _uiState.value = state.copy(errorMessage = "Add a title and a valid price before submitting.")
            return
        }

        // TODO: the backend's Listing.sellerID is an int per the Data Models
        // table (Section 7), but Firebase's UID is a string — this needs a
        // real mapping once Luke's /api/auth/firebase exchange endpoint is
        // live and returns the backend's integer userID for the signed-in
        // user. Hashing as a placeholder keeps this screen functional for
        // demo purposes without pretending the mapping is solved for real.
        val sellerID = firebaseAuth.currentUser?.uid?.hashCode() ?: 0

        Log.i(TAG, "Submitting listing '${state.title}' offline-first")
        _uiState.value = state.copy(isSubmitting = true)

        viewModelScope.launch {
            val clientId = repository.createListingOfflineFirst(
                title = state.title,
                description = state.description,
                categoryID = state.categoryID,
                price = price,
                condition = state.condition.ifBlank { null },
                sellerID = sellerID
            )
            Log.d(TAG, "Listing written locally as $clientId (pendingSync until confirmed)")

            // Schedules a real, connectivity-constrained sync attempt via
            // WorkManager — this is what makes Offline Drafts actually
            // auto-sync rather than relying solely on the immediate
            // attempt inside createListingOfflineFirst (which does nothing
            // useful while offline) or the manual Retry button on the
            // Offline Drafts screen.
            syncScheduler.scheduleSync()

            _uiState.value = CreateListingUiState(submittedClientId = clientId)
        }
    }

    fun consumeSubmission() {
        _uiState.value = _uiState.value.copy(submittedClientId = null)
    }
}