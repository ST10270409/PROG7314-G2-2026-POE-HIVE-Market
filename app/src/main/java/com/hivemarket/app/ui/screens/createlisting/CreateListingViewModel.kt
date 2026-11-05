
package com.hivemarket.app.ui.screens.createlisting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.local.SyncScheduler
import com.hivemarket.app.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
 * ViewModel for the Create Listing screen.
 *
 * Implements offline-first listing submission by:
 * 1. Validating user input.
 * 2. Saving listings locally through RoomDB.
 * 3. Scheduling background synchronisation using WorkManager.
 * 4. Handling database and synchronisation errors.
 *
 * Listings are stored locally before synchronisation is attempted,
 * ensuring that user data is not lost when internet connectivity
 * is unavailable.
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

    // Update listing form fields.
    fun setTitle(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value,
            errorMessage = null
        )
    }

    fun setDescription(value: String) {
        _uiState.value = _uiState.value.copy(
            description = value
        )
    }

    fun setPrice(value: String) {
        _uiState.value = _uiState.value.copy(
            price = value,
            errorMessage = null
        )
    }

    fun setCategory(categoryID: Int) {
        _uiState.value = _uiState.value.copy(
            categoryID = categoryID
        )
    }

    fun setCondition(value: String) {
        _uiState.value = _uiState.value.copy(
            condition = value
        )
    }

    /**
     * Validates and saves a listing using an offline-first approach.
     */
    fun submit() {

        val state = _uiState.value

        // Prevent duplicate submissions.
        if (state.isSubmitting) {
            Log.w(TAG, "Submission already in progress.")
            return
        }

        // Validate required fields.
        val price = state.price.toDoubleOrNull()

        if (state.title.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = "Please enter a listing title."
            )
            return
        }

        if (price == null || !price.isFinite() || price <= 0.0) {
            _uiState.value = state.copy(
                errorMessage = "Please enter a valid price greater than zero."
            )
            return
        }

        // Obtain the signed-in Firebase user's identifier.
        // The backend user ID mapping remains a placeholder until
        // the Firebase authentication exchange endpoint is available.
        val sellerID = firebaseAuth.currentUser?.uid?.hashCode() ?: 0

        Log.i(TAG, "Starting offline-first submission: ${state.title}")

        _uiState.value = state.copy(
            isSubmitting = true,
            errorMessage = null
        )

        viewModelScope.launch {

            val clientId: String

            try {
                // STEP 1: Save the listing locally using RoomDB.
                // This happens before attempting server synchronisation.
                clientId = repository.createListingOfflineFirst(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    categoryID = state.categoryID,
                    price = price,
                    condition = state.condition.ifBlank { null },
                    sellerID = sellerID
                )

                Log.i(
                    TAG,
                    "Listing successfully saved locally. Client ID: $clientId"
                )

            } catch (e: CancellationException) {
                // Preserve coroutine cancellation behaviour.
                throw e

            } catch (e: Exception) {

                // Handle local database or repository errors.
                Log.e(
                    TAG,
                    "Failed to save listing in local database.",
                    e
                )

                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Unable to save your listing. Please try again."
                )

                return@launch
            }

            // STEP 2: Schedule background synchronisation.
            // The listing is already safely stored locally.
            var syncSchedulingFailed = false

            try {
                syncScheduler.scheduleSync()

                Log.i(
                    TAG,
                    "Background listing synchronisation scheduled."
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                // Do not treat a successful local save as a failed save
                // just because background scheduling encountered an error.
                syncSchedulingFailed = true

                Log.e(
                    TAG,
                    "Listing saved locally, but synchronisation scheduling failed.",
                    e
                )
            }

            // STEP 3: Update the UI to confirm local storage succeeded.
            _uiState.value = CreateListingUiState(
                submittedClientId = clientId,
                errorMessage = if (syncSchedulingFailed) {
                    "Your listing was saved on this device, but automatic synchronisation could not be scheduled."
                } else {
                    null
                }
            )

            Log.i(
                TAG,
                "Listing submission completed. Local ID: $clientId"
            )
        }
    }

    /**
     * Clears the successful submission identifier after
     * the screen has processed the submission.
     */
    fun consumeSubmission() {
        _uiState.value = _uiState.value.copy(
            submittedClientId = null
        )
    }
}
