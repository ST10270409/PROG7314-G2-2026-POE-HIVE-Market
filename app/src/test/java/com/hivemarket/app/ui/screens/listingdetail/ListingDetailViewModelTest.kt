package com.hivemarket.app.ui.screens.listingdetail

import androidx.lifecycle.SavedStateHandle
import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.Listing
import com.hivemarket.app.domain.Offer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ListingRepository = mockk()
    private lateinit var viewModel: ListingDetailViewModel

    private val sampleListing = Listing(
        listingID = 5, title = "Desk lamp", description = "Works fine", categoryID = 3,
        price = 80.0, condition = "Used - Good", image = null, datePosted = "2026-08-01T10:00:00Z",
        sellerID = 1, status = "Active", sellerName = "Thabiso", sellerTrustScore = 82
    )

    private fun buildViewModel() {
        viewModel = ListingDetailViewModel(repository, SavedStateHandle(mapOf("listingID" to 5)))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the listing on init and exposes it as Loaded`() = runTest {
        coEvery { repository.getListingDetail(5) } returns ApiResult.Success(sampleListing)

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ListingDetailUiState.Loaded)
        assertEquals("Desk lamp", (state as ListingDetailUiState.Loaded).listing.title)
    }

    @Test
    fun `a failed load surfaces as Error, not a crash`() = runTest {
        coEvery { repository.getListingDetail(5) } returns ApiResult.Error("Listing not available offline")

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ListingDetailUiState.Error)
    }

    @Test
    fun `makeOffer on success emits an OfferSubmitted event`() = runTest {
        coEvery { repository.getListingDetail(5) } returns ApiResult.Success(sampleListing)
        coEvery { repository.makeOffer(5, 70.0, "Would you take 70?") } returns
            ApiResult.Success(Offer(1, 5, 2, 70.0, "Would you take 70?", "2026-08-01T10:00:00Z", "Pending"))

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.makeOffer(70.0, "Would you take 70?")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ListingDetailEvent.OfferSubmitted(true), viewModel.events.value)
    }

    @Test
    fun `openOrStartConversation on success emits ConversationReady`() = runTest {
        coEvery { repository.getListingDetail(5) } returns ApiResult.Success(sampleListing)
        val conversation = Conversation(1, 5, 2, 1, otherParticipant = "Thabiso")
        coEvery { repository.startOrGetConversation(5) } returns ApiResult.Success(conversation)

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.openOrStartConversation()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.value
        assertTrue(event is ListingDetailEvent.ConversationReady)
        assertEquals(1, (event as ListingDetailEvent.ConversationReady).conversation.conversationID)
    }
}
