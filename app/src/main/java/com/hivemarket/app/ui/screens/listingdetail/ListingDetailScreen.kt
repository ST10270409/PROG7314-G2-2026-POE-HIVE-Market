package com.hivemarket.app.ui.screens.listingdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.Listing

/**
 * Mirrors the Listing Detail wireframe: back arrow, image, title/price, a
 * seller row with a Verified Student badge, description, and Make
 * Offer / Message actions. Make Offer is a dialog rather than a separate
 * screen — the group's Part 2 plan only committed to Listing Detail,
 * Create Listing, and Chat this milestone; a dedicated Make/View Offer
 * screen (shown in the wireframes) would follow this same pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    onBack: () -> Unit,
    onOpenChat: (Conversation) -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    var showOfferDialog by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is ListingDetailEvent.ConversationReady -> {
                onOpenChat(e.conversation)
                viewModel.consumeEvent()
            }
            is ListingDetailEvent.OfferSubmitted -> {
                showOfferDialog = false
                viewModel.consumeEvent()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ListingDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ListingDetailUiState.Error -> Text(
                    "Couldn't load this listing: ${state.message}",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                is ListingDetailUiState.Loaded -> ListingDetailContent(
                    listing = state.listing,
                    onMakeOffer = { showOfferDialog = true },
                    onMessage = viewModel::openOrStartConversation
                )
            }
        }
    }

    if (showOfferDialog) {
        MakeOfferDialog(
            onDismiss = { showOfferDialog = false },
            onSubmit = { amount, message -> viewModel.makeOffer(amount, message) }
        )
    }
}

@Composable
private fun ListingDetailContent(listing: Listing, onMakeOffer: () -> Unit, onMessage: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.4f)
        ) {
            // Coil AsyncImage goes here once listing.image is a real Firebase
            // Storage URL — placeholder surface until Create Listing's photo
            // upload is wired to real storage.
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(listing.title, style = MaterialTheme.typography.titleLarge)
        Text(
            "R${listing.price.toInt()}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(listing.sellerName ?: "Seller", style = MaterialTheme.typography.bodyMedium)
            if ((listing.sellerTrustScore ?: 0) > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "✓ Verified Student",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Description", style = MaterialTheme.typography.labelLarge)
        Text(listing.description, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onMakeOffer, modifier = Modifier.weight(1f)) { Text("Make Offer") }
            OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f)) { Text("Message") }
        }
    }
}

@Composable
private fun MakeOfferDialog(onDismiss: () -> Unit, onSubmit: (Double, String?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Make an offer") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Your offer (R)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (optional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (parsed != null) onSubmit(parsed, message.ifBlank { null })
            }) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
