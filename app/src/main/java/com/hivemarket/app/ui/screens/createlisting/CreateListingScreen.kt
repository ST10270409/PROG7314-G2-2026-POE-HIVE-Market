package com.hivemarket.app.ui.screens.createlisting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Mirrors the Create Listing wireframe: title, category, price,
 * description, and an "Upload photos" control. Photo upload itself is a
 * stub — Firebase Storage isn't wired into this project yet (see the
 * README's "what's deliberately not built yet" section) — but the rest of
 * the form is fully functional and offline-first via
 * CreateListingViewModel/ListingRepository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onViewDrafts: () -> Unit = {},
    viewModel: CreateListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submittedClientId) {
        if (uiState.submittedClientId != null) {
            onSubmitted()
            viewModel.consumeSubmission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What are you selling today?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = onViewDrafts) { Text("Drafts") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Listing title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            CategoryDropdown(selected = uiState.categoryID, onSelect = viewModel::setCategory)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::setPrice,
                label = { Text("Price (R)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = { /* TODO: Firebase Storage not wired yet */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Upload photos")
            }
            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSubmitting) "Saving…" else "Post Listing")
            }
            Text(
                "Works offline — this will sync automatically once you're back online.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Matches the seed categories in the Data Models table (Section 7):
    // Textbooks=1, Electronics=2, Furniture=3.
    val categories = listOf(1 to "Textbook", 2 to "Electronics", 3 to "Furniture")
    val currentLabel = categories.firstOrNull { it.first == selected }?.second ?: "Category"

    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(currentLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { (id, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
        }
    }
}
