package com.hivemarket.app.ui.screens.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.hivemarket.app.R

/**
 * Mirrors the Login wireframe from the Planning and Design document:
 * student email + password fields, a primary Sign In action, a secondary
 * Google option, and inline error feedback (rather than a dialog/toast)
 * so the failure state is visible in a demo recording without narration.
 *
 * Also toggles into a Register mode, and offers Google Sign-In via the
 * classic GoogleSignInClient API. Both feed into the same Firebase Auth
 * instance as email/password sign-in, so LoginUiState.Success and the
 * LaunchedEffect below fire the same way regardless of which method the
 * person used.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    // GoogleSignInOptions.requestIdToken(...) is required — without it,
    // account.idToken below is ALWAYS null, and Google Sign-In silently
    // does nothing after the account picker closes (no error, no
    // success, just nothing). The Web Client ID this needs is
    // auto-generated into R.string.default_web_client_id by the
    // google-services Gradle plugin, but ONLY once Google is enabled as
    // a sign-in provider in Firebase Console and a fresh
    // google-services.json is downloaded afterward.
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    // Safe cast: MainActivity is this app's sole Activity and hosts the
    // Compose content directly (see MainActivity.kt), so LocalContext.current
    // here is always that Activity, never a wrapped/unrelated Context.
    val googleSignInClient = remember { GoogleSignIn.getClient(context as Activity, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                            onSignedIn()
                        } else {
                            Toast.makeText(context, "Authentication Failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Google Sign-In did not return a valid token.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Navigate away the moment sign-in/registration succeeds; LaunchedEffect
    // keys on uiState so this only fires once per state change, not on
    // every recomposition.
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onSignedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isRegisterMode) "Create your account" else stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_email_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        when (val state = uiState) {
            is LoginUiState.Loading -> CircularProgressIndicator()
            is LoginUiState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            else -> {}
        }

        Button(
            onClick = {
                if (isRegisterMode) viewModel.register(email.trim(), password)
                else viewModel.signIn(email.trim(), password)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegisterMode) "Register" else stringResource(R.string.login_sign_in))
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(
                if (isRegisterMode) "Already have an account? Sign in"
                else "New here? Register"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.login_or), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleLauncher.launch(signInIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_google))
        }
    }
}