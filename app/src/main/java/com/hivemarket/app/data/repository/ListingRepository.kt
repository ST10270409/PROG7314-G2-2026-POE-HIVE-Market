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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {

    data class Success<T>(
        val data: T
    ) : ApiResult<T>()

    data class Error(
        val message: String
    ) : ApiResult<Nothing>()
}

/**
 * Repository pattern per the "Structural plan" in the Planning and Design
 * document.
 *
 * The Android client uses this repository as the single source of truth
 * between the Room database and the remote API.
 */
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
    // LISTINGS
    // ---------------------------------------------------------

    /**
     * Live, locally cached listings.
     */
    fun observeCachedListings(): Flow<List<Listing>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Live list of listings still waiting to sync.
     */
    fun observePendingDrafts(): Flow<List<Listing>> =
        dao.observePendingSync().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Manually attempts to sync one pending listing.
     */
    suspend fun retrySync(
        clientId: String
    ): ApiResult<Unit> {

        val entity = dao.getPendingSync()
            .firstOrNull { it.clientId == clientId }
            ?: return ApiResult.Error("Draft not found")

        return try {

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

                response.body()?.let { createdListing ->
                    dao.markSynced(
                        entity.clientId,
                        createdListing.listingID
                    )
                }

                ApiResult.Success(Unit)

            } else {

                ApiResult.Error(
                    "Server returned ${response.code()}"
                )
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Retry sync failed for $clientId",
                e
            )

            ApiResult.Error(
                e.message ?: "Network error"
            )
        }
    }

    /**
     * Fetches a single listing's full detail.
     *
     * If the API request fails, the repository attempts
     * to return the locally cached version.
     */
    suspend fun getListingDetail(
        listingID: Int
    ): ApiResult<Listing> {

        return try {

            val response = api.getListing(listingID)

            if (response.isSuccessful && response.body() != null) {

                val listing = response.body()!!

                dao.upsert(
                    listing.toEntity(
                        pendingSync = false
                    )
                )

                ApiResult.Success(listing)

            } else {

                Log.w(
                    TAG,
                    "GET /api/listings/$listingID failed: ${response.code()}"
                )

                fallBackToCache(listingID)
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "GET /api/listings/$listingID failed, trying cache",
                e
            )

            fallBackToCache(listingID)
        }
    }

    /**
     * Returns a cached listing when the API is unavailable.
     */
    private suspend fun fallBackToCache(
        listingID: Int
    ): ApiResult<Listing> {

        val cached = dao.getById(listingID)

        return if (cached != null) {
            ApiResult.Success(
                cached.toDomain()
            )
        } else {
            ApiResult.Error(
                "Listing not available offline"
            )
        }
    }

    /**
     * FR11: Making an offer is online-only.
     */
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

                ApiResult.Success(
                    response.body()!!
                )

            } else {

                ApiResult.Error(
                    "Server returned ${response.code()}"
                )
            }

        } catch (e: Exception) {

            ApiResult.Error(
                e.message ?: "Network error"
            )
        }
    }

    /**
     * Finds an existing conversation for a listing.
     *
     * If no conversation exists, a new one is started.
     */
    suspend fun startOrGetConversation(
        listingID: Int
    ): ApiResult<Conversation> {

        return try {

            val existing = api.getConversations()

            val match = existing.body()
                ?.items
                ?.firstOrNull {
                    it.listingID == listingID
                }

            if (match != null) {
                return ApiResult.Success(match)
            }

            val response = api.startConversation(
                StartConversationRequest(listingID)
            )

            if (response.isSuccessful && response.body() != null) {

                ApiResult.Success(
                    response.body()!!
                )

            } else {

                ApiResult.Error(
                    "Server returned ${response.code()}"
                )
            }

        } catch (e: Exception) {

            ApiResult.Error(
                e.message ?: "Network error"
            )
        }
    }

    /**
     * Refreshes the local listing cache from the API.
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

                val listings =
                    response.body()?.items.orEmpty()

                Log.d(
                    TAG,
                    "Fetched ${listings.size} listings from API"
                )

                dao.upsertAll(
                    listings.map {
                        it.toEntity(
                            pendingSync = false
                        )
                    }
                )

                ApiResult.Success(Unit)

            } else {

                Log.w(
                    TAG,
                    "GET /api/listings failed: ${response.code()}"
                )

                ApiResult.Error(
                    "Server returned ${response.code()}"
                )
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "GET /api/listings failed, serving cache",
                e
            )

            ApiResult.Error(
                e.message ?: "Network error"
            )
        }
    }

    /**
     * FR4/FR9:
     *
     * Saves a listing locally first and then attempts
     * to synchronise it with the backend.
     */
    suspend fun createListingOfflineFirst(
        title: String,
        description: String,
        categoryID: Int,
        price: Double,
        condition: String?,
        sellerID: Int
    ): String {

        val clientId = UUID.randomUUID().toString()

        dao.upsert(
            ListingEntity(
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
        )

        trySyncNow(
            clientId = clientId,
            title = title,
            description = description,
            categoryID = categoryID,
            price = price,
            condition = condition
        )

        return clientId
    }

    /**
     * Attempts to synchronise a newly created listing immediately.
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

                response.body()?.let { createdListing ->

                    dao.markSynced(
                        clientId,
                        createdListing.listingID
                    )

                    Log.d(
                        TAG,
                        "Listing $clientId synced immediately"
                    )
                }

            } else {

                Log.w(
                    TAG,
                    "Immediate sync returned ${response.code()} for $clientId"
                )
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Immediate sync failed for $clientId, will retry via WorkManager",
                e
            )
        }
    }

    // ---------------------------------------------------------
    // FAVOURITES
    // ---------------------------------------------------------

    /**
     * Observes all locally saved favourites.
     */
    fun getFavouritesFromDb(): Flow<List<FavouriteEntity>> =
        favouriteDao.getAllFavourites()

    /**
     * Checks whether a specific listing is currently favourited.
     */
    fun isListingFavourite(
        listingID: Int
    ): Flow<Boolean> =
        favouriteDao.isFavourite(listingID.toString())

    /**
     * Toggles a listing's favourite state.
     */
    suspend fun toggleFavourite(
        listing: Listing,
        userId: String
    ): Boolean {
        val listingIdStr = listing.listingID.toString()
        val listingIdInt = listing.listingID
        val userIdInt = userId.toIntOrNull() ?: 0

        val currentlyFavourite = favouriteDao.isFavouriteSync(listingIdStr)

        return if (currentlyFavourite) {

            // Remove locally first
            favouriteDao.deleteFavouriteById(listingIdStr)

            // Remove from backend (expects String parameters)
            try {
                api.removeFavourite(
                    userId = userId,
                    listingId = listingIdStr
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove favourite on backend", e)
            }

            false

        } else {

            // Add locally first
            val favourite = FavouriteEntity(
                listingId = listingIdStr,
                title = listing.title,
                price = listing.price,
                imageUrl = listing.image,
                isAvailable = true
            )

            favouriteDao.insertFavourite(favourite)

            // Add to backend (FavouriteRequest expects Int parameters)
            try {
                api.addFavourite(
                    FavouriteRequest(
                        listingId = listingIdInt,
                        userId = userIdInt
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add favourite on backend", e)
            }

            true
        }
    }

    /**
     * Downloads user favourites from backend and caches them locally.
     */
    suspend fun syncFavouritesFromRemote(
        userId: String
    ) {
        try {
            // api.getFavourites expects String
            val response = api.getFavourites(userId)

            if (response.isSuccessful && response.body() != null) {
                val remoteFavourites = response.body()!!

                remoteFavourites.forEach { listing ->
                    favouriteDao.insertFavourite(
                        FavouriteEntity(
                            listingId = listing.listingID.toString(),
                            title = listing.title,
                            price = listing.price,
                            imageUrl = listing.image,
                            isAvailable = true
                        )
                    )
                }

                Log.d(TAG, "Synced ${remoteFavourites.size} favourites from backend")
            } else {
                Log.w(TAG, "Failed to fetch favourites: ${response.code()}")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync favourites from remote", e)
        }
    }

    // ---------------------------------------------------------
    // LISTING ↔ ROOM MAPPERS
    // ---------------------------------------------------------

    /**
     * Converts a domain Listing into a Room ListingEntity.
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
     * Converts a Room ListingEntity into a domain Listing.
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
}