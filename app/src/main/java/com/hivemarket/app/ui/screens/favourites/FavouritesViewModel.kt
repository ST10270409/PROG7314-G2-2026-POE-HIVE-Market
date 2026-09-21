package com.hivemarket.app.ui.screens.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.local.FavouriteEntity
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Listing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: ListingRepository
) : ViewModel() {

    // Automatically observes Room DB changes and emits to the UI
    val favourites: StateFlow<List<FavouriteEntity>> = repository.getFavouritesFromDb()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFavourite(favourite: FavouriteEntity, currentUserId: String) {
        viewModelScope.launch {
            // Map FavouriteEntity back to domain Listing to invoke toggle
            val listing = Listing(
                listingID = favourite.listingId.toIntOrNull() ?: 0,
                title = favourite.title,
                description = "",
                categoryID = 0,
                price = favourite.price,
                condition = null,
                image = favourite.imageUrl,
                datePosted = "",
                sellerID = 0,
                status = "Active",
                sellerName = null,
                sellerTrustScore = null,
                pendingSync = false,
                clientId = favourite.listingId
            )
            repository.toggleFavourite(listing, currentUserId)
        }
    }

    fun syncFromRemote(userId: String) {
        viewModelScope.launch {
            repository.syncFavouritesFromRemote(userId)
        }
    }
}