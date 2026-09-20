package com.hivemarket.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hivemarket.app.R

/**
 * Mirrors the Settings wireframe: Account (Edit Profile), Preferences
 * (Language, Notifications, Biometrics Login), App (About, Log Out).
 * Edit Profile and About are stubs for now — same "designed but not this
 * milestone's scope" status as Chat/Messages/Profile screens.
 *
 * All labels come from string resources (values / values-zu / values-af)
 * to stay consistent with FR6 — this screen switches language along with
 * the rest of the app, it doesn't hardcode English.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            SectionHeader(stringResource(R.string.settings_section_account))
            SettingsRow(label = stringResource(R.string.settings_edit_profile)) { /* TODO: not built this milestone */ }

            SectionSpacer()
            SectionHeader(stringResource(R.string.settings_section_preferences))
            LanguageRow(current = uiState.settings.language, onSelect = viewModel::setLanguage)
            ToggleRow(
                label = stringResource(R.string.settings_notifications),
                checked = uiState.settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled
            )
            ToggleRow(
                label = stringResource(R.string.settings_biometrics),
                checked = uiState.settings.biometricEnabled,
                onCheckedChange = viewModel::setBiometricEnabled
            )
            if (uiState.isSyncing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.settings_syncing), style = MaterialTheme.typography.bodySmall)
                }
            } else if (uiState.syncFailed) {
                Text(
                    stringResource(R.string.settings_sync_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            SectionSpacer()
            SectionHeader(stringResource(R.string.settings_section_app))
            SettingsRow(label = stringResource(R.string.settings_about)) { /* TODO: not built this milestone */ }
            TextButton(onClick = viewModel::signOut) {
                Text(stringResource(R.string.settings_log_out), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SectionSpacer() = androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        TextButton(onClick = onClick) { Text(">") }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageRow(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Labels are intentionally shown in their own language regardless of the
    // app's current locale (e.g. "isiZulu" not "Zulu" when viewing in
    // English) — this is standard practice for language pickers, since a
    // user looking for their language reads its own name, not a translation.
    val labels = mapOf("en" to "English", "zu" to "isiZulu", "af" to "Afrikaans")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.settings_language))
        TextButton(onClick = { expanded = true }) {
            Text(labels[current] ?: current)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (code, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(code)
                    expanded = false
                })
            }
        }
    }
}
