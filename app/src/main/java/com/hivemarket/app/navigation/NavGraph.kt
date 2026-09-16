package com.hivemarket.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hivemarket.app.ui.screens.browse.BrowseScreen
import com.hivemarket.app.ui.screens.login.LoginScreen

/**
 * Only Login -> Browse is wired up for this prototype milestone (matches
 * the "show the group something real" scope discussed for the personal
 * branch). Listing Detail, Chat, Messages, Profile, Settings, and the
 * three chosen user-defined features follow the same pattern once the
 * group has actually picked which 3 features to build (see the Part 2
 * group plan) — each just needs a route added here and a screen composable
 * added under ui/screens/, following BrowseScreen as the template.
 */
object Routes {
    const val LOGIN = "login"
    const val BROWSE = "browse"
}

@Composable
fun HiveMarketNavHost(navController: NavHostController = rememberNavController()) {
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
            BrowseScreen()
        }
    }
}
