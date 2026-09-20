package com.hivemarket.app.ui.screens.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.SendMessageRequest
import com.hivemarket.app.domain.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val draft: String = ""
)

/**
 * Backs Chat. Loads message history via GET /api/conversations/{id}/messages
 * and posts new ones via the existing sendMessage endpoint. Falls back to
 * an empty (not broken) thread if the load fails, and appends sent
 * messages to local state immediately rather than waiting for a refetch —
 * the same "don't block the UI on the network" shape as the other
 * ViewModels, even though Message doesn't have a Room cache the way
 * Listing does (a real offline-first chat cache is a reasonable Part 3
 * extension, not attempted here).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: HiveMarketApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val conversationID: Int = checkNotNull(savedStateHandle["conversationID"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.getMessages(conversationID)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        messages = response.body()?.items.orEmpty(),
                        isLoading = false,
                        isOffline = false
                    )
                } else {
                    Log.w(TAG, "GET messages failed: ${response.code()}")
                    _uiState.value = _uiState.value.copy(isLoading = false, isOffline = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "GET messages failed, showing empty thread", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isOffline = true)
            }
        }
    }

    fun setDraft(text: String) {
        _uiState.value = _uiState.value.copy(draft = text)
    }

    fun send() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return

        Log.d(TAG, "Sending message in conversation $conversationID")
        _uiState.value = _uiState.value.copy(draft = "")

        viewModelScope.launch {
            try {
                val response = api.sendMessage(conversationID, SendMessageRequest(content = text))
                if (response.isSuccessful && response.body() != null) {
                    // Appended locally rather than re-fetching the whole
                    // thread — instant feedback, one less round-trip.
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + response.body()!!
                    )
                } else {
                    Log.w(TAG, "Send failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Send failed (likely offline) — message not queued in this milestone", e)
                // NOTE: unlike listings, there's no offline-send queue for
                // chat messages yet. A dropped message here is silently
                // lost rather than retried — a real gap worth flagging to
                // Luke/the group if Chat becomes one of the 3 chosen
                // features, since FR11 doesn't currently cover this case.
            }
        }
    }
}
