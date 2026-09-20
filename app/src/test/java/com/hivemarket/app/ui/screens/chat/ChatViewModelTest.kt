package com.hivemarket.app.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.MessagesResponse
import com.hivemarket.app.domain.Message
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val api: HiveMarketApi = mockk()
    private lateinit var viewModel: ChatViewModel

    private val sampleMessage = Message(1, 7, 2, "Is this still available?", "Sent", "2026-08-01T10:00:00Z")

    private fun buildViewModel() {
        viewModel = ChatViewModel(api, SavedStateHandle(mapOf("conversationID" to 7)))
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
    fun `loads message history on init`() = runTest {
        coEvery { api.getMessages(7) } returns Response.success(MessagesResponse(items = listOf(sampleMessage)))

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a failed load shows an empty thread rather than crashing`() = runTest {
        coEvery { api.getMessages(7) } returns
            Response.error(500, "error".toResponseBody("text/plain".toMediaType()))

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(viewModel.uiState.value.isOffline)
    }

    @Test
    fun `send appends the returned message to state and clears the draft`() = runTest {
        coEvery { api.getMessages(7) } returns Response.success(MessagesResponse(items = emptyList()))
        val sent = Message(2, 7, 1, "Sure, 2pm works", "Sent", "2026-08-01T10:05:00Z")
        coEvery { api.sendMessage(7, any()) } returns Response.success(sent)

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDraft("Sure, 2pm works")
        viewModel.send()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `send with a blank draft does not call the API`() = runTest {
        coEvery { api.getMessages(7) } returns Response.success(MessagesResponse(items = emptyList()))

        buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDraft("   ")
        viewModel.send()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { api.sendMessage(any(), any()) }
    }
}
