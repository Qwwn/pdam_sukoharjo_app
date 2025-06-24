package com.metromultindo.tirtapanrannuangku.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.metromultindo.tirtapanrannuangku.ui.contact.ContactScreen
import com.metromultindo.tirtapanrannuangku.ui.customer.BillHistoryScreen
import com.metromultindo.tirtapanrannuangku.ui.customer.CustomerInfoScreen
import com.metromultindo.tirtapanrannuangku.ui.home.HomeScreen
import com.metromultindo.tirtapanrannuangku.ui.login.LoginScreen
import com.metromultindo.tirtapanrannuangku.ui.news.NewsScreen
import com.metromultindo.tirtapanrannuangku.ui.news.NewsDetailScreen
import com.metromultindo.tirtapanrannuangku.ui.onboarding.OnboardingScreen
import com.metromultindo.tirtapanrannuangku.ui.onboarding.OnboardingViewModel
import com.metromultindo.tirtapanrannuangku.ui.splash.SplashScreen
import com.metromultindo.tirtapanrannuangku.ui.splash.SplashViewModel
import com.metromultindo.tirtapanrannuangku.ui.complaint.ComplaintScreen
import com.metromultindo.tirtapanrannuangku.ui.selfmeter.SelfMeterScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object CustomerInfo : Screen("customerInfo")
    object BillHistory : Screen("billHistory")
    object News : Screen("news")
    object Contact : Screen("contact")
    object Complaint : Screen("complaint")
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
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

        composable("selfMeter") {
            SelfMeterScreen(navController = navController)
        }

        // Alias untuk news route (backward compatibility)
        composable("news") {
            NewsScreen(navController = navController)
        }

        composable(route = Screen.Contact.route) {
            ContactScreen(navController = navController)
        }

        // News detail route - Simplified dan konsisten
        composable(
            route = "newsDetail/{newsId}",
            arguments = listOf(
                navArgument("newsId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val newsIdString = backStackEntry.arguments?.getString("newsId") ?: "0"

            // Parse newsId dengan error handling
            val newsId = try {
                newsIdString.toIntOrNull() ?: 0
            } catch (e: Exception) {
                Log.e("NavGraph", "Error parsing newsId: $newsIdString", e)
                0
            }

            Log.d("NavGraph", "Navigating to NewsDetailScreen - newsId: $newsId (from: $newsIdString)")

            // Validasi newsId
            if (newsId > 0) {
                NewsDetailScreen(
                    newsId = newsId,
                    navController = navController
                )
            } else {
                // Fallback ke news list jika newsId invalid
                Log.w("NavGraph", "Invalid newsId: $newsId, redirecting to news list")

                LaunchedEffect(Unit) {
                    // Short delay untuk smooth transition
                    delay(100)
                    navController.navigate("news") {
                        popUpTo("newsDetail/{newsId}") { inclusive = true }
                        launchSingleTop = true
                    }
                }

                // Show loading while redirecting
                LoadingNewsDetail()
            }
        }

        // Complaint screen route
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

/**
 * Loading component untuk news detail saat redirect
 */
@Composable
private fun LoadingNewsDetail() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}