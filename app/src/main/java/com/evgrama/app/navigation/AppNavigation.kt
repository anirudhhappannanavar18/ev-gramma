package com.evgrama.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.evgrama.app.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object UserDashboard : Screen("user_dashboard")
    object HostDashboard : Screen("host_dashboard")
    object Map : Screen("map")
    object Booking : Screen("booking/{stationId}") {
        fun createRoute(stationId: String) = "booking/$stationId"
    }
    object Calculator : Screen("calculator")
    object AddStation : Screen("add_station")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Wallet : Screen("wallet")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToUserDashboard = {
                    navController.navigate(Screen.UserDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHostDashboard = {
                    navController.navigate(Screen.HostDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginAsUser = {
                    navController.navigate(Screen.UserDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginAsHost = {
                    navController.navigate(Screen.HostDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.UserDashboard.route) {
            UserDashboardScreen(
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToCalculator = { navController.navigate(Screen.Calculator.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) }
            )
        }

        composable(Screen.HostDashboard.route) {
            HostDashboardScreen(
                onNavigateToAddStation = { navController.navigate(Screen.AddStation.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onBookClick = { stationId -> 
                    navController.navigate(Screen.Booking.createRoute(stationId)) 
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Booking.route) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
            BookingScreen(
                stationId = stationId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Calculator.route) {
            CalculatorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AddStation.route) {
            AddStationScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clear entire back stack
                    }
                }
            )
        }

        composable(Screen.Wallet.route) {
            WalletScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
