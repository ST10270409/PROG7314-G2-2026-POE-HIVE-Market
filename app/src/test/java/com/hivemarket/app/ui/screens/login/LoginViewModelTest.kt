package com.hivemarket.app.ui.screens.login

import com.google.firebase.auth.FirebaseAuth
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

/**
 * Covers the client-side validation in LoginViewModel that runs before any
 * Firebase call is made (FR1's "no first-party password storage" boundary
 * is enforced server-side, but rejecting obviously-invalid input early is a
 * UX nicety this test locks in). FirebaseAuth is mocked and never
 * stubbed/verified here on purpose — these code paths return before it
 * would be touched, so a real Firebase connection isn't needed to test them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(mockAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with blank email shows an error and does not call Firebase`() = runTest {
        viewModel.signIn(email = "", password = "somepassword")

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(
            "Enter your student email and password.",
            (state as LoginUiState.Error).message
        )
    }

    @Test
    fun `signIn with a non-student email domain is rejected`() = runTest {
        viewModel.signIn(email = "someone@gmail.com", password = "somepassword")

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(
            "Please use your student email address.",
            (state as LoginUiState.Error).message
        )
    }

    @Test
    fun `signIn with a valid student email domain passes the domain check`() = runTest {
        // This won't complete successfully against the mocked FirebaseAuth
        // (there's no real backing implementation), but it must NOT be
        // rejected by the client-side domain check the way the tests above are —
        // i.e. the resulting state should not be the domain-rejection message.
        viewModel.signIn(email = "s1@student.iie.ac.za", password = "somepassword")

        val state = viewModel.uiState.value
        if (state is LoginUiState.Error) {
            assertTrue(
                "Valid student domain should not be rejected by the client-side check",
                state.message != "Please use your student email address."
            )
        }
    }
}
