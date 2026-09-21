package com.hivemarket.app.data.repository

import android.util.Log
import com.hivemarket.app.data.local.ListingDao
import com.hivemarket.app.data.local.ListingEntity
import com.hivemarket.app.data.remote.CreateListingRequest
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.MakeOfferRequest
import com.hivemarket.app.data.remote.StartConversationRequest
import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.Listing
import com.hivemarket.app.domain.Offer
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
    private val dao: ListingDao
) {
    companion object {
        private const val TAG = "ListingRepository"
    }

    fun observeCachedListings(): Flow<List<Listing>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observePendingDrafts(): Flow<List<Listing>> =
        dao.observePendingSync().map { entities -> entities.map { it.toDomain() } }

    suspend fun retrySync(clientId: String): ApiResult<Unit> {
        val entity = dao.getPendingSync().firstOrNull { it.clientId == clientId }
            ?: return ApiResult.Error("Draft not found")
        return syncOneEntity(entity)
    }

    suspend fun syncAllPendingDrafts(): Int {
        val pending = dao.getPendingSync()
        var failures = 0
        for (entity in pending) {
            if (syncOneEntity(entity) is ApiResult.Error) failures++
        }
        return failures
    }

    private suspend fun syncOneEntity(entity: ListingEntity): ApiResult<Unit> = try {
        val response = api.createListing(
            CreateListingRequest(
                clientId = entity.clientId, title = entity.title, description = entity.description,
                price = entity.price, categoryID = entity.categoryID, condition = entity.condition
            )
        )
        if (response.isSuccessful) {
            response.body()?.let { dao.markSynced(entity.clientId, it.listingID) }
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error("Server returned ${response.code()}")
        }
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error")
    }

    suspend fun getListingDetail(listingID: Int): ApiResult<Listing> {
        return try {
            val response = api.getListing(listingID)
            if (response.isSuccessful && response.body() != null) {
                val listing = response.body()!!
                dao.upsert(listing.toEntity(pendingSync = false))
                ApiResult.Success(listing)
            } else {
                Log.w(TAG, "GET /api/listings/$listingID failed: ${response.code()}")
                fallBackToCache(listingID)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET /api/listings/$listingID failed, trying cache", e)
            fallBackToCache(listingID)
        }
    }

    private suspend fun fallBackToCache(listingID: Int): ApiResult<Listing> {
        val cached = dao.getById(listingID)
        return if (cached != null) ApiResult.Success(cached.toDomain())
        else ApiResult.Error("Listing not available offline")
    }

    suspend fun makeOffer(listingID: Int, amount: Double, message: String?): ApiResult<Offer> {
        return try {
            val response = api.makeOffer(listingID, MakeOfferRequest(amount = amount, message = message))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun startOrGetConversation(listingID: Int): ApiResult<Conversation> {
        return try {
            val existing = api.getConversations()
            val match = existing.body()?.items?.firstOrNull { it.listingID == listingID }
            if (match != null) return ApiResult.Success(match)

            val response = api.startConversation(StartConversationRequest(listingID))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun refreshListings(categoryID: Int? = null, query: String? = null): ApiResult<Unit> {
        return try {
            val response = api.getListings(query = query, categoryID = categoryID)
            if (response.isSuccessful) {
                val listings = response.body()?.items.orEmpty()
                Log.d(TAG, "Fetched ${listings.size} listings from API")
                dao.upsertAll(listings.map { it.toEntity(pendingSync = false) })
                ApiResult.Success(Unit)
            } else {
                Log.w(TAG, "GET /api/listings failed: ${response.code()}")
                ApiResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET /api/listings failed, serving cache", e)
            ApiResult.Error(e.message ?: "Network error")
        }
    }

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
        trySyncNow(clientId, title, description, categoryID, price, condition)
        return clientId
    }

    private suspend fun trySyncNow(
        clientId: String, title: String, description: String,
        categoryID: Int, price: Double, condition: String?
    ) {
        try {
            val response = api.createListing(
                CreateListingRequest(
                    clientId = clientId, title = title, description = description,
                    price = price, categoryID = categoryID, condition = condition
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { dao.markSynced(clientId, it.listingID) }
                Log.d(TAG, "Listing $clientId synced immediately")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync failed for $clientId, will retry via WorkManager", e)
        }
    }
}

private fun Listing.toEntity(pendingSync: Boolean) = ListingEntity(
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
