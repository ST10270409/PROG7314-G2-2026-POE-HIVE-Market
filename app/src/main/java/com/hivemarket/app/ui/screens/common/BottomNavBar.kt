package com.hivemarket.app.ui.screens.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hivemarket.app.navigation.Routes

/**
 * The five persistent bottom-navigation destinations from the wireframes
 * (Home/Browse, Search, Sell, Messages, Profile). This was missing from
 * every screen until now — without it there was no way to reach anything
 * once more than one top-level screen existed, which is exactly the
 * structural gap this fixes.
 *
 * Home and Search both route to Browse (per the Planning and Design
 * document: "Search shares the Browse feed and layout"); Messages and
 * Profile route to placeholder screens until those are actually built —
 * see ComingSoonScreen.
 */
enum class BottomNavDestination(val route: String, val label: String) {
    HOME(Routes.BROWSE, "Home"),
    SEARCH(Routes.BROWSE, "Search"),
    SELL(Routes.CREATE_LISTING, "Sell"),
    MESSAGES(Routes.MESSAGES, "Messages"),
    PROFILE(Routes.PROFILE, "Profile")
}

@Composable
fun HiveMarketBottomNav(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        BottomNavDestination.entries.forEach { destination ->
            val icon = when (destination) {
                BottomNavDestination.HOME -> Icons.Filled.Home
                BottomNavDestination.SEARCH -> Icons.Filled.Search
                BottomNavDestination.SELL -> Icons.Filled.AddCircle
                BottomNavDestination.MESSAGES -> Icons.Filled.MailOutline
                BottomNavDestination.PROFILE -> Icons.Filled.Person
            }
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
