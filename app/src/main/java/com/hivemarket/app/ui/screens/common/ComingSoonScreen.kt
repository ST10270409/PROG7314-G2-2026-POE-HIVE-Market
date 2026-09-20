package com.hivemarket.app.ui.screens.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A named, honest placeholder rather than a dead/missing tab — Messages
 * and Profile are designed (see the Planning and Design document's
 * wireframes) but not built this milestone. Showing this instead of
 * nothing keeps the bottom nav bar fully functional: every tap goes
 * somewhere, nothing silently does nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    title: String,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = { HiveMarketBottomNav(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                "$title is designed but not built in this milestone yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}
