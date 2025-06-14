package com.metromultindo.tirtamakmur.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metromultindo.tirtamakmur.ui.components.CarouselItem
import com.metromultindo.tirtamakmur.ui.components.CarouselPage
import kotlinx.coroutines.delay
import com.metromultindo.tirtamakmur.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val carouselItems = listOf(
        CarouselItem(
            title = stringResource(id = R.string.title_first_carousell),
            description = stringResource(id = R.string.content_first_carousell),
            imageResId = R.drawable.ic_info_customer
        ),
        CarouselItem(
            title = stringResource(id = R.string.title_second_carousell),
            description = stringResource(id = R.string.content_second_carousell),
            imageResId = R.drawable.ic_outage
        ),
        CarouselItem(
            title = stringResource(id = R.string.title_third_carousell),
            description = stringResource(id = R.string.content_third_carousell),
            imageResId = R.drawable.ic_news
        ),
        CarouselItem(
            title = stringResource(id = R.string.title_fourth_carousel),
            description = stringResource(id = R.string.content_fourth_carousel),
            imageResId = R.drawable.ic_complaint
        ),
        CarouselItem(
            title = stringResource(id = R.string.title_fifth_carousel),
            description = stringResource(id = R.string.content_fifth_carousel),
            imageResId = R.drawable.ic_meter_reading
        )
    )

    val pagerState = rememberPagerState(pageCount = { carouselItems.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = viewModel.currentPage.collectAsState()

    // When the page changes, update the ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    // Auto scroll the carousel
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000) //
            val nextPage = (pagerState.currentPage + 1) % carouselItems.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        // Logo and welcome text
        Text(
            text = stringResource(id = R.string.entity_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Carousel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            CarouselPage(carouselItems[page])
        }

        // Indicators
        Row(
            Modifier
                .height(30.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color = color, shape = MaterialTheme.shapes.small)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue button
        Button(
            onClick = {
                // Mark onboarding as completed in ViewModel
                viewModel.completeOnboarding()

                // Navigate to login screen
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Lanjutkan",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}