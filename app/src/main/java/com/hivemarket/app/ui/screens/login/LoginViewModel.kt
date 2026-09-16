package com.hivemarket.app.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * FR1: Firebase Authentication, restricted to a recognised student email
 * domain. This prototype checks the domain client-side for immediate UX
 * feedback; the API still re-verifies the token server-side on every
 * request, per the login sequence diagram (Figure 5) — the client check
 * here is a convenience, not the security boundary.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
        // TODO(Wabo): replace with your actual institution's real student
        // email domain — e.g. "@student.iie.ac.za" was always a placeholder,
        // never a real one. Whatever comes after the @ in your actual
        // student email address goes here.
        private const val ALLOWED_EMAIL_DOMAIN = "@student.iie.ac.za"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Enter your student email and password.")
            return
        }
        if (!email.endsWith(ALLOWED_EMAIL_DOMAIN, ignoreCase = true)) {
            Log.w(TAG, "Rejected sign-in attempt with non-student domain: $email")
            _uiState.value = LoginUiState.Error("Please use your student email address.")
            return
        }

        _uiState.value = LoginUiState.Loading
        Log.i(TAG, "Attempting sign-in for $email")

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Log.i(TAG, "Sign-in succeeded, uid=${auth.currentUser?.uid}")
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                Log.w(TAG, "Sign-in failed", e)
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-in failed.")
            }
        }
    }

    fun register(email: String, password: String) {
        if (!email.endsWith(ALLOWED_EMAIL_DOMAIN, ignoreCase = true)) {
            _uiState.value = LoginUiState.Error("Registration requires a student email address.")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                Log.i(TAG, "Registration succeeded for $email")
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                Log.w(TAG, "Registration failed", e)
                _uiState.value = LoginUiState.Error(e.message ?: "Registration failed.")
            }
        }
    }
}
