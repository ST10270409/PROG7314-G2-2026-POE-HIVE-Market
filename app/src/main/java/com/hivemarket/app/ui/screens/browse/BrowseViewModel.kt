package com.hivemarket.app.ui.screens.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Listing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Category { ALL, TEXTBOOK, ELECTRONICS, FURNITURE }

data class BrowseUiState(
    val listings: List<Listing> = emptyList(),
    val selectedCategory: Category = Category.ALL,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Backs the Browse screen (Home tab). Follows the offline-first pattern
 * from the Structural Plan: [ListingRepository.observeCachedListings] is
 * the source of truth for what's on screen, and [refresh] is just a
 * best-effort attempt to update that cache from the API — a failed
 * refresh degrades to "isOffline = true" rather than an empty screen.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: ListingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BrowseViewModel"
    }

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "init — observing cached listings and triggering first refresh")
        viewModelScope.launch {
            repository.observeCachedListings().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ).collect { cached ->
                _uiState.value = _uiState.value.copy(listings = applyCategoryFilter(cached, _uiState.value.selectedCategory))
            }
        }
        refresh()
    }

    fun selectCategory(category: Category) {
        Log.d(TAG, "Category filter changed: $category")
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        refresh()
    }

    fun refresh() {
        val categoryId = categoryToId(_uiState.value.selectedCategory)
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.refreshListings(categoryID = categoryId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Refresh succeeded")
                    _uiState.value = _uiState.value.copy(isRefreshing = false, isOffline = false)
                }
                is ApiResult.Error -> {
                    // Cached listings (from the Flow collector above) still
                    // render — this just surfaces that they may be stale.
                    Log.w(TAG, "Refresh failed, falling back to cache: ${result.message}")
                    _uiState.value = _uiState.value.copy(isRefreshing = false, isOffline = true)
                }
            }
        }
    }

    private fun applyCategoryFilter(listings: List<Listing>, category: Category): List<Listing> {
        val categoryId = categoryToId(category) ?: return listings
        return listings.filter { it.categoryID == categoryId }
    }

    // TODO: replace with real IDs once Luke's Category table is seeded —
    // these are placeholders matching the seed values suggested in the
    // Planning and Design document's Data Models table (Section 7).
    private fun categoryToId(category: Category): Int? = when (category) {
        Category.ALL -> null
        Category.TEXTBOOK -> 1
        Category.ELECTRONICS -> 2
        Category.FURNITURE -> 3
    }
}
