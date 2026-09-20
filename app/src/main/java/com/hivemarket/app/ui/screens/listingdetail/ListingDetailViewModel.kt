package com.hivemarket.app.ui.screens.listingdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.Listing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ListingDetailUiState {
    object Loading : ListingDetailUiState()
    data class Loaded(val listing: Listing) : ListingDetailUiState()
    data class Error(val message: String) : ListingDetailUiState()
}

sealed class ListingDetailEvent {
    data class OfferSubmitted(val success: Boolean) : ListingDetailEvent()
    data class ConversationReady(val conversation: Conversation) : ListingDetailEvent()
    data class ActionFailed(val message: String) : ListingDetailEvent()
}

/**
 * Backs Listing Detail. Takes listingID via SavedStateHandle (the nav-args
 * mechanism Hilt ViewModels use) rather than a constructor parameter, since
 * the ViewModel is created by Hilt's navigation-compose integration, not by
 * the screen directly — see NavGraph's listingDetail composable route.
 */
@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val repository: ListingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "ListingDetailViewModel"
    }

    private val listingID: Int = checkNotNull(savedStateHandle["listingID"])

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ListingDetailEvent?>(null)
    val events: StateFlow<ListingDetailEvent?> = _events.asStateFlow()

    init {
        Log.d(TAG, "Loading listing $listingID")
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ListingDetailUiState.Loading
            when (val result = repository.getListingDetail(listingID)) {
                is ApiResult.Success -> _uiState.value = ListingDetailUiState.Loaded(result.data)
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to load listing $listingID: ${result.message}")
                    _uiState.value = ListingDetailUiState.Error(result.message)
                }
            }
        }
    }

    fun makeOffer(amount: Double, message: String?) {
        Log.d(TAG, "Submitting offer of $amount on listing $listingID")
        viewModelScope.launch {
            when (val result = repository.makeOffer(listingID, amount, message)) {
                is ApiResult.Success -> _events.value = ListingDetailEvent.OfferSubmitted(success = true)
                is ApiResult.Error -> _events.value = ListingDetailEvent.ActionFailed(result.message)
            }
        }
    }

    fun openOrStartConversation() {
        Log.d(TAG, "Opening/starting conversation for listing $listingID")
        viewModelScope.launch {
            when (val result = repository.startOrGetConversation(listingID)) {
                is ApiResult.Success -> _events.value = ListingDetailEvent.ConversationReady(result.data)
                is ApiResult.Error -> _events.value = ListingDetailEvent.ActionFailed(result.message)
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
