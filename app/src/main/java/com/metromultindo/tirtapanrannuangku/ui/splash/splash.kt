package com.metromultindo.tirtapanrannuangku.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metromultindo.tirtapanrannuangku.utils.Constants
import kotlinx.coroutines.delay
import android.util.Log
import com.metromultindo.tirtapanrannuangku.R

@Composable
fun SplashScreen(
    navController: NavController,
    onboardingCompleted: Boolean,
    isLoggedIn: Boolean,
    skipSplash: Boolean = false // Parameter baru untuk skip splash
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000
        )
    )

    // Skip splash screen jika ada notification intent
    if (skipSplash) {
        Log.d("SplashScreen", "Skipping splash screen due to notification intent")
        // Don't show splash content, let direct navigation handle
        return
    }

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(Constants.SPLASH_SCREEN_DURATION)

        // Navigate based on user state
        if (!onboardingCompleted) {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // Always go to home screen after onboarding, regardless of login status
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier
                    .size(180.dp)
                    .alpha(alpha = alphaAnim.value),
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = stringResource(id = R.string.app_logo)
            )

            Text(
                modifier = Modifier.alpha(alpha = alphaAnim.value),
                text = stringResource(id = R.string.entity_name),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}