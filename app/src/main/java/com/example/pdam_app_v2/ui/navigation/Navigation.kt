package com.example.pdam_app_v2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pdam_app_v2.ui.contact.ContactScreen
import com.example.pdam_app_v2.ui.customer.BillHistoryScreen
import com.example.pdam_app_v2.ui.customer.CustomerInfoScreen
import com.example.pdam_app_v2.ui.home.HomeScreen
import com.example.pdam_app_v2.ui.login.LoginScreen
import com.example.pdam_app_v2.ui.news.NewsScreen
import com.example.pdam_app_v2.ui.news.NewsDetailScreen
import com.example.pdam_app_v2.ui.onboarding.OnboardingScreen
import com.example.pdam_app_v2.ui.onboarding.OnboardingViewModel
import com.example.pdam_app_v2.ui.splash.SplashScreen
import com.example.pdam_app_v2.ui.splash.SplashViewModel
import com.example.pdam_app_v2.ui.complaint.ComplaintScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object CustomerInfo : Screen("customerInfo")
    object BillHistory : Screen("billHistory")
    object News : Screen("news")
    object Outage : Screen("outage")
    object Contact : Screen("contact")
    object Complaint : Screen("complaint")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val splashViewModel: SplashViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    val onboardingCompleted by onboardingViewModel.hasCompletedOnboarding.collectAsState()
    val isLoggedIn by splashViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                onboardingCompleted = onboardingCompleted,
                isLoggedIn = isLoggedIn
            )
        }

        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // Login with destination route
        composable(
            route = "login/{destination}",
            arguments = listOf(
                navArgument("destination") {
                    type = NavType.StringType
                    defaultValue = "home"
                }
            )
        ) { backStackEntry ->
            val destination = backStackEntry.arguments?.getString("destination") ?: "home"
            LoginScreen(
                navController = navController,
                destination = destination
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(route = Screen.CustomerInfo.route) {
            CustomerInfoScreen(navController = navController)
        }

        composable(route = Screen.BillHistory.route) {
            BillHistoryScreen(navController = navController)
        }

        composable(route = Screen.News.route) {
            NewsScreen(navController = navController)
        }

        composable(route = Screen.Contact.route) {
            ContactScreen(navController = navController)
        }

        composable("news") {
            NewsScreen(navController = navController)
        }

        composable(
            "newsDetail/{newsId}",
            arguments = listOf(navArgument("newsId") { type = NavType.IntType })
        ) { backStackEntry ->
            val newsId = backStackEntry.arguments?.getInt("newsId") ?: 0
            NewsDetailScreen(
                navController = navController,
                newsId = newsId
            )
        }

        composable(
            route = "complaint?name={name}&number={number}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("number") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ComplaintScreen(
                navController = navController,
                customerName = backStackEntry.arguments?.getString("name"),
                customerNumber = backStackEntry.arguments?.getString("number")
            )
        }
    }
}