package com.hivemarket.app.data.repository

import com.hivemarket.app.data.local.ListingDao
import com.hivemarket.app.data.local.ListingEntity
import com.hivemarket.app.data.remote.CreateListingResponse
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.ListingsResponse
import com.hivemarket.app.domain.Listing
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Covers the API round-trip logic in ListingRepository: what happens on a
 * successful response, a server error response, and a network exception —
 * the three states the offline-first design in the Planning and Design
 * document depends on (Section 5 "Structural plan").
 */
class ListingRepositoryTest {

    private val api: HiveMarketApi = mockk()
    private val dao: ListingDao = mockk(relaxed = true)
    private lateinit var repository: ListingRepository

    private val sampleListing = Listing(
        listingID = 1,
        title = "Calculus textbook",
        description = "Barely used",
        categoryID = 1,
        price = 150.0,
        condition = "Used - Good",
        image = null,
        datePosted = "2026-08-01T10:00:00Z",
        sellerID = 1,
        status = "Active"
    )

    @Before
    fun setUp() {
        repository = ListingRepository(api, dao)
    }

    @Test
    fun `refreshListings on a successful response caches the listings and returns Success`() = runTest {
        coEvery { api.getListings(query = null, categoryID = null) } returns
                Response.success(ListingsResponse(items = listOf(sampleListing)))

        val result = repository.refreshListings()

        assertTrue(result is ApiResult.Success)
        // The repository's job is to keep Room in sync with what the API returned —
        // this checks that actually happened, not just that the call didn't crash.
        coVerify { dao.upsertAll(match { it.size == 1 && it[0].title == "Calculus textbook" }) }
    }

    @Test
    fun `refreshListings on a server error response returns Error and does not touch the cache`() = runTest {
        coEvery { api.getListings(query = null, categoryID = null) } returns
                Response.error(500, "server error".toResponseBody("text/plain".toMediaType()))

        val result = repository.refreshListings()

        assertTrue(result is ApiResult.Error)
        // A failed refresh must not wipe out whatever was already cached —
        // that cached data is what keeps the Browse screen usable offline.
        coVerify(exactly = 0) { dao.upsertAll(any()) }
    }

    @Test
    fun `refreshListings when the network throws returns Error instead of crashing`() = runTest {
        coEvery { api.getListings(query = null, categoryID = null) } throws java.io.IOException("no connectivity")

        val result = repository.refreshListings()

        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `refreshListings passes the selected category through to the API call`() = runTest {
        coEvery { api.getListings(query = null, categoryID = 2) } returns
                Response.success(ListingsResponse(items = emptyList()))

        repository.refreshListings(categoryID = 2)

        coVerify { api.getListings(query = null, categoryID = 2) }
    }

    @Test
    fun `createListingOfflineFirst writes a pendingSync row before attempting to sync`() = runTest {
        coEvery { api.createListing(any()) } returns
                Response.success(CreateListingResponse(listingID = 9, status = "Active", datePosted = "2026-08-01T10:00:00Z"))

        repository.createListingOfflineFirst(
            title = "Desk lamp",
            description = "Works fine",
            categoryID = 3,
            price = 80.0,
            condition = "Used - Good",
            sellerID = 1
        )

        // The local write (with pendingSync = true) must happen regardless of
        // whether the sync attempt right after it succeeds — that ordering is
        // what makes FR4/FR9 (offline listing creation) actually offline-first.
        coVerify {
            dao.upsert(match<ListingEntity> {
                it.title == "Desk lamp" && it.pendingSync
            })
        }
    }

    private val pendingEntity = ListingEntity(
        clientId = "guid-1", title = "Desk lamp", description = "Works fine",
        categoryID = 3, price = 80.0, condition = "Used - Good", sellerID = 1,
        status = "Active", pendingSync = true
    )

    @Test
    fun `syncAllPendingDrafts syncs every pending entity and marks each synced on success`() = runTest {
        coEvery { dao.getPendingSync() } returns listOf(pendingEntity)
        coEvery { api.createListing(any()) } returns
                Response.success(CreateListingResponse(listingID = 5, status = "Active", datePosted = "2026-08-01T10:00:00Z"))

        val failures = repository.syncAllPendingDrafts()

        assertTrue(failures == 0)
        coVerify { dao.markSynced("guid-1", 5) }
    }

    @Test
    fun `syncAllPendingDrafts counts failures instead of throwing when the API rejects a draft`() = runTest {
        coEvery { dao.getPendingSync() } returns listOf(pendingEntity)
        coEvery { api.createListing(any()) } returns
                Response.error(500, "server error".toResponseBody("text/plain".toMediaType()))

        val failures = repository.syncAllPendingDrafts()

        assertTrue(failures == 1)
        coVerify(exactly = 0) { dao.markSynced(any(), any()) }
    }

    @Test
    fun `syncAllPendingDrafts with nothing pending does no work and reports zero failures`() = runTest {
        coEvery { dao.getPendingSync() } returns emptyList()

        val failures = repository.syncAllPendingDrafts()

        assertTrue(failures == 0)
        coVerify(exactly = 0) { api.createListing(any()) }
    }
}