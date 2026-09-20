package com.hivemarket.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hivemarket.app.ui.screens.browse.BrowseScreen
import com.hivemarket.app.ui.screens.chat.ChatScreen
import com.hivemarket.app.ui.screens.common.ComingSoonScreen
import com.hivemarket.app.ui.screens.createlisting.CreateListingScreen
import com.hivemarket.app.ui.screens.listingdetail.ListingDetailScreen
import com.hivemarket.app.ui.screens.login.LoginScreen
import com.hivemarket.app.ui.screens.offlinedrafts.OfflineDraftsScreen
import com.hivemarket.app.ui.screens.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Login -> Browse -> {Listing Detail, Create Listing, Chat, Settings} is
 * wired up for this milestone, plus a fully functional bottom nav bar
 * across every top-level screen. Messages and Profile route to
 * ComingSoonScreen — designed (see the Planning and Design wireframes) but
 * not built yet — so every nav tap goes somewhere rather than nothing
 * happening.
 */
object Routes {
    const val LOGIN = "login"
    const val BROWSE = "browse"
    const val SETTINGS = "settings"
    const val CREATE_LISTING = "create_listing"
    const val OFFLINE_DRAFTS = "offline_drafts"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val LISTING_DETAIL = "listing_detail/{listingID}"
    const val CHAT = "chat/{conversationID}?title={title}"

    fun listingDetail(listingID: Int) = "listing_detail/$listingID"
    fun chat(conversationID: Int, title: String) =
        "chat/$conversationID?title=${URLEncoder.encode(title, "UTF-8")}"
}

@Composable
fun HiveMarketNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
        // Home and Search both resolve to the Browse route for bottom-nav
        // highlighting purposes — see BottomNavDestination.
        ?.let { if (it == Routes.BROWSE) Routes.BROWSE else it }

    fun navigateFromBottomNav(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.BROWSE) { inclusive = false; saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    navController.navigate(Routes.BROWSE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                currentRoute = currentRoute,
                onNavigateBottomNav = ::navigateFromBottomNav,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenListing = { listingID -> navController.navigate(Routes.listingDetail(listingID)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) // clear the entire back stack — no returning to Browse post-logout
                    }
                }
            )
        }
        composable(Routes.CREATE_LISTING) {
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() },
                onViewDrafts = { navController.navigate(Routes.OFFLINE_DRAFTS) }
            )
        }
        composable(Routes.OFFLINE_DRAFTS) {
            OfflineDraftsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MESSAGES) {
            ComingSoonScreen(title = "Messages", currentRoute = currentRoute, onNavigate = ::navigateFromBottomNav)
        }
        composable(Routes.PROFILE) {
            ComingSoonScreen(title = "Profile", currentRoute = currentRoute, onNavigate = ::navigateFromBottomNav)
        }
        composable(
            route = Routes.LISTING_DETAIL,
            arguments = listOf(navArgument("listingID") { type = NavType.IntType })
        ) {
            ListingDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { conversation ->
                    navController.navigate(Routes.chat(conversation.conversationID, conversation.otherParticipant ?: "Chat"))
                }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationID") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType; defaultValue = "Chat" }
            )
        ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Chat"
            ChatScreen(
                title = URLDecoder.decode(encodedTitle, "UTF-8"),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
