package com.hivemarket.app.ui.screens.createlisting

import com.google.firebase.auth.FirebaseAuth
import com.hivemarket.app.data.local.SyncScheduler
import com.hivemarket.app.data.repository.ListingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Covers CreateListingViewModel's validation (the one piece of real logic
 * here — the offline-first write itself is ListingRepository's
 * responsibility and is already covered by ListingRepositoryTest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateListingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ListingRepository = mockk()
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)
    private lateinit var viewModel: CreateListingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.createListingOfflineFirst(any(), any(), any(), any(), any(), any()) } returns "client-guid-1"
        viewModel = CreateListingViewModel(repository, firebaseAuth, syncScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit with a blank title is rejected before touching the repository`() = runTest {
        viewModel.setTitle("")
        viewModel.setPrice("80")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.createListingOfflineFirst(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `submit with a non-numeric price is rejected`() = runTest {
        viewModel.setTitle("Desk lamp")
        viewModel.setPrice("free")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.createListingOfflineFirst(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a valid submission calls the repository and clears the form`() = runTest {
        viewModel.setTitle("Desk lamp")
        viewModel.setPrice("80")
        viewModel.setDescription("Works fine")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.createListingOfflineFirst(
                title = "Desk lamp", description = "Works fine", categoryID = any(),
                price = 80.0, condition = any(), sellerID = any()
            )
        }
        assertEquals("client-guid-1", viewModel.uiState.value.submittedClientId)
        assertEquals("", viewModel.uiState.value.title) // form reset after a successful submit
    }

    @Test
    fun `a valid submission schedules a WorkManager sync attempt`() = runTest {
        viewModel.setTitle("Desk lamp")
        viewModel.setPrice("80")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { syncScheduler.scheduleSync() }
    }
}