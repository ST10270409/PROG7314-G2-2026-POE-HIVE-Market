package com.hivemarket.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hivemarket.app.R
import com.hivemarket.app.domain.Listing

/**
 * Home tab: category dropdown/chips + a two-column grid, matching the
 * Browse wireframe in the Planning and Design document. This screen reads
 * straight from [BrowseViewModel]'s offline-first state, so it renders
 * cached listings immediately and just shows an "offline" banner if the
 * network refresh underneath it fails — it never blocks on the network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(viewModel: BrowseViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.browse_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            if (uiState.isOffline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.browse_offline_banner),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            CategoryChipRow(
                selected = uiState.selectedCategory,
                onSelect = viewModel::selectCategory
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isRefreshing && uiState.listings.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.listings.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.browse_empty),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    }
                    else -> {
                        ListingGrid(listings = uiState.listings)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(selected: Category, onSelect: (Category) -> Unit) {
    val chips = listOf(
        Category.ALL to stringResource(R.string.browse_category_all),
        Category.TEXTBOOK to stringResource(R.string.browse_category_textbook),
        Category.ELECTRONICS to stringResource(R.string.browse_category_electronics),
        Category.FURNITURE to stringResource(R.string.browse_category_furniture)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { (category, label) ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ListingGrid(listings: List<Listing>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(listings, key = { it.listingID }) { listing ->
            ListingCard(listing)
        }
    }
}

@Composable
private fun ListingCard(listing: Listing) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                // Coil's AsyncImage goes here once real listing photos exist
                // (listing.image is a Firebase Storage URL per the schema);
                // left as a placeholder surface for this milestone.
            }
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "R${listing.price.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (listing.pendingSync) {
                Text(
                    text = "Pending sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
