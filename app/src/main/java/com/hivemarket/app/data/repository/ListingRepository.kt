package com.hivemarket.app.data.repository

import android.util.Log
import com.hivemarket.app.data.local.ListingDao
import com.hivemarket.app.data.local.ListingEntity
import com.hivemarket.app.data.remote.CreateListingRequest
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.domain.Listing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

/**
 * Repository pattern per the "Structural plan" in the Planning and Design
 * document: the Android client never talks to the database directly — it
 * goes through this repository, which is the single source of truth
 * combining the Room cache with the API.
 */
@Singleton
class ListingRepository @Inject constructor(
    private val api: HiveMarketApi,
    private val dao: ListingDao
) {
    companion object {
        private const val TAG = "ListingRepository"
    }

    /** Live, locally-cached listings — usable immediately even before a network call resolves. */
    fun observeCachedListings(): Flow<List<Listing>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Refreshes the cache from GET /api/listings. On failure, callers still
     * have whatever's already cached via [observeCachedListings] — this is
     * the offline-first behaviour FR4 describes.
     */
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
            // No connectivity, DNS failure, timeout, etc. — the cached Flow
            // above still has last-known-good data for the UI to show.
            Log.w(TAG, "GET /api/listings failed, serving cache", e)
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * FR4/FR9: writes locally first with a client-generated GUID so the UI
     * is instant and works offline, then attempts to sync immediately. If
     * that fails, the row stays pendingSync = true for WorkManager to pick
     * up later (see the offline sync sequence diagram, Figure 6).
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
            // Left pendingSync = true; WorkManager's sync worker retries later.
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
    pendingSync = pendingSync
)
