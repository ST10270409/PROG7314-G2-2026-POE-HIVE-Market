package com.hivemarket.app.ui.screens.settings

import com.google.firebase.auth.FirebaseAuth
import com.hivemarket.app.data.local.LocalSettings
import com.hivemarket.app.data.local.LocaleSwitcher
import com.hivemarket.app.data.local.SettingsStore
import com.hivemarket.app.data.remote.HiveMarketApi
import com.hivemarket.app.data.remote.SettingsRequest
import com.hivemarket.app.data.remote.UpdatedResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Covers the offline-first contract in SettingsViewModel: a change is
 * written locally and reflected in state immediately, and a failed API
 * push degrades gracefully (syncFailed = true) rather than losing or
 * rolling back the local change (FR2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val api: HiveMarketApi = mockk()
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val localeSwitcher: LocaleSwitcher = mockk(relaxed = true)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsStore.read() } returns LocalSettings()
        coEvery { api.updateSettings(any()) } returns Response.success(UpdatedResponse(updated = true))
        viewModel = SettingsViewModel(settingsStore, api, firebaseAuth, localeSwitcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggling notifications updates state immediately, before the API call resolves`() = runTest {
        viewModel.setNotificationsEnabled(false)

        // Checked before advanceUntilIdle() — the local write and state
        // update must happen synchronously, not wait on the network call.
        assertFalse(viewModel.uiState.value.settings.notificationsEnabled)
        assertTrue(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun `toggling notifications writes to the local store`() = runTest {
        viewModel.setNotificationsEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { settingsStore.write(match { !it.notificationsEnabled }) }
    }

    @Test
    fun `a successful sync clears isSyncing and syncFailed`() = runTest {
        viewModel.setBiometricEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
        assertFalse(viewModel.uiState.value.syncFailed)
        coVerify {
            api.updateSettings(match<SettingsRequest> { it.biometricEnabled == true })
        }
    }

    @Test
    fun `when the API call fails the local change still stands and syncFailed is set`() = runTest {
        coEvery { api.updateSettings(any()) } throws java.io.IOException("offline")

        viewModel.setBiometricEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Local value should stick even though sync failed", viewModel.uiState.value.settings.biometricEnabled)
        assertTrue(viewModel.uiState.value.syncFailed)
    }

    @Test
    fun `signOut calls FirebaseAuth signOut and flips signedOut`() {
        viewModel.signOut()

        verify { firebaseAuth.signOut() }
        assertTrue(viewModel.uiState.value.signedOut)
    }

    @Test
    fun `changing language updates the language field and calls the locale switcher`() = runTest {
        viewModel.setLanguage("zu")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("zu", viewModel.uiState.value.settings.language)
        verify { localeSwitcher.apply("zu") }
    }
}
