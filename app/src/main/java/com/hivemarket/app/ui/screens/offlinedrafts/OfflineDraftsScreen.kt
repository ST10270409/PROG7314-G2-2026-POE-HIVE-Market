package com.hivemarket.app.ui.screens.offlinedrafts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hivemarket.app.domain.Listing

/**
 * Mirrors the Offline Drafts wireframe: a count of drafts waiting to sync,
 * and a card per draft with a pending badge. Adds a Retry action per card,
 * since there's no automatic background sync worker yet (see the
 * ViewModel's comment) — without it, a draft that failed its one
 * immediate sync attempt at creation time would be stuck forever with no
 * way to try again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDraftsScreen(
    onBack: () -> Unit,
    viewModel: OfflineDraftsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Drafts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                if (uiState.drafts.isEmpty()) "No drafts waiting to sync"
                else "${uiState.drafts.size} draft${if (uiState.drafts.size == 1) "" else "s"} waiting to sync",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (uiState.lastRetryFailed) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Still couldn't sync — check your connection and try again.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.drafts.isEmpty()) {
                    Text(
                        "Listings you create while offline will show up here until they sync.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.drafts, key = { it.clientId ?: it.listingID }) { draft ->
                            DraftCard(
                                draft = draft,
                                isRetrying = uiState.retryingClientId == draft.clientId,
                                onRetry = { draft.clientId?.let(viewModel::retry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCard(draft: Listing, isRetrying: Boolean, onRetry: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp).aspectRatio(1f)
            ) { /* photo placeholder — see README */ }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(draft.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "R${draft.price.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isRetrying) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
