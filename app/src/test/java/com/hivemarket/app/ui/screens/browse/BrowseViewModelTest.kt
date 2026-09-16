package com.hivemarket.app.ui.screens.browse

import com.hivemarket.app.data.repository.ApiResult
import com.hivemarket.app.data.repository.ListingRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

/**
 * Covers BrowseViewModel's category-to-filter mapping and its offline
 * fallback behaviour — the two pieces of real logic in this ViewModel
 * beyond simple state-holding.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ListingRepository = mockk()
    private lateinit var viewModel: BrowseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Empty cache by default; individual tests override refreshListings' result.
        every { repository.observeCachedListings() } returns flowOf(emptyList())
        coEvery { repository.refreshListings(categoryID = any(), query = null) } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting Textbook maps to categoryID 1`() = runTest {
        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(Category.TEXTBOOK)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.refreshListings(categoryID = 1, query = null) }
    }

    @Test
    fun `selecting Electronics maps to categoryID 2`() = runTest {
        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(Category.ELECTRONICS)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.refreshListings(categoryID = 2, query = null) }
    }

    @Test
    fun `selecting Furniture maps to categoryID 3`() = runTest {
        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(Category.FURNITURE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.refreshListings(categoryID = 3, query = null) }
    }

    @Test
    fun `selecting All maps to no categoryID filter`() = runTest {
        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(Category.TEXTBOOK) // move away from the default first
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectCategory(Category.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.refreshListings(categoryID = null, query = null) }
    }

    @Test
    fun `when refresh fails the UI state flips to offline instead of showing nothing`() = runTest {
        coEvery { repository.refreshListings(categoryID = any(), query = null) } returns
            ApiResult.Error("no connectivity")

        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOffline)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `when refresh succeeds isOffline is false`() = runTest {
        viewModel = BrowseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOffline)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(Category.ALL, viewModel.uiState.value.selectedCategory)
    }
}
