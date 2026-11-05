
package com.hivemarket.app.data.repository

import android.util.Log
import com.hivemarket.app.data.local.FavouriteDao
import com.hivemarket.app.data.local.FavouriteEntity
import com.hivemarket.app.data.local.ListingDao
import com.hivemarket.app.data.local.ListingEntity
import com.hivemarket.app.data.remote.CreateListingRequest
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.MakeOfferRequest
import com.hivemarket.app.data.remote.StartConversationRequest
import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.FavouriteRequest
import com.hivemarket.app.domain.Listing
import com.hivemarket.app.domain.Offer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

@Singleton
class ListingRepository @Inject constructor(
    private val api: HiveMarketApi,
    private val dao: ListingDao,
    private val favouriteDao: FavouriteDao
) {

    companion object {
        private const val TAG = "ListingRepository"
    }

    // ---------------------------------------------------------
    // LOCAL LISTING STORAGE AND OBSERVATION
    // ---------------------------------------------------------

    /**
     * Observes listings stored in RoomDB.
     * Cached listings remain accessible when internet is unavailable.
     */
    fun observeCachedListings(): Flow<List<Listing>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Observes listings that have not yet synchronised with the server.
     */
    fun observePendingDrafts(): Flow<List<Listing>> =
        dao.observePendingSync().map { entities ->
            entities.map { it.toDomain() }
        }

    // ---------------------------------------------------------
    // OFFLINE LISTING SYNCHRONISATION
    // ---------------------------------------------------------

    /**
     * Retries synchronisation for an individual pending listing.
     */
    suspend fun retrySync(clientId: String): ApiResult<Unit> {
        return try {
            val entity = dao.getPendingSync()
                .firstOrNull { it.clientId == clientId }
                ?: return ApiResult.Error("Draft not found.")

            syncOneEntity(entity)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry listing synchronisation: $clientId", e)
            ApiResult.Error(
                e.message ?: "Unable to retry listing synchronisation."
            )
        }
    }

    /**
     * Synchronises all pending listings stored in RoomDB.
     *
     * Returns the number of listings that failed to synchronise.
     * Failed listings remain stored locally for future retries.
     */
    suspend fun syncAllPendingDrafts(): Int {
        val pending = try {
            dao.getPendingSync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve pending listings from RoomDB.", e)
            return 1
        }

        var failures = 0

        Log.i(TAG, "Starting synchronisation of ${pending.size} pending listings.")

        for (entity in pending) {
            when (syncOneEntity(entity)) {
                is ApiResult.Success -> {
                    Log.i(TAG, "Synchronised listing: ${entity.clientId}")
                }

                is ApiResult.Error -> {
                    failures++
                    Log.w(TAG, "Listing remains pending: ${entity.clientId}")
                }
            }
        }

        Log.i(TAG, "Synchronisation finished. Failed listings: $failures")

        return failures
    }

    /**
     * Sends an existing locally stored listing to the API.
     *
     * A listing is only marked as synchronised when the server
     * returns a successful response containing a valid listing ID.
     */
    private suspend fun syncOneEntity(
        entity: ListingEntity
    ): ApiResult<Unit> {

        return try {

            Log.i(TAG, "Synchronising listing: ${entity.clientId}")

            val response = api.createListing(
                CreateListingRequest(
                    clientId = entity.clientId,
                    title = entity.title,
                    description = entity.description,
                    price = entity.price,
                    categoryID = entity.categoryID,
                    condition = entity.condition
                )
            )

            if (response.isSuccessful) {

                val serverListing = response.body()

                if (serverListing == null || serverListing.listingID <= 0) {

                    Log.e(
                        TAG,
                        "Invalid server response for listing ${entity.clientId}"
                    )

                    return ApiResult.Error(
                        "The server returned an invalid response. Your draft remains saved locally."
                    )
                }

                // Update RoomDB only after server confirmation.
                dao.markSynced(
                    entity.clientId,
                    serverListing.listingID
                )

                Log.i(
                    TAG,
                    "Listing successfully synchronised: ${entity.clientId}"
                )

                ApiResult.Success(Unit)

            } else {

                Log.w(
                    TAG,
                    "Server rejected listing ${entity.clientId}. HTTP ${response.code()}"
                )

                ApiResult.Error(
                    "Server returned HTTP ${response.code()}. Your draft remains saved locally."
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Synchronisation failed for listing ${entity.clientId}",
                e
            )

            ApiResult.Error(
                e.message ?: "Network or database error. Your draft remains saved locally."
            )
        }
    }

    // ---------------------------------------------------------
    // LISTING DETAILS AND OFFLINE FALLBACK
    // ---------------------------------------------------------

    /**
     * Retrieves listing details from the server.
     * Uses RoomDB as a fallback when the server cannot be reached.
     */
    suspend fun getListingDetail(listingID: Int): ApiResult<Listing> {

        return try {

            val response = api.getListing(listingID)

            if (response.isSuccessful && response.body() != null) {

                val listing = response.body()!!

                dao.upsert(
                    listing.toEntity(pendingSync = false)
                )

                Log.i(TAG, "Listing $listingID retrieved and cached.")

                ApiResult.Success(listing)

            } else {

                Log.w(
                    TAG,
                    "GET /api/listings/$listingID failed: ${response.code()}"
                )

                fallBackToCache(listingID)
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Unable to retrieve listing $listingID. Trying local cache.",
                e
            )

            fallBackToCache(listingID)
        }
    }

    /**
     * Retrieves an existing listing from RoomDB.
     */
    private suspend fun fallBackToCache(
        listingID: Int
    ): ApiResult<Listing> {

        return try {

            val cached = dao.getById(listingID)

            if (cached != null) {

                Log.i(TAG, "Serving listing $listingID from RoomDB.")

                ApiResult.Success(cached.toDomain())

            } else {

                Log.w(TAG, "Listing $listingID is not available offline.")

                ApiResult.Error("Listing not available offline.")
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(TAG, "Failed to retrieve cached listing $listingID.", e)

            ApiResult.Error(
                "Unable to retrieve listing from local storage."
            )
        }
    }

    // ---------------------------------------------------------
    // OFFERS
    // ---------------------------------------------------------

    suspend fun makeOffer(
        listingID: Int,
        amount: Double,
        message: String?
    ): ApiResult<Offer> {

        return try {

            val response = api.makeOffer(
                listingID,
                MakeOfferRequest(
                    amount = amount,
                    message = message
                )
            )

            if (response.isSuccessful && response.body() != null) {

                ApiResult.Success(response.body()!!)

            } else {

                Log.w(TAG, "Offer request failed: HTTP ${response.code()}")

                ApiResult.Error("Server returned HTTP ${response.code()}.")
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(TAG, "Unable to submit offer.", e)

            ApiResult.Error(
                e.message ?: "Network error while submitting offer."
            )
        }
    }

    // ---------------------------------------------------------
    // CONVERSATIONS
    // ---------------------------------------------------------

    suspend fun startOrGetConversation(
        listingID: Int
    ): ApiResult<Conversation> {

        return try {

            val existing = api.getConversations()

            val match = existing.body()
                ?.items
                ?.firstOrNull { it.listingID == listingID }

            if (match != null) {
                return ApiResult.Success(match)
            }

            val response = api.startConversation(
                StartConversationRequest(listingID)
            )

            if (response.isSuccessful && response.body() != null) {

                ApiResult.Success(response.body()!!)

            } else {

                ApiResult.Error(
                    "Unable to start conversation. HTTP ${response.code()}."
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(TAG, "Unable to start or retrieve conversation.", e)

            ApiResult.Error(
                e.message ?: "Network error while starting conversation."
            )
        }
    }

    // ---------------------------------------------------------
    // MARKETPLACE CACHE REFRESH
    // ---------------------------------------------------------

    /**
     * Downloads marketplace listings and stores them locally.
     *
     * If the API is unavailable, existing cached listings
     * remain available through observeCachedListings().
     */
    suspend fun refreshListings(
        categoryID: Int? = null,
        query: String? = null
    ): ApiResult<Unit> {

        return try {

            val response = api.getListings(
                query = query,
                categoryID = categoryID
            )

            if (response.isSuccessful) {

                val listings = response.body()?.items.orEmpty()

                Log.i(
                    TAG,
                    "Fetched ${listings.size} listings from API."
                )

                // Save retrieved marketplace listings in RoomDB.
                dao.upsertAll(
                    listings.map { it.toEntity(pendingSync = false) }
                )

                Log.i(TAG, "Marketplace cache successfully updated.")

                ApiResult.Success(Unit)

            } else {

                Log.w(
                    TAG,
                    "Marketplace refresh failed: HTTP ${response.code()}"
                )

                ApiResult.Error(
                    "Unable to refresh marketplace. Cached listings remain available."
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Marketplace refresh failed. Existing cache retained.",
                e
            )

            ApiResult.Error(
                "Unable to refresh marketplace. Please check your connection."
            )
        }
    }

    // ---------------------------------------------------------
    // OFFLINE-FIRST LISTING CREATION
    // ---------------------------------------------------------

    /**
     * Saves a new listing in RoomDB before attempting network access.
     *
     * The listing is initially marked as pendingSync = true.
     * This ensures that the user's listing is preserved even when
     * the device has no internet connection.
     */
    suspend fun createListingOfflineFirst(
        title: String,
        description: String,
        categoryID: Int,
        price: Double,
        condition: String?,
        sellerID: Int
    ): String {

        require(title.isNotBlank()) {
            "Listing title cannot be empty."
        }

        require(price.isFinite() && price > 0.0) {
            "Listing price must be greater than zero."
        }

        val clientId = UUID.randomUUID().toString()

        val entity = ListingEntity(
            clientId = clientId,
            title = title,
            description = description,
            categoryID = categoryID,
            price = price,
            condition = condition,
            sellerID = sellerID,
            status = "Active",
            pendingSync = true
        )

        try {

            // STEP 1: Persist the listing locally.
            // This must succeed before any network request is attempted.
            dao.upsert(entity)

            Log.i(
                TAG,
                "Listing saved in RoomDB. Pending synchronisation: $clientId"
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "RoomDB failed to save listing locally.",
                e
            )

            throw IllegalStateException(
                "Unable to save listing locally. Please try again.",
                e
            )
        }

        // STEP 2: Attempt immediate synchronisation.
        // If this fails, the listing remains safely stored in RoomDB.
        trySyncNow(
            clientId = clientId,
            title = title,
            description = description,
            categoryID = categoryID,
            price = price,
            condition = condition
        )

        // STEP 3: Return the local identifier.
        // WorkManager handles background retries.
        return clientId
    }

    /**
     * Attempts to synchronise a newly created listing immediately.
     *
     * Failure does not delete the local record or change its
     * pendingSync status.
     */
    private suspend fun trySyncNow(
        clientId: String,
        title: String,
        description: String,
        categoryID: Int,
        price: Double,
        condition: String?
    ) {

        try {

            Log.i(TAG, "Attempting immediate synchronisation: $clientId")

            val response = api.createListing(
                CreateListingRequest(
                    clientId = clientId,
                    title = title,
                    description = description,
                    price = price,
                    categoryID = categoryID,
                    condition = condition
                )
            )

            if (response.isSuccessful) {

                val serverListing = response.body()

                if (serverListing != null && serverListing.listingID > 0) {

                    // Mark the local record as synchronised.
                    dao.markSynced(
                        clientId,
                        serverListing.listingID
                    )

                    Log.i(
                        TAG,
                        "Immediate synchronisation successful: $clientId"
                    )

                } else {

                    Log.w(
                        TAG,
                        "Server returned an invalid response. Listing remains pending: $clientId"
                    )
                }

            } else {

                Log.w(
                    TAG,
                    "Immediate synchronisation failed: HTTP ${response.code()}. Listing remains pending."
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Immediate synchronisation failed. Listing remains stored locally: $clientId",
                e
            )
        }
    }

    // ---------------------------------------------------------
    // FAVOURITES
    // ---------------------------------------------------------

    /**
     * Observes locally saved favourites.
     */
    fun getFavouritesFromDb(): Flow<List<FavouriteEntity>> =
        favouriteDao.getAllFavourites()

    /**
     * Updates local favourite storage and attempts server synchronisation.
     */
    suspend fun toggleFavourite(
        listing: Listing,
        currentUserId: String
    ) {

        val listingIdStr = listing.listingID.toString()

        val userIdInt = currentUserId.toIntOrNull()
            ?: currentUserId.hashCode()

        try {

            val alreadyFavourite =
                favouriteDao.isFavouriteSync(listingIdStr)

            if (alreadyFavourite) {

                favouriteDao.deleteFavouriteById(listingIdStr)

                try {
                    api.removeFavourite(
                        userId = currentUserId,
                        listingId = listingIdStr
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Favourite removal could not synchronise.", e)
                }

            } else {

                favouriteDao.insertFavourite(
                    FavouriteEntity(
                        listingId = listingIdStr,
                        title = listing.title,
                        price = listing.price,
                        imageUrl = listing.image
                    )
                )

                try {
                    api.addFavourite(
                        FavouriteRequest(
                            listingId = listing.listingID,
                            userId = userIdInt
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Favourite addition could not synchronise.", e)
                }
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(TAG, "Unable to update local favourites.", e)

            throw IllegalStateException(
                "Unable to update your favourites. Please try again.",
                e
            )
        }
    }

    /**
     * Refreshes locally stored favourites using server data.
     */
    suspend fun syncFavouritesFromRemote(userId: String) {

        try {

            val response = api.getFavourites(userId)

            if (response.isSuccessful) {

                val listings = response.body().orEmpty()

                favouriteDao.clearAll()

                listings.forEach { listing ->

                    favouriteDao.insertFavourite(
                        FavouriteEntity(
                            listingId = listing.listingID.toString(),
                            title = listing.title,
                            price = listing.price,
                            imageUrl = listing.image
                        )
                    )
                }

                Log.i(TAG, "Favourites successfully synchronised.")

            } else {

                Log.w(
                    TAG,
                    "GET /api/favourites/$userId failed: HTTP ${response.code()}"
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Favourites synchronisation failed. Local cache retained.",
                e
            )
        }
    }
}

// -------------------------------------------------------------
// DATABASE ENTITY AND DOMAIN MODEL MAPPERS
// -------------------------------------------------------------

/**
 * Converts a domain Listing into a RoomDB ListingEntity.
 */
private fun Listing.toEntity(
    pendingSync: Boolean
) = ListingEntity(
    clientId = listingID.toString(),
    serverListingId = listingID,
    title = title,
    description = description,
    categoryID = categoryID,
    price = price,
    condition = condition,
    image = image,
    sellerID = sellerID,
    status = status,
    sellerName = sellerName,
    sellerTrustScore = sellerTrustScore,
    pendingSync = pendingSync
)

/**
 * Converts a RoomDB ListingEntity into a domain Listing.
 */
private fun ListingEntity.toDomain() = Listing(
    listingID = serverListingId ?: 0,
    title = title,
    description = description,
    categoryID = categoryID,
    price = price,
    condition = condition,
    image = image,
    datePosted = "",
    sellerID = sellerID,
    status = status,
    sellerName = sellerName,
    sellerTrustScore = sellerTrustScore,
    pendingSync = pendingSync,
    clientId = clientId
)
