package com.hivemarket.app.ui.screens.offlinedrafts

import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import com.hivemarket.app.domain.Listing
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDraftsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ListingRepository = mockk()
    private lateinit var viewModel: OfflineDraftsViewModel

    private val draft = Listing(
        listingID = 0, title = "Desk lamp", description = "Works fine", categoryID = 3,
        price = 80.0, condition = "Used - Good", image = null, datePosted = "",
        sellerID = 1, status = "Active", pendingSync = true, clientId = "guid-1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes pending drafts from the repository`() = runTest {
        every { repository.observePendingDrafts() } returns flowOf(listOf(draft))

        viewModel = OfflineDraftsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.drafts.size)
        assertEquals("Desk lamp", viewModel.uiState.value.drafts[0].title)
    }

    @Test
    fun `a successful retry clears retryingClientId and lastRetryFailed`() = runTest {
        every { repository.observePendingDrafts() } returns flowOf(listOf(draft))
        coEvery { repository.retrySync("guid-1") } returns ApiResult.Success(Unit)

        viewModel = OfflineDraftsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retry("guid-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.retryingClientId)
        assertFalse(viewModel.uiState.value.lastRetryFailed)
    }

    @Test
    fun `a failed retry sets lastRetryFailed without crashing`() = runTest {
        every { repository.observePendingDrafts() } returns flowOf(listOf(draft))
        coEvery { repository.retrySync("guid-1") } returns ApiResult.Error("offline")

        viewModel = OfflineDraftsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retry("guid-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastRetryFailed)
    }
}
